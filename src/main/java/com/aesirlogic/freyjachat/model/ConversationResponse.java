package com.aesirlogic.freyjachat.model;

import lombok.Data;

import java.util.List;

@Data
public class ConversationResponse {
    private String conversationId;
    private String title;
    private List<String> messages;
}
