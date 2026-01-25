package com.aesirlogic.freyjachat.model.openapi.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsesRequest {
    private String model;
    private String input;
    private String instructions;
    private TextConfig text;

    @Data
    public static class TextConfig {
        private FormatConfig format;

        public static TextConfig json() {
            TextConfig tc = new TextConfig();
            FormatConfig fc = new FormatConfig();
            fc.setType("json_object");
            tc.setFormat(fc);
            return tc;
        }
    }

    @Data
    public static class FormatConfig {
        private String type;
    }
}
