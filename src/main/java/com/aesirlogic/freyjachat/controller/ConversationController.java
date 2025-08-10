package com.aesirlogic.freyjachat.controller;

import com.aesirlogic.freyjachat.model.ConversationRequest;
import com.aesirlogic.freyjachat.model.ConversationResponse;
import com.aesirlogic.freyjachat.service.ConversationService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RequestMapping("/api/v1/conversations")
@RestController
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ConversationResponse chat(@RequestBody ConversationRequest request) {
        return conversationService.addToConversation(request);
    }

    @GetMapping
    public List<ConversationResponse> getConversations() {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId("testid");
        response.setTitle("test title");
        response.setMessages(Collections.singletonList("This is a placeholder response for the message"));

        return Collections.singletonList(response);
    }

    @GetMapping("/{conversationId}")
    public ConversationResponse getConversations(@PathVariable String conversationId) {
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(conversationId);
        response.setTitle("test title");
        response.setMessages(Collections.singletonList("This is a placeholder response for the message"));

        return response;
    }

}
