package com.aesirlogic.freyjachat.model;

import lombok.Data;

@Data
public class CompletionRequest {
    private String systemPrompt;
    private String userMessage;
    private boolean jsonMode;
}
