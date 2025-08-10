package com.aesirlogic.freyjachat.model.openapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class ResponsesRequest {
    private String model;
    private List<ResponsesMessage> input;
}
