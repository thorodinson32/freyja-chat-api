package com.aesirlogic.freyjachat.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockNewsResearchRequest {
    private String symbol;
    private String companyName;
    private String session;
    private LocalDate marketDate;
    private String timezone;
    private QuoteSnapshot quote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuoteSnapshot {
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
        private Instant asOf;
    }
}
