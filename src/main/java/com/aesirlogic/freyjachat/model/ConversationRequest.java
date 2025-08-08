package com.aesirlogic.freyjachat.model;

import lombok.Data;

@Data
public class ConversationRequest {
    private String conversationId;
    private String message;
}
