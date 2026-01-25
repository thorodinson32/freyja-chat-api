package com.aesirlogic.freyjachat.client;

import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsRequest;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsResponse;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesRequest;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OpenAPIClient {

    @Value("${openai.api-key}")
    private String openaiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ModerationsResponse getModeratonsResult(ModerationsRequest request) {
        String url = "https://api.openai.com/v1/moderations";
        String apiKey = "Bearer " + openaiApiKey;


        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Mono<ModerationsResponse> responseMono = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ModerationsResponse.class);

        return responseMono.block();
    }

    public ResponsesResponse getResponsesResult(ResponsesRequest request) {
        String url = "https://api.openai.com/v1/responses";
        String apiKey = "Bearer " + openaiApiKey;

        try {
            String requestJson = objectMapper.writeValueAsString(request);
            log.debug("OpenAI request: {}", requestJson);
        } catch (Exception e) {
            log.warn("Could not serialize request for logging", e);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        try {
            ResponsesResponse response = webClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ResponsesResponse.class)
                    .block();
            log.debug("OpenAI response received successfully");
            return response;
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
