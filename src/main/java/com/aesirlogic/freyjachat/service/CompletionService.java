package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.client.OpenAPIClient;
import com.aesirlogic.freyjachat.model.CompletionRequest;
import com.aesirlogic.freyjachat.model.CompletionResponse;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsRequest;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsResponse;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesRequest;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CompletionService {

    private final OpenAPIClient openAPIClient;

    public CompletionService(OpenAPIClient openAPIClient) {
        this.openAPIClient = openAPIClient;
    }

    public CompletionResponse complete(CompletionRequest request) {
        CompletionResponse response = new CompletionResponse();

        if (request == null || StringUtils.isBlank(request.getUserMessage())) {
            response.setSuccess(false);
            response.setError("User message is required");
            return response;
        }

        // Check moderation
        if (isFlaggedByModeration(request.getUserMessage())) {
            response.setSuccess(false);
            response.setError("Message was flagged by moderation");
            return response;
        }

        try {
            String content = getCompletion(request);
            response.setContent(content);
            response.setSuccess(true);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setError("Failed to get completion: " + e.getMessage());
        }

        return response;
    }

    private boolean isFlaggedByModeration(String message) {
        ModerationsRequest moderationsRequest = new ModerationsRequest();
        moderationsRequest.setInput(Collections.singletonList(message));
        ModerationsResponse moderationsResponse = openAPIClient.getModeratonsResult(moderationsRequest);
        return moderationsResponse != null && !moderationsResponse.getResults().isEmpty()
                && moderationsResponse.getResults().getFirst().isFlagged();
    }

    private String getCompletion(CompletionRequest request) {
        ResponsesRequest responsesRequest = buildResponsesRequest(request);
        ResponsesResponse responsesResponse = openAPIClient.getResponsesResult(responsesRequest);

        if (responsesResponse != null && responsesResponse.getOutput() != null && !responsesResponse.getOutput().isEmpty()) {
            var output = responsesResponse.getOutput().getFirst();
            if (output.getContent() != null && !output.getContent().isEmpty()) {
                return output.getContent().getFirst().getText();
            }
        }
        return null;
    }

    private ResponsesRequest buildResponsesRequest(CompletionRequest request) {
        ResponsesRequest req = new ResponsesRequest();
        req.setModel("gpt-4.1-mini");
        req.setInput(request.getUserMessage());

        if (StringUtils.isNotBlank(request.getSystemPrompt())) {
            req.setInstructions(request.getSystemPrompt());
        }

        if (request.isJsonMode()) {
            req.setText(ResponsesRequest.TextConfig.json());
        }

        return req;
    }
}
