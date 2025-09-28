package com.releasescribe.normalizer;

import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultNormalizer implements Normalizer {
    
    private static final Pattern CONVENTIONAL_COMMIT = Pattern.compile(
        "^(feat|fix|docs|style|refactor|perf|test|chore|security)(\\(([^)]+)\\))?: (.+)$"
    );
    
    private static final Pattern BREAKING_CHANGE = Pattern.compile(
        "(?i)(breaking|breaking.?change|major)"
    );
    
    @Override
    public List<PullRequest> normalizePullRequests(List<PullRequest> pullRequests, 
                                                  Map<String, List<String>> labelMapping,
                                                  Map<String, List<String>> componentPaths) {
        return pullRequests.stream()
                .map(pr -> {
                    // Categorize based on labels and title
                    String category = categorize(pr.getTitle(), pr.getBody(), pr.getLabels(), labelMapping);
                    pr.setCategory(category);
                    
                    // Detect breaking changes
                    boolean isBreaking = pr.getLabels().contains("breaking-change") ||
                                       pr.getLabels().contains("breaking") ||
                                       BREAKING_CHANGE.matcher(pr.getTitle()).find() ||
                                       BREAKING_CHANGE.matcher(pr.getBody()).find();
                    pr.setBreakingChange(isBreaking);
                    
                    // Detect component from title (conventional commit scope)
                    String component = detectComponentFromTitle(pr.getTitle());
                    if (component == null) {
                        component = "general";
                    }
                    pr.setComponent(component);
                    
                    return pr;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Commit> normalizeCommits(List<Commit> commits, 
                                        Map<String, List<String>> componentPaths) {
        return commits.stream()
                .map(commit -> {
                    // Parse conventional commit format
                    String message = commit.getMessage();
                    Matcher matcher = CONVENTIONAL_COMMIT.matcher(message);
                    
                    if (matcher.matches()) {
                        String type = matcher.group(1);
                        String scope = matcher.group(3);
                        String description = matcher.group(4);
                        
                        // Map conventional commit types to categories
                        switch (type) {
                            case "feat":
                                commit.setCategory("feature");
                                break;
                            case "fix":
                                commit.setCategory("fix");
                                break;
                            case "docs":
                                commit.setCategory("docs");
                                break;
                            case "perf":
                                commit.setCategory("perf");
                                break;
                            case "security":
                                commit.setCategory("security");
                                break;
                            case "refactor":
                                commit.setCategory("refactor");
                                break;
                            default:
                                commit.setCategory("other");
                        }
                        
                        commit.setComponent(scope != null ? scope : "general");
                    } else {
                        // Fallback heuristics
                        String category = categorizeFromMessage(message);
                        commit.setCategory(category);
                        commit.setComponent("general");
                    }
                    
                    return commit;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public String detectComponent(List<String> changedFiles, 
                                 Map<String, List<String>> componentPaths) {
        if (changedFiles == null || changedFiles.isEmpty()) {
            return "general";
        }
        
        // Count matches for each component
        Map<String, Integer> componentMatches = new HashMap<>();
        
        for (String file : changedFiles) {
            for (Map.Entry<String, List<String>> entry : componentPaths.entrySet()) {
                String component = entry.getKey();
                List<String> patterns = entry.getValue();
                
                for (String pattern : patterns) {
                    if (file.matches(pattern.replace("**", ".*").replace("*", "[^/]*"))) {
                        componentMatches.merge(component, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Return component with most matches
        return componentMatches.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("general");
    }
    
    @Override
    public String categorize(String title, String body, List<String> labels, 
                           Map<String, List<String>> labelMapping) {
        // First, try to categorize based on labels
        for (Map.Entry<String, List<String>> entry : labelMapping.entrySet()) {
            String category = entry.getKey();
            List<String> categoryLabels = entry.getValue();
            
            for (String label : labels) {
                if (categoryLabels.contains(label.toLowerCase())) {
                    return category;
                }
            }
        }
        
        // Fallback to title/body heuristics
        String text = (title + " " + (body != null ? body : "")).toLowerCase();
        
        if (text.contains("feat") || text.contains("feature") || text.contains("enhancement")) {
            return "feature";
        } else if (text.contains("fix") || text.contains("bug") || text.contains("issue")) {
            return "fix";
        } else if (text.contains("security") || text.contains("cve") || text.contains("vulnerability")) {
            return "security";
        } else if (text.contains("perf") || text.contains("performance") || text.contains("optimize")) {
            return "perf";
        } else if (text.contains("docs") || text.contains("documentation")) {
            return "docs";
        } else if (text.contains("refactor") || text.contains("cleanup")) {
            return "refactor";
        } else {
            return "other";
        }
    }
    
    @Override
    public List<PullRequest> deduplicatePullRequests(List<PullRequest> pullRequests) {
        // Remove exact duplicates (same PR number)
        Set<Integer> seen = new HashSet<>();
        return pullRequests.stream()
                .filter(pr -> seen.add(pr.getNumber()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Commit> deduplicateCommits(List<Commit> commits) {
        // Remove exact duplicates (same hash)
        Set<String> seen = new HashSet<>();
        return commits.stream()
                .filter(commit -> seen.add(commit.getHash()))
                .collect(Collectors.toList());
    }
    
    private String detectComponentFromTitle(String title) {
        Matcher matcher = CONVENTIONAL_COMMIT.matcher(title);
        if (matcher.matches()) {
            return matcher.group(3); // scope
        }
        return null;
    }
    
    private String categorizeFromMessage(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.startsWith("feat") || lowerMessage.contains("feature")) {
            return "feature";
        } else if (lowerMessage.startsWith("fix") || lowerMessage.contains("bug")) {
            return "fix";
        } else if (lowerMessage.contains("security") || lowerMessage.contains("cve")) {
            return "security";
        } else if (lowerMessage.contains("perf") || lowerMessage.contains("performance")) {
            return "perf";
        } else if (lowerMessage.contains("docs") || lowerMessage.contains("documentation")) {
            return "docs";
        } else if (lowerMessage.contains("refactor") || lowerMessage.contains("cleanup")) {
            return "refactor";
        } else {
            return "other";
        }
    }
}
