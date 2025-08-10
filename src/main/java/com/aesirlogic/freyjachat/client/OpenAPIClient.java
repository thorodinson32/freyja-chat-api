package com.aesirlogic.freyjachat.client;

import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsRequest;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsResponse;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesRequest;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OpenAPIClient {

    @Value("${openai.api-key}")
    private String openaiApiKey;

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

        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Mono<ResponsesResponse> responseMono = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ResponsesResponse.class);

        return responseMono.block();
    }
}
