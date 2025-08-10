package com.aesirlogic.freyjachat.model.openapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class ResponsesOutput {
    private String status;
    private List<ResponsesContent> content;
    private String role;
}
