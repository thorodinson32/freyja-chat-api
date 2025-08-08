package com.aesirlogic.freyjachat.service;

import com.aesirlogic.freyjachat.model.ConversationResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    public ConversationResponse addToConversation(String conversationId, String message) {
        // call moderation API
        if(StringUtils.isBlank(conversationId)) {
            // create new conversation
        } else {
            // get last 6 messages from conversation
        }

        // call responses API

        return null;
    }
}
