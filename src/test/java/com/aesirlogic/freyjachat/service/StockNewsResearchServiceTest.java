package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.client.OpenAPIClient;
import com.aesirlogic.freyjachat.model.StockNewsResearchRequest;
import com.aesirlogic.freyjachat.model.StockNewsResearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockNewsResearchServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void usesOneRequiredSearchAndParsesOnlyActualSearchSources() throws Exception {
        OpenAPIClient client = mock(OpenAPIClient.class);
        StockNewsQuotaService quotas = mock(StockNewsQuotaService.class);
        when(quotas.reserve(true)).thenReturn(StockNewsResearchResponse.Quota.builder()
                .dailyRemaining(5).monthlyRemaining(119).manualRemaining(1).build());
        when(client.getResponsesJson(any())).thenReturn(mapper.readTree("""
                {"output":[
                  {"type":"web_search_call","action":{"sources":[
                    {"title":"SEC filing","url":"https://sec.gov/a"},
                    {"title":"Duplicate","url":"https://sec.gov/a"},
                    {"title":"Bad","url":"javascript:alert(1)"}]}},
                  {"type":"message","content":[{"type":"output_text","text":"{\\\"sentiment\\\":\\\"SOMEWHAT_BULLISH\\\",\\\"confidence\\\":72,\\\"summary\\\":\\\"Shares rose while investors assessed current reporting.\\\",\\\"drivers\\\":[{\\\"direction\\\":\\\"BULLISH\\\",\\\"summary\\\":\\\"A verified company update supported sentiment.\\\"}]}"}]}
                ],"usage":{"input_tokens":120,"input_tokens_details":{"cached_tokens":20},"output_tokens":40,"output_tokens_details":{"reasoning_tokens":0},"total_tokens":160}}
                """));
        var service = new StockNewsResearchService(client, quotas, mapper, "gpt-5.6-luna");
        var quote = StockNewsResearchRequest.QuoteSnapshot.builder().price(new BigDecimal("230.40"))
                .change(new BigDecimal("2.10")).changePercent(new BigDecimal("0.92"))
                .asOf(Instant.parse("2026-09-08T15:00:00Z")).build();

        var response = service.research(StockNewsResearchRequest.builder().symbol("nvda")
                .companyName("NVIDIA").session("MANUAL").marketDate(LocalDate.of(2026, 9, 8))
                .timezone("America/Chicago").quote(quote).build());

        assertThat(response.getSymbol()).isEqualTo("NVDA");
        assertThat(response.getQuote()).isSameAs(quote);
        assertThat(response.getSources()).extracting(StockNewsResearchResponse.Source::getUrl)
                .containsExactly("https://sec.gov/a");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(160);
        ArgumentCaptor<Object> request = ArgumentCaptor.forClass(Object.class);
        verify(client).getResponsesJson(request.capture());
        @SuppressWarnings("unchecked") Map<String, Object> body = (Map<String, Object>) request.getValue();
        assertThat(body).containsEntry("model", "gpt-5.6-luna")
                .containsEntry("tool_choice", "required")
                .containsEntry("max_tool_calls", 1)
                .containsEntry("max_output_tokens", 500)
                .containsEntry("store", false);
        assertThat(body.get("reasoning")).isEqualTo(Map.of("effort", "none"));
    }

    @Test
    void quotaIsReservedBeforeOpenAiIsCalled() {
        OpenAPIClient client = mock(OpenAPIClient.class);
        StockNewsQuotaService quotas = mock(StockNewsQuotaService.class);
        when(quotas.reserve(false)).thenThrow(new IllegalStateException("quota unavailable"));
        var service = new StockNewsResearchService(client, quotas, mapper, "gpt-5.6-luna");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.research(StockNewsResearchRequest.builder().symbol("NVDA").session("MIDDAY").build()));

        verifyNoInteractions(client);
    }
}
