package com.aesirlogic.freyjachat.model;

import lombok.Data;

@Data
public class CompletionResponse {
    private String content;
    private boolean success;
    private String error;
}
