package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.client.OpenAPIClient;
import com.aesirlogic.freyjachat.model.StockNewsResearchRequest;
import com.aesirlogic.freyjachat.model.StockNewsResearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class StockNewsResearchService {
    private static final Set<String> SENTIMENTS = Set.of("BULLISH", "SOMEWHAT_BULLISH", "NEUTRAL", "MIXED",
            "SOMEWHAT_BEARISH", "BEARISH", "INSUFFICIENT_DATA");
    private static final Set<String> DIRECTIONS = Set.of("BULLISH", "BEARISH", "NEUTRAL");

    private final OpenAPIClient openAIClient;
    private final StockNewsQuotaService quotaService;
    private final ObjectMapper objectMapper;
    private final String model;

    public StockNewsResearchService(OpenAPIClient openAIClient, StockNewsQuotaService quotaService,
                                    ObjectMapper objectMapper,
                                    @Value("${openai.stock-news.model:gpt-5.6-luna}") String model) {
        this.openAIClient = openAIClient;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public StockNewsResearchResponse research(StockNewsResearchRequest request) {
        if (request == null || StringUtils.isBlank(request.getSymbol())) {
            throw new IllegalArgumentException("symbol is required");
        }
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        String session = StringUtils.defaultIfBlank(request.getSession(), "MANUAL").toUpperCase(Locale.ROOT);
        LocalDate marketDate = request.getMarketDate() == null ? LocalDate.now() : request.getMarketDate();
        var quota = quotaService.reserve("MANUAL".equals(session));
        JsonNode raw = openAIClient.getResponsesJson(buildOpenAIRequest(request, symbol, session, marketDate));
        String outputText = extractOutputText(raw);
        try {
            JsonNode assessment = objectMapper.readTree(outputText);
            String sentiment = assessment.path("sentiment").asText("INSUFFICIENT_DATA");
            if (!SENTIMENTS.contains(sentiment)) sentiment = "INSUFFICIENT_DATA";
            List<StockNewsResearchResponse.Source> sources = extractSources(raw);
            int confidence = Math.max(0, Math.min(100, assessment.path("confidence").asInt()));
            if (sources.isEmpty()) {
                sentiment = "INSUFFICIENT_DATA";
                confidence = 0;
            }
            List<StockNewsResearchResponse.Driver> drivers = new ArrayList<>();
            for (JsonNode driver : assessment.path("drivers")) {
                String direction = driver.path("direction").asText("NEUTRAL");
                if (!DIRECTIONS.contains(direction)) direction = "NEUTRAL";
                drivers.add(StockNewsResearchResponse.Driver.builder()
                        .direction(direction).summary(driver.path("summary").asText()).build());
                if (drivers.size() == 3) break;
            }
            return StockNewsResearchResponse.builder()
                    .symbol(symbol).marketDate(marketDate).session(session).sentiment(sentiment)
                    .confidence(confidence)
                    .summary(assessment.path("summary").asText())
                    .drivers(drivers).sources(sources).quote(request.getQuote()).generatedAt(Instant.now())
                    .usage(extractUsage(raw)).quota(quota).build();
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI returned an unreadable stock-news assessment", e);
        }
    }

    private Map<String, Object> buildOpenAIRequest(StockNewsResearchRequest request, String symbol,
                                                    String session, LocalDate marketDate) {
        String company = StringUtils.defaultIfBlank(request.getCompanyName(), "unknown company/fund name");
        String quote = request.getQuote() == null ? "Quote unavailable" : String.format(Locale.ROOT,
                "price=%s, change=%s, changePercent=%s%%, quoteAsOf=%s",
                request.getQuote().getPrice(), request.getQuote().getChange(),
                request.getQuote().getChangePercent(), request.getQuote().getAsOf());
        String input = String.format(Locale.ROOT, """
                Research current, credible reporting about %s (%s) for market date %s, session %s.
                Finnhub observed quote: %s.
                Identify news that may plausibly help explain today's move. Do not claim causation, predict the
                next move, or give investment advice. Prefer primary company/regulatory sources and established
                financial reporting. If credible relevant news is absent, return INSUFFICIENT_DATA. If meaningful
                positive and negative drivers conflict, return MIXED.
                """, company, symbol, marketDate, session, quote);

        Map<String, Object> driverSchema = Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "direction", Map.of("type", "string", "enum", List.of("BULLISH", "BEARISH", "NEUTRAL")),
                        "summary", Map.of("type", "string")),
                "required", List.of("direction", "summary"));
        Map<String, Object> schema = Map.of(
                "type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "sentiment", Map.of("type", "string", "enum", SENTIMENTS.stream().sorted().toList()),
                        "confidence", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "summary", Map.of("type", "string"),
                        "drivers", Map.of("type", "array", "maxItems", 3, "items", driverSchema)),
                "required", List.of("sentiment", "confidence", "summary", "drivers"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", "Return a concise, source-grounded assessment. The summary must contain no more than three sentences.");
        body.put("input", input);
        body.put("tools", List.of(Map.of("type", "web_search", "search_context_size", "low")));
        body.put("tool_choice", "required");
        body.put("max_tool_calls", 1);
        body.put("max_output_tokens", 500);
        body.put("reasoning", Map.of("effort", "none"));
        body.put("include", List.of("web_search_call.action.sources"));
        body.put("store", false);
        body.put("text", Map.of("format", Map.of("type", "json_schema", "name", "stock_news_brief",
                "strict", true, "schema", schema)));
        return body;
    }

    private String extractOutputText(JsonNode raw) {
        if (raw == null) throw new IllegalStateException("Empty OpenAI response");
        for (JsonNode item : raw.path("output")) {
            if (!"message".equals(item.path("type").asText())) continue;
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText()) && content.hasNonNull("text")) {
                    return content.get("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI response contained no output text");
    }

    private List<StockNewsResearchResponse.Source> extractSources(JsonNode raw) {
        LinkedHashMap<String, String> sources = new LinkedHashMap<>();
        for (JsonNode item : raw.path("output")) {
            for (JsonNode source : item.path("action").path("sources")) {
                String url = source.path("url").asText(null);
                if (url != null && (url.startsWith("https://") || url.startsWith("http://"))) {
                    sources.putIfAbsent(url, source.path("title").asText(url));
                }
            }
        }
        return sources.entrySet().stream().limit(5)
                .map(e -> StockNewsResearchResponse.Source.builder().url(e.getKey()).title(e.getValue()).build())
                .toList();
    }

    private StockNewsResearchResponse.Usage extractUsage(JsonNode raw) {
        JsonNode usage = raw.path("usage");
        return StockNewsResearchResponse.Usage.builder()
                .inputTokens(usage.path("input_tokens").asLong())
                .cachedInputTokens(usage.path("input_tokens_details").path("cached_tokens").asLong())
                .outputTokens(usage.path("output_tokens").asLong())
                .reasoningTokens(usage.path("output_tokens_details").path("reasoning_tokens").asLong())
                .totalTokens(usage.path("total_tokens").asLong()).build();
    }
}
