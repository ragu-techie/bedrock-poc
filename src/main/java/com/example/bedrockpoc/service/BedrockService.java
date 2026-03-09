package com.example.bedrockpoc.service;

import com.example.bedrockpoc.dto.ChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedrockService {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    @Value("${bedrock.modelId:anthropic.claude-3-haiku-20240307-v1:0}")
    private String modelId;

    public BedrockService(BedrockRuntimeClient bedrockRuntimeClient, ObjectMapper objectMapper) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode sendPrompt(ChatRequest request) {
        try {
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", request.getPrompt());

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", List.of(messageContent));

            Map<String, Object> payload = new HashMap<>();
            payload.put("anthropic_version", "bedrock-2023-05-31");
            payload.put("max_tokens", 512);
            payload.put("temperature", 0.5);
            payload.put("messages", List.of(message));

            String json = objectMapper.writeValueAsString(payload);

            InvokeModelRequest invokeModelRequest = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromString(json, StandardCharsets.UTF_8))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(invokeModelRequest);

            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Error calling AWS Bedrock", e);
        }
    }
}

