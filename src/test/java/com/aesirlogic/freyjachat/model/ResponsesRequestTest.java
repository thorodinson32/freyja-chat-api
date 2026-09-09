package com.aesirlogic.freyjachat.model;

import com.aesirlogic.freyjachat.model.openapi.responses.ResponsesRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponsesRequestTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesStructuredOutputFormat() throws Exception {
        ResponsesRequest request = new ResponsesRequest();
        request.setText(ResponsesRequest.TextConfig.schema("plan",
                mapper.readTree("{\"type\":\"object\"}"), true));

        String json = mapper.writeValueAsString(request);
        assertThat(json).contains("\"type\":\"json_schema\"")
                .contains("\"name\":\"plan\"")
                .contains("\"strict\":true")
                .contains("\"schema\":{\"type\":\"object\"}");
    }
}
