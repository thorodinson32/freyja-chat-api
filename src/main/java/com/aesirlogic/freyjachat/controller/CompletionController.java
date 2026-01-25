package com.aesirlogic.freyjachat.controller;

import com.aesirlogic.freyjachat.model.CompletionRequest;
import com.aesirlogic.freyjachat.model.CompletionResponse;
import com.aesirlogic.freyjachat.service.CompletionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/completions")
@RestController
public class CompletionController {

    private final CompletionService completionService;

    public CompletionController(CompletionService completionService) {
        this.completionService = completionService;
    }

    @PostMapping
    public ResponseEntity<CompletionResponse> complete(@RequestBody CompletionRequest request) {
        CompletionResponse response = completionService.complete(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
