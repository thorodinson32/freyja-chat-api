package com.aesirlogic.freyjachat.controller;

import com.aesirlogic.freyjachat.model.StockNewsResearchRequest;
import com.aesirlogic.freyjachat.model.StockNewsResearchResponse;
import com.aesirlogic.freyjachat.service.StockNewsResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-news")
@RequiredArgsConstructor
public class StockNewsResearchController {
    private final StockNewsResearchService service;

    @PostMapping("/research")
    public ResponseEntity<StockNewsResearchResponse> research(@RequestBody StockNewsResearchRequest request) {
        return ResponseEntity.ok(service.research(request));
    }
}
