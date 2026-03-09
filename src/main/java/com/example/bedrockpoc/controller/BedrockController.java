package com.example.bedrockpoc.controller;

import com.example.bedrockpoc.dto.ChatRequest;
import com.example.bedrockpoc.service.BedrockService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bedrock")
public class BedrockController {

    private final BedrockService bedrockService;

    public BedrockController(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
    }

    @PostMapping("/chat")
    public ResponseEntity<JsonNode> chat(@RequestBody ChatRequest request) {
        JsonNode response = bedrockService.sendPrompt(request);
        return ResponseEntity.ok(response);
    }
}

