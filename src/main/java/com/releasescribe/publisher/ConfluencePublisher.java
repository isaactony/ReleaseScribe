package com.releasescribe.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasescribe.model.ReleaseNotes;
import okhttp3.*;

import java.io.IOException;
import java.util.Base64;

public class ConfluencePublisher implements Publisher {
    
    private static final String CONFLUENCE_API_VERSION = "application/json";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String username;
    private final String apiToken;
    private final String spaceKey;
    private final String parentPageId;
    
    public ConfluencePublisher(String baseUrl, String username, String apiToken, 
                              String spaceKey, String parentPageId) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiToken = apiToken;
        this.spaceKey = spaceKey;
        this.parentPageId = parentPageId;
    }
    
    @Override
    public void publish(ReleaseNotes releaseNotes, String version, String tag) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("Confluence Publisher not configured");
        }
        
        String pageTitle = "Release Notes - " + version;
        String pageContent = buildConfluenceContent(releaseNotes, version, tag);
        
        // Check if page already exists
        String existingPageId = findPageByTitle(pageTitle);
        if (existingPageId != null) {
            System.out.println("Page " + pageTitle + " already exists, updating...");
            updatePage(existingPageId, pageTitle, pageContent, version);
        } else {
            System.out.println("Creating new page " + pageTitle + "...");
            createPage(pageTitle, pageContent, version);
        }
    }
    
    @Override
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.trim().isEmpty() &&
               username != null && !username.trim().isEmpty() &&
               apiToken != null && !apiToken.trim().isEmpty() &&
               spaceKey != null && !spaceKey.trim().isEmpty() &&
               parentPageId != null && !parentPageId.trim().isEmpty();
    }
    
    @Override
    public String getName() {
        return "Confluence";
    }
    
    private String findPageByTitle(String title) throws IOException {
        String url = baseUrl + "/rest/api/content?title=" + title + "&spaceKey=" + spaceKey;
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", CONFLUENCE_API_VERSION)
                .addHeader("Authorization", getBasicAuth())
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode results = jsonNode.get("results");
            
            if (results.isArray() && results.size() > 0) {
                return results.get(0).get("id").asText();
            }
            
            return null;
        }
    }
    
    private void createPage(String title, String content, String version) throws IOException {
        String url = baseUrl + "/rest/api/content";
        
        String json = String.format("""
            {
                "type": "page",
                "title": "%s",
                "space": {
                    "key": "%s"
                },
                "ancestors": [
                    {
                        "id": "%s"
                    }
                ],
                "body": {
                    "storage": {
                        "value": %s,
                        "representation": "storage"
                    }
                }
            }
            """, title, spaceKey, parentPageId, objectMapper.writeValueAsString(content));
        
        RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Accept", CONFLUENCE_API_VERSION)
                .addHeader("Authorization", getBasicAuth())
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create page: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String pageUrl = jsonNode.get("_links").get("webui").asText();
            
            System.out.println("Page created successfully: " + baseUrl + pageUrl);
        }
    }
    
    private void updatePage(String pageId, String title, String content, String version) throws IOException {
        // First get the current page version
        String currentVersion = getPageVersion(pageId);
        int newVersion = Integer.parseInt(currentVersion) + 1;
        
        String url = baseUrl + "/rest/api/content/" + pageId;
        
        String json = String.format("""
            {
                "id": "%s",
                "type": "page",
                "title": "%s",
                "version": {
                    "number": %d
                },
                "body": {
                    "storage": {
                        "value": %s,
                        "representation": "storage"
                    }
                }
            }
            """, pageId, title, newVersion, objectMapper.writeValueAsString(content));
        
        RequestBody requestBody = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8"));
        
        Request request = new Request.Builder()
                .url(url)
                .put(requestBody)
                .addHeader("Accept", CONFLUENCE_API_VERSION)
                .addHeader("Authorization", getBasicAuth())
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to update page: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String pageUrl = jsonNode.get("_links").get("webui").asText();
            
            System.out.println("Page updated successfully: " + baseUrl + pageUrl);
        }
    }
    
    private String getPageVersion(String pageId) throws IOException {
        String url = baseUrl + "/rest/api/content/" + pageId;
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", CONFLUENCE_API_VERSION)
                .addHeader("Authorization", getBasicAuth())
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get page version: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("version").get("number").asText();
        }
    }
    
    private String buildConfluenceContent(ReleaseNotes releaseNotes, String version, String tag) {
        StringBuilder content = new StringBuilder();
        
        content.append("<h1>Release Notes - ").append(version).append("</h1>");
        content.append("<p><strong>Tag:</strong> ").append(tag).append("</p>");
        content.append("<p><strong>Generated:</strong> ").append(java.time.LocalDateTime.now()).append("</p>");
        
        // Add customer highlights first
        if (releaseNotes.getCustomerHighlightsMd() != null && !releaseNotes.getCustomerHighlightsMd().trim().isEmpty()) {
            content.append("<h2>What's New</h2>");
            content.append(convertMarkdownToConfluence(releaseNotes.getCustomerHighlightsMd()));
        }
        
        // Add breaking changes if any
        if (releaseNotes.getBreakingChanges() != null && !releaseNotes.getBreakingChanges().isEmpty()) {
            content.append("<h2>Breaking Changes</h2>");
            content.append("<ul>");
            for (String breakingChange : releaseNotes.getBreakingChanges()) {
                content.append("<li>").append(escapeHtml(breakingChange)).append("</li>");
            }
            content.append("</ul>");
        }
        
        // Add upgrade steps if any
        if (releaseNotes.getUpgradeSteps() != null && !releaseNotes.getUpgradeSteps().isEmpty()) {
            content.append("<h2>Upgrade Steps</h2>");
            content.append("<ul>");
            for (String upgradeStep : releaseNotes.getUpgradeSteps()) {
                content.append("<li>").append(escapeHtml(upgradeStep)).append("</li>");
            }
            content.append("</ul>");
        }
        
        // Add full changelog
        if (releaseNotes.getChangelogMd() != null && !releaseNotes.getChangelogMd().trim().isEmpty()) {
            content.append("<h2>Full Changelog</h2>");
            content.append(convertMarkdownToConfluence(releaseNotes.getChangelogMd()));
        }
        
        return content.toString();
    }
    
    private String convertMarkdownToConfluence(String markdown) {
        if (markdown == null) return "";
        
        // Simple markdown to Confluence storage format conversion
        String confluence = markdown
                .replaceAll("^# (.*)$", "<h1>$1</h1>")
                .replaceAll("^## (.*)$", "<h2>$1</h2>")
                .replaceAll("^### (.*)$", "<h3>$1</h3>")
                .replaceAll("^\\* (.*)$", "<li>$1</li>")
                .replaceAll("^\\- (.*)$", "<li>$1</li>")
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>")
                .replaceAll("\\*(.*?)\\*", "<em>$1</em>")
                .replaceAll("`(.*?)`", "<code>$1</code>")
                .replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");
        
        // Wrap consecutive list items in ul tags
        confluence = confluence.replaceAll("(<li>.*</li>\\s*)+", "<ul>$0</ul>");
        
        // Convert line breaks to <br>
        confluence = confluence.replaceAll("\\n", "<br>");
        
        return confluence;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private String getBasicAuth() {
        String credentials = username + ":" + apiToken;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encoded;
    }
}
