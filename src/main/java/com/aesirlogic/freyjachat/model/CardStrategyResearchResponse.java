package com.aesirlogic.freyjachat.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data @Builder
public class CardStrategyResearchResponse {
    private List<CardAssessment> cards;
    private List<CategoryRecommendation> categoryRecommendations;
    private List<Source> sources;
    private Instant generatedAt;
    private Usage usage;
    private Quota quota;

    @Data @Builder public static class CardAssessment {
        private String accountId;
        private String summary;
        private String annualFee;
        private String role;
        private List<Benefit> benefits;
    }
    @Data @Builder public static class Benefit { private String category; private String reward; private String conditions; }
    @Data @Builder public static class CategoryRecommendation { private String category; private String accountId; private String reward; private String rationale; }
    @Data @Builder public static class Source { private String title; private String url; }
    @Data @Builder public static class Usage {
        private long inputTokens;
        private long cachedInputTokens;
        private long outputTokens;
        private long reasoningTokens;
        private long totalTokens;
    }
    @Data @Builder public static class Quota { private long dailyRemaining; private long monthlyRemaining; }
}
