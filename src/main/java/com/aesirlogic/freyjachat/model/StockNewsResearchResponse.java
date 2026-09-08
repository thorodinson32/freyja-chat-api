package com.aesirlogic.freyjachat.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StockNewsResearchResponse {
    private String symbol;
    private LocalDate marketDate;
    private String session;
    private String sentiment;
    private int confidence;
    private String summary;
    private List<Driver> drivers;
    private List<Source> sources;
    private StockNewsResearchRequest.QuoteSnapshot quote;
    private Instant generatedAt;
    private Usage usage;
    private Quota quota;

    @Data
    @Builder
    public static class Driver {
        private String direction;
        private String summary;
    }

    @Data
    @Builder
    public static class Source {
        private String title;
        private String url;
    }

    @Data
    @Builder
    public static class Usage {
        private long inputTokens;
        private long cachedInputTokens;
        private long outputTokens;
        private long reasoningTokens;
        private long totalTokens;
    }

    @Data
    @Builder
    public static class Quota {
        private long dailyRemaining;
        private long monthlyRemaining;
        private long manualRemaining;
    }
}
