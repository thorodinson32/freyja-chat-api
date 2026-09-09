package com.aesirlogic.freyjachat.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CompletionRequest {
    private String systemPrompt;
    private String userMessage;
    private boolean jsonMode;
    private String responseSchemaName;
    private JsonNode responseSchema;
    private Boolean strict;
}
