package com.aesirlogic.freyjachat.model;

import lombok.Data;
import java.util.List;

@Data
public class CardStrategyResearchRequest {
    private List<Card> cards;

    @Data
    public static class Card {
        private String accountId;
        private String name;
        private String institution;
        private List<String> knownBenefits;
    }
}
