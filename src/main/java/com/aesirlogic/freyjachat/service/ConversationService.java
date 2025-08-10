package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.client.OpenAPIClient;
import com.aesirlogic.freyjachat.model.ConversationRequest;
import com.aesirlogic.freyjachat.model.ConversationResponse;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsRequest;
import com.aesirlogic.freyjachat.model.openapi.moderations.ModerationsResponse;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesMessage;
import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final OpenAPIClient openAPIClient;

    public ConversationService(OpenAPIClient openAPIClient) {
        this.openAPIClient = openAPIClient;
    }

    public ConversationResponse addToConversation(ConversationRequest request) {
        if (request == null || StringUtils.isBlank(request.getMessage())) {
            return null;
        }

        if (isFlaggedByModeration(request.getMessage())) {
            return buildFlaggedResponse();
        }

        String reply = getAIReply(request.getMessage());
//        String title = getAITitle(request.getMessage());
//        return buildConversationResponseWithTitle(reply, title);
        return buildConversationResponseWithTitle(reply, null);
    }

    private boolean isFlaggedByModeration(String message) {
        ModerationsRequest moderationsRequest = new ModerationsRequest();
        moderationsRequest.setInput(Collections.singletonList(message));
        ModerationsResponse moderationsResponse = openAPIClient.getModeratonsResult(moderationsRequest);
        return moderationsResponse != null && !moderationsResponse.getResults().isEmpty()
                && moderationsResponse.getResults().getFirst().isFlagged();
    }

    private String cleanAIText(String text) {
        if (text == null) return null;
        // Remove leading/trailing quotes and trim whitespace
        return text.replaceAll("^\"|\"$", "").trim();
    }

    private String getAIReply(String userMessage) {
        ResponsesRequest responsesRequest = buildResponsesRequest(userMessage);
        var responsesResponse = openAPIClient.getResponsesResult(responsesRequest);
        if (responsesResponse != null && !responsesResponse.getOutput().isEmpty()) {
            String raw = responsesResponse.getOutput().getFirst().getContent().getFirst().getText();
            return cleanAIText(raw);
        }
        return null;
    }

//    private String getAITitle(String userMessage) {
//        ResponsesRequest titleRequest = buildResponsesRequest(
//                "Make a short, relevant title for this message. Do not include quotes or any extra text—just the title: " + userMessage
//        );
//        var titleResponse = openAPIClient.getResponsesResult(titleRequest);
//        if (titleResponse != null && !titleResponse.getOutput().isEmpty()) {
//            String raw = titleResponse.getOutput().getFirst().getContent().getFirst().getText();
//            return cleanAIText(raw);
//        }
//        return null;
//    }

    private ConversationResponse buildFlaggedResponse() {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(null);
        response.setTitle(null);
        response.setMessages(Collections.singletonList("Your message was flagged by the moderation system and cannot be processed."));
        return response;
    }

    private ResponsesRequest buildResponsesRequest(String userMessage) {
        ResponsesMessage systemMessage = new ResponsesMessage();
        systemMessage.setContent("You are Freyja, Norse Goddess of Wisdom and War. You are helpful, creative, clever, and friendly. You provide concise and relevant answers to user questions.");
        systemMessage.setRole("system");
        ResponsesMessage userMsg = new ResponsesMessage();
        userMsg.setContent(userMessage);
        userMsg.setRole("user");
        ResponsesRequest req = new ResponsesRequest();
        req.setInput(List.of(systemMessage, userMsg));
        req.setModel("gpt-4o");
        return req;
    }

    private ConversationResponse buildConversationResponseWithTitle(String reply, String title) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(UUID.randomUUID().toString());
        response.setTitle(StringUtils.defaultIfBlank(title, "Untitled"));
        response.setMessages(Collections.singletonList(StringUtils.defaultIfBlank(reply, "The response from the AI was empty.")));
        return response;
    }
}
