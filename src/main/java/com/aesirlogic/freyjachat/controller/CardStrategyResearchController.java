package com.aesirlogic.freyjachat.controller;

import com.aesirlogic.freyjachat.model.CardStrategyResearchRequest;
import com.aesirlogic.freyjachat.model.CardStrategyResearchResponse;
import com.aesirlogic.freyjachat.service.CardStrategyResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/card-strategy") @RequiredArgsConstructor
public class CardStrategyResearchController {
    private final CardStrategyResearchService service;
    @PostMapping("/research") public CardStrategyResearchResponse research(@RequestBody CardStrategyResearchRequest request){return service.research(request);}
}
