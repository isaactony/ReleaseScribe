package com.releasescribe.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasescribe.model.ReleaseNotes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnthropicReleaseNotesClient {
    
    private final AnthropicClient client;
    private final ObjectMapper objectMapper;
    private final double temperature;
    private final int maxTokens;
    
    public AnthropicReleaseNotesClient(String apiKey) {
        this(apiKey, 0.1, 2000);
    }
    
    public AnthropicReleaseNotesClient(String apiKey, double temperature, int maxTokens) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }
    
    public ReleaseNotes generateReleaseNotes(String systemPrompt, String userPrompt) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model("claude-opus-4-1-20250805")
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .system(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();
            
            Message response = client.messages().create(params);
            
            // Extract the content from the response
            if (response.content() == null || response.content().isEmpty()) {
                throw new RuntimeException("No content in Anthropic response");
            }
            
            TextBlock textBlock = response.content().get(0).text().orElse(null);
            if (textBlock == null) {
                throw new RuntimeException("No text content in response");
            }
            String text = textBlock.text();
            
            // Clean up the text to remove markdown code blocks
            String cleanText = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            // Additional cleanup for any remaining markdown artifacts
            if (cleanText.startsWith("```") && cleanText.endsWith("```")) {
                cleanText = cleanText.substring(3, cleanText.length() - 3).trim();
            }
            
            // Try to parse as JSON first
            try {
                return parseReleaseNotes(cleanText);
            } catch (Exception e) {
                System.err.println("Failed to parse JSON response: " + e.getMessage());
                System.err.println("Raw response: " + text);
                // If not JSON, create a simple release notes object
                return createSimpleReleaseNotes(text);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate release notes", e);
        }
    }
    
    public ReleaseNotes generateReleaseNotesWithRetry(String systemPrompt, String userPrompt, int maxRetries) {
        Exception lastException = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                return generateReleaseNotes(systemPrompt, userPrompt);
            } catch (Exception e) {
                lastException = e;
                System.err.println("Attempt " + (i + 1) + " failed: " + e.getMessage());
                
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(1000 * (i + 1)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed to generate release notes after " + maxRetries + " attempts", lastException);
    }
    
    private ReleaseNotes parseReleaseNotes(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            
            String changelogMd = getStringValue(root, "changelog_md");
            String releaseNotesMd = getStringValue(root, "release_notes_md");
            String customerHighlightsMd = getStringValue(root, "customer_highlights_md");
            
            List<String> breakingChanges = getStringArray(root, "breaking_changes");
            List<String> upgradeSteps = getStringArray(root, "upgrade_steps");
            
            return new ReleaseNotes(changelogMd, releaseNotesMd, customerHighlightsMd, 
                                  breakingChanges, upgradeSteps);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse release notes JSON", e);
        }
    }
    
    private ReleaseNotes createSimpleReleaseNotes(String text) {
        // If the response is not JSON, create a simple release notes object
        // Clean up the text to remove JSON artifacts
        String cleanText = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        
        return new ReleaseNotes(
            cleanText, // changelog
            cleanText, // release notes
            cleanText, // customer highlights
            new ArrayList<>(), // breaking changes
            new ArrayList<>()  // upgrade steps
        );
    }
    
    private String getStringValue(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null ? node.asText() : "";
    }
    
    private List<String> getStringArray(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }
        
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(item.asText());
        }
        return result;
    }
    
    public void close() throws IOException {
        // Anthropic client doesn't need explicit closing
    }
}
