package com.releasescribe.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasescribe.model.ReleaseNotes;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackPublisher implements Publisher {
    
    private static final String SLACK_API_VERSION = "application/json";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String webhookUrl;
    private final String channel;
    private final String username;
    
    public SlackPublisher(String webhookUrl, String channel, String username) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.webhookUrl = webhookUrl;
        this.channel = channel;
        this.username = username;
    }
    
    @Override
    public void publish(ReleaseNotes releaseNotes, String version, String tag) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Slack Publisher not configured");
        }
        
        String message = buildSlackMessage(releaseNotes, version, tag);
        sendSlackMessage(message);
    }
    
    @Override
    public boolean isConfigured() {
        return webhookUrl != null && !webhookUrl.trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "Slack";
    }
    
    private void sendSlackMessage(String message) throws IOException {
        String json = String.format("""
            {
                "channel": "%s",
                "username": "%s",
                "text": %s,
                "icon_emoji": ":rocket:"
            }
            """, channel, username, objectMapper.writeValueAsString(message));
        
        RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .addHeader("Content-Type", SLACK_API_VERSION)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to send Slack message: " + response.code() + " " + response.message());
            }
            
            System.out.println("Slack message sent successfully");
        }
    }
    
    private String buildSlackMessage(ReleaseNotes releaseNotes, String version, String tag) {
        StringBuilder message = new StringBuilder();
        
        message.append(":rocket: *New Release: ").append(version).append("*\n");
        message.append("Tag: `").append(tag).append("`\n\n");
        
        // Add customer highlights (top 5)
        if (releaseNotes.getCustomerHighlightsMd() != null && !releaseNotes.getCustomerHighlightsMd().trim().isEmpty()) {
            message.append("*What's New:*\n");
            List<String> highlights = extractHighlights(releaseNotes.getCustomerHighlightsMd(), 5);
            for (String highlight : highlights) {
                message.append("• ").append(highlight).append("\n");
            }
            message.append("\n");
        }
        
        // Add breaking changes if any
        if (releaseNotes.getBreakingChanges() != null && !releaseNotes.getBreakingChanges().isEmpty()) {
            message.append(":warning: *Breaking Changes:*\n");
            for (String breakingChange : releaseNotes.getBreakingChanges()) {
                message.append("• ").append(breakingChange).append("\n");
            }
            message.append("\n");
        }
        
        // Add upgrade steps if any
        if (releaseNotes.getUpgradeSteps() != null && !releaseNotes.getUpgradeSteps().isEmpty()) {
            message.append(":arrow_up: *Upgrade Steps:*\n");
            for (String upgradeStep : releaseNotes.getUpgradeSteps()) {
                message.append("• ").append(upgradeStep).append("\n");
            }
            message.append("\n");
        }
        
        // Add link to full release notes
        message.append(":page_facing_up: View full release notes for complete details");
        
        return message.toString();
    }
    
    private List<String> extractHighlights(String highlightsMd, int maxItems) {
        List<String> highlights = new ArrayList<>();
        String[] lines = highlightsMd.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- ") || line.startsWith("* ")) {
                String highlight = line.substring(2).trim();
                if (!highlight.isEmpty()) {
                    highlights.add(highlight);
                    if (highlights.size() >= maxItems) {
                        break;
                    }
                }
            }
        }
        
        return highlights;
    }
}
