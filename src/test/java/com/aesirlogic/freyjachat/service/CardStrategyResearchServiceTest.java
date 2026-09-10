package com.aesirlogic.freyjachat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aesirlogic.freyjachat.client.OpenAPIClient;
import com.aesirlogic.freyjachat.model.CardStrategyResearchRequest;
import com.aesirlogic.freyjachat.model.CardStrategyResearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CardStrategyResearchServiceTest {

    @Test
    void usesOneWebSearchForTheWholePortfolioAndReturnsCitations() throws Exception {
        OpenAPIClient client = mock(OpenAPIClient.class);
        CardStrategyQuotaService quotas = mock(CardStrategyQuotaService.class);
        when(quotas.reserve()).thenReturn(CardStrategyResearchResponse.Quota.builder()
                .dailyRemaining(11).monthlyRemaining(29).build());
        when(client.getResponsesJson(any())).thenReturn(new ObjectMapper().readTree("""
                {
                  "output":[
                    {"type":"web_search_call","action":{"sources":[{"title":"Issuer rewards","url":"https://example.com/rewards"}]}},
                    {"type":"message","content":[{"type":"output_text","text":"{\\"cards\\":[{\\"accountId\\":\\"card-one\\",\\"summary\\":\\"Best everyday card\\",\\"annualFee\\":\\"$0\\",\\"role\\":\\"PRIMARY\\",\\"benefits\\":[]}],\\"categoryRecommendations\\":[{\\"category\\":\\"DINING\\",\\"accountId\\":\\"card-one\\",\\"reward\\":\\"3x\\",\\"rationale\\":\\"Highest verified return\\"}]}"}]}
                  ],
                  "usage":{"input_tokens":100,"input_tokens_details":{"cached_tokens":10},"output_tokens":200,"output_tokens_details":{"reasoning_tokens":0},"total_tokens":300}
                }
                """));
        CardStrategyResearchRequest request = new CardStrategyResearchRequest();
        CardStrategyResearchRequest.Card first = new CardStrategyResearchRequest.Card();
        first.setAccountId("card-one"); first.setName("Everyday Card"); first.setInstitution("Example Bank");
        first.setKnownBenefits(List.of("Dining · 3x points"));
        CardStrategyResearchRequest.Card second = new CardStrategyResearchRequest.Card();
        second.setAccountId("card-two"); second.setName("Travel Card");
        request.setCards(List.of(first, second));

        CardStrategyResearchResponse result = new CardStrategyResearchService(client, quotas, new ObjectMapper(), "gpt-5.6-luna").research(request);

        assertThat(result.getCards()).singleElement().satisfies(card -> assertThat(card.getAccountId()).isEqualTo("card-one"));
        assertThat(result.getSources()).singleElement().satisfies(source -> assertThat(source.getUrl()).isEqualTo("https://example.com/rewards"));
        assertThat(result.getUsage().getCachedInputTokens()).isEqualTo(10);
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(client).getResponsesJson(body.capture());
        @SuppressWarnings("unchecked") Map<String, Object> requestBody = (Map<String, Object>) body.getValue();
        assertThat(requestBody.get("max_tool_calls")).isEqualTo(1);
        assertThat(requestBody.get("max_output_tokens")).isEqualTo(4000);
        assertThat(requestBody.get("store")).isEqualTo(false);
        assertThat(String.valueOf(requestBody.get("input"))).contains("card-one", "card-two");
    }
}
