package com.releasescribe.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasescribe.model.ReleaseNotes;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GitHubReleasePublisher implements Publisher {
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_API_VERSION = "application/vnd.github.v3+json";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String owner;
    private final String repo;
    
    public GitHubReleasePublisher(String token, String owner, String repo) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.token = token;
        this.owner = owner;
        this.repo = repo;
    }
    
    @Override
    public void publish(ReleaseNotes releaseNotes, String version, String tag) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("GitHub Release Publisher not configured");
        }
        
        String releaseBody = buildReleaseBody(releaseNotes);
        
        // Check if release already exists
        if (releaseExists(tag)) {
            System.out.println("Release " + tag + " already exists, updating...");
            updateRelease(tag, version, releaseBody);
        } else {
            System.out.println("Creating new release " + tag + "...");
            createRelease(tag, version, releaseBody);
        }
    }
    
    @Override
    public boolean isConfigured() {
        return token != null && !token.trim().isEmpty() &&
               owner != null && !owner.trim().isEmpty() &&
               repo != null && !repo.trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "GitHub Release";
    }
    
    private boolean releaseExists(String tag) throws IOException {
        String url = String.format("%s/repos/%s/%s/releases/tags/%s", 
                GITHUB_API_BASE, owner, repo, tag);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }
    
    private void createRelease(String tag, String version, String body) throws IOException {
        String url = String.format("%s/repos/%s/%s/releases", GITHUB_API_BASE, owner, repo);
        
        String json = String.format("""
            {
                "tag_name": "%s",
                "target_commitish": "main",
                "name": "%s",
                "body": %s,
                "draft": false,
                "prerelease": false
            }
            """, tag, version, objectMapper.writeValueAsString(body));
        
        RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create release: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String releaseUrl = jsonNode.get("html_url").asText();
            
            System.out.println("Release created successfully: " + releaseUrl);
        }
    }
    
    private void updateRelease(String tag, String version, String body) throws IOException {
        // First get the release ID
        String releaseId = getReleaseIdByTag(tag);
        if (releaseId == null) {
            throw new IOException("Could not find release ID for tag: " + tag);
        }
        
        String url = String.format("%s/repos/%s/%s/releases/%s", 
                GITHUB_API_BASE, owner, repo, releaseId);
        
        String json = String.format("""
            {
                "tag_name": "%s",
                "name": "%s",
                "body": %s
            }
            """, tag, version, objectMapper.writeValueAsString(body));
        
        RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .patch(requestBody)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to update release: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String releaseUrl = jsonNode.get("html_url").asText();
            
            System.out.println("Release updated successfully: " + releaseUrl);
        }
    }
    
    private String getReleaseIdByTag(String tag) throws IOException {
        String url = String.format("%s/repos/%s/%s/releases/tags/%s", 
                GITHUB_API_BASE, owner, repo, tag);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("id").asText();
        }
    }
    
    private String buildReleaseBody(ReleaseNotes releaseNotes) {
        StringBuilder body = new StringBuilder();
        
        // Add customer highlights first
        if (releaseNotes.getCustomerHighlightsMd() != null && !releaseNotes.getCustomerHighlightsMd().trim().isEmpty()) {
            body.append("## What's New\n\n");
            body.append(releaseNotes.getCustomerHighlightsMd()).append("\n\n");
        }
        
        // Add breaking changes if any
        if (releaseNotes.getBreakingChanges() != null && !releaseNotes.getBreakingChanges().isEmpty()) {
            body.append("## Breaking Changes\n\n");
            for (String breakingChange : releaseNotes.getBreakingChanges()) {
                body.append("- ").append(breakingChange).append("\n");
            }
            body.append("\n");
        }
        
        // Add upgrade steps if any
        if (releaseNotes.getUpgradeSteps() != null && !releaseNotes.getUpgradeSteps().isEmpty()) {
            body.append("## Upgrade Steps\n\n");
            for (String upgradeStep : releaseNotes.getUpgradeSteps()) {
                body.append("- ").append(upgradeStep).append("\n");
            }
            body.append("\n");
        }
        
        // Add full changelog
        if (releaseNotes.getChangelogMd() != null && !releaseNotes.getChangelogMd().trim().isEmpty()) {
            body.append("## Full Changelog\n\n");
            body.append(releaseNotes.getChangelogMd());
        }
        
        return body.toString();
    }
}
