package com.releasescribe.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GitHubCollector implements ScmCollector {
    
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String GITHUB_API_VERSION = "application/vnd.github.v3+json";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;
    
    public GitHubCollector(String token) {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.token = token;
    }
    
    @Override
    public List<PullRequest> collectPullRequests(String owner, String repo, 
                                               LocalDateTime since, LocalDateTime until) {
        try {
            // Get commits in range first
            List<Commit> commits = collectCommits(owner, repo, since, until);
            
            // For each commit, get associated PRs
            List<PullRequest> allPRs = new ArrayList<>();
            for (Commit commit : commits) {
                List<PullRequest> prs = getPullRequestsForCommit(owner, repo, commit.getHash());
                allPRs.addAll(prs);
            }
            
            // Remove duplicates and filter by date range
            return allPRs.stream()
                    .distinct()
                    .filter(pr -> pr.getMergedAt() != null)
                    .filter(pr -> !pr.getMergedAt().isBefore(since) && !pr.getMergedAt().isAfter(until))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect pull requests", e);
        }
    }
    
    @Override
    public List<PullRequest> collectPullRequestsByTags(String owner, String repo, 
                                                      String sinceTag, String untilTag) {
        try {
            // Get commits between tags
            List<Commit> commits = collectCommitsByTags(owner, repo, sinceTag, untilTag);
            
            // For each commit, get associated PRs
            List<PullRequest> allPRs = new ArrayList<>();
            for (Commit commit : commits) {
                List<PullRequest> prs = getPullRequestsForCommit(owner, repo, commit.getHash());
                allPRs.addAll(prs);
            }
            
            return allPRs.stream().distinct().collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect pull requests by tags", e);
        }
    }
    
    @Override
    public List<PullRequest> collectPullRequestsByRange(String owner, String repo, 
                                                       String base, String head) {
        try {
            // Get commits between base and head
            List<Commit> commits = collectCommitsByRange(owner, repo, base, head);
            
            // For each commit, get associated PRs
            List<PullRequest> allPRs = new ArrayList<>();
            for (Commit commit : commits) {
                List<PullRequest> prs = getPullRequestsForCommit(owner, repo, commit.getHash());
                allPRs.addAll(prs);
            }
            
            return allPRs.stream().distinct().collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect pull requests by range", e);
        }
    }
    
    @Override
    public List<Commit> collectCommits(String owner, String repo, 
                                      LocalDateTime since, LocalDateTime until) {
        try {
            String sinceStr = since.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            String untilStr = until.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
            
            String url = String.format("%s/repos/%s/%s/commits?since=%s&until=%s&per_page=100",
                    GITHUB_API_BASE, owner, repo, sinceStr, untilStr);
            
            return fetchCommits(url);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits", e);
        }
    }
    
    @Override
    public List<Commit> collectCommitsByTags(String owner, String repo, 
                                            String sinceTag, String untilTag) {
        try {
            String url = String.format("%s/repos/%s/%s/compare/%s...%s?per_page=100",
                    GITHUB_API_BASE, owner, repo, sinceTag, untilTag);
            
            return fetchCommitsFromCompare(url);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits by tags", e);
        }
    }
    
    @Override
    public List<Commit> collectCommitsByRange(String owner, String repo, 
                                             String base, String head) {
        try {
            String url = String.format("%s/repos/%s/%s/compare/%s...%s?per_page=100",
                    GITHUB_API_BASE, owner, repo, base, head);
            
            return fetchCommitsFromCompare(url);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits by range", e);
        }
    }
    
    private List<PullRequest> getPullRequestsForCommit(String owner, String repo, String commitHash) {
        try {
            String url = String.format("%s/repos/%s/%s/commits/%s/pulls",
                    GITHUB_API_BASE, owner, repo, commitHash);
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", GITHUB_API_VERSION)
                    .addHeader("Authorization", "token " + token)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return new ArrayList<>();
                }
                
                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                List<PullRequest> prs = new ArrayList<>();
                for (JsonNode prNode : jsonNode) {
                    PullRequest pr = parsePullRequest(prNode);
                    prs.add(pr);
                }
                
                return prs;
            }
            
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private List<Commit> fetchCommits(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch commits: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            List<Commit> commits = new ArrayList<>();
            for (JsonNode commitNode : jsonNode) {
                Commit commit = parseCommit(commitNode);
                commits.add(commit);
            }
            
            return commits;
        }
    }
    
    private List<Commit> fetchCommitsFromCompare(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", GITHUB_API_VERSION)
                .addHeader("Authorization", "token " + token)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch commits: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode commitsNode = jsonNode.get("commits");
            
            List<Commit> commits = new ArrayList<>();
            for (JsonNode commitNode : commitsNode) {
                Commit commit = parseCommit(commitNode);
                commits.add(commit);
            }
            
            return commits;
        }
    }
    
    private PullRequest parsePullRequest(JsonNode prNode) {
        PullRequest pr = new PullRequest();
        pr.setNumber(prNode.get("number").asInt());
        pr.setTitle(prNode.get("title").asText());
        pr.setBody(prNode.has("body") ? prNode.get("body").asText() : "");
        pr.setAuthor(prNode.get("user").get("login").asText());
        
        if (prNode.has("merged_at") && !prNode.get("merged_at").isNull()) {
            String mergedAtStr = prNode.get("merged_at").asText();
            pr.setMergedAt(LocalDateTime.parse(mergedAtStr.substring(0, 19)));
        }
        
        // Parse labels
        List<String> labels = new ArrayList<>();
        if (prNode.has("labels")) {
            for (JsonNode labelNode : prNode.get("labels")) {
                labels.add(labelNode.get("name").asText());
            }
        }
        pr.setLabels(labels);
        
        // Check for breaking change label
        pr.setBreakingChange(labels.contains("breaking-change") || 
                           labels.contains("breaking") ||
                           pr.getTitle().toLowerCase().contains("breaking"));
        
        return pr;
    }
    
    private Commit parseCommit(JsonNode commitNode) {
        Commit commit = new Commit();
        commit.setHash(commitNode.get("sha").asText());
        
        JsonNode commitInfo = commitNode.get("commit");
        commit.setMessage(commitInfo.get("message").asText());
        commit.setAuthor(commitInfo.get("author").get("name").asText());
        
        String dateStr = commitInfo.get("author").get("date").asText();
        commit.setDate(LocalDateTime.parse(dateStr.substring(0, 19)));
        
        return commit;
    }
}
