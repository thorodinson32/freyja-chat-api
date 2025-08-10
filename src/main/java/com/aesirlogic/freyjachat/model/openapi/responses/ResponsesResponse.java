package com.aesirlogic.freyjachat.model.openapi.responses;

import lombok.Data;

import java.util.List;

@Data
public class ResponsesResponse {
    private List<ResponsesOutput> output;
}
