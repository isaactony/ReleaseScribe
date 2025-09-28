package com.releasescribe.prompt;

import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PromptBuilder {
    
    private static final String SYSTEM_PROMPT = """
        You are a release notes generator. Return ONLY valid JSON with these exact fields:
        {
          "changelog_md": "markdown changelog content",
          "release_notes_md": "markdown release notes content", 
          "customer_highlights_md": "markdown customer highlights content",
          "breaking_changes": ["array of breaking changes"],
          "upgrade_steps": ["array of upgrade steps"]
        }
        
        Follow Keep a Changelog style; be concise and accurate. No code inventions.
        Return ONLY the JSON object, no other text.
        """;
    
    private static final String PROMPT_TEMPLATE = """
        Repo: %s/%s; Range: %s
        
        Audience profiles: dev (engineering), customer (non-technical).
        
        Categories & mapping: %s
        
        Data (compact):
        
        %s
        
        Breaking flags: PRs labelled breaking-change → include summary.
        
        Upgrade hints gathered from PR bodies (migration notes).
        
        Constraints:
        - Group by category and component; link PRs/issues like [#123].
        - Add a short "What's in this release" overview (3–5 bullets).
        - Keep customer highlights to 5–8 bullets max, value-oriented.
        - Return JSON with the fields listed in System.
        """;
    
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }
    
    public String buildUserPrompt(String owner, String repo, String range, 
                                 Map<String, List<String>> labelMapping,
                                 List<PullRequest> pullRequests, 
                                 List<Commit> commits) {
        
        String categoriesMapping = buildCategoriesMapping(labelMapping);
        String dataSection = buildDataSection(pullRequests, commits);
        
        return String.format(PROMPT_TEMPLATE, owner, repo, range, categoriesMapping, dataSection);
    }
    
    private String buildCategoriesMapping(Map<String, List<String>> labelMapping) {
        return labelMapping.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("; "));
    }
    
    private String buildDataSection(List<PullRequest> pullRequests, List<Commit> commits) {
        StringBuilder data = new StringBuilder();
        
        // Group PRs by category
        Map<String, List<PullRequest>> prsByCategory = pullRequests.stream()
                .collect(Collectors.groupingBy(pr -> pr.getCategory() != null ? pr.getCategory() : "other"));
        
        // Group commits by category
        Map<String, List<Commit>> commitsByCategory = commits.stream()
                .collect(Collectors.groupingBy(commit -> commit.getCategory() != null ? commit.getCategory() : "other"));
        
        // Build features section
        if (prsByCategory.containsKey("feature")) {
            data.append("Features:\n");
            for (PullRequest pr : prsByCategory.get("feature")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Build fixes section
        if (prsByCategory.containsKey("fix")) {
            data.append("Fixes:\n");
            for (PullRequest pr : prsByCategory.get("fix")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Build security section
        if (prsByCategory.containsKey("security")) {
            data.append("Security:\n");
            for (PullRequest pr : prsByCategory.get("security")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Build performance section
        if (prsByCategory.containsKey("perf")) {
            data.append("Performance:\n");
            for (PullRequest pr : prsByCategory.get("perf")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Build docs section
        if (prsByCategory.containsKey("docs")) {
            data.append("Documentation:\n");
            for (PullRequest pr : prsByCategory.get("docs")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Build refactor section
        if (prsByCategory.containsKey("refactor")) {
            data.append("Refactoring:\n");
            for (PullRequest pr : prsByCategory.get("refactor")) {
                data.append(String.format("  [#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor()));
            }
        }
        
        // Add commits that don't have associated PRs
        if (!commits.isEmpty()) {
            data.append("Commits:\n");
            for (Commit commit : commits) {
                data.append(String.format("  %s: %s — @%s\n", 
                    commit.getHash().substring(0, 7), 
                    commit.getMessage().split("\n")[0], 
                    commit.getAuthor()));
            }
        }
        
        return data.toString();
    }
    
    public String buildCompactContext(List<PullRequest> pullRequests, List<Commit> commits, int maxTokens) {
        StringBuilder context = new StringBuilder();
        int currentTokens = 0;
        
        // Add PRs first (they're more informative)
        for (PullRequest pr : pullRequests) {
            String prText = String.format("[#%d] %s — @%s\n", pr.getNumber(), pr.getTitle(), pr.getAuthor());
            if (currentTokens + prText.length() > maxTokens) {
                break;
            }
            context.append(prText);
            currentTokens += prText.length();
        }
        
        // Add commits if we have space
        for (Commit commit : commits) {
            String commitText = String.format("%s: %s — @%s\n", 
                commit.getHash().substring(0, 7), 
                commit.getMessage().split("\n")[0], 
                commit.getAuthor());
            if (currentTokens + commitText.length() > maxTokens) {
                break;
            }
            context.append(commitText);
            currentTokens += commitText.length();
        }
        
        return context.toString();
    }
    
    public String buildBreakingChangesSection(List<PullRequest> pullRequests) {
        List<PullRequest> breakingPRs = pullRequests.stream()
                .filter(PullRequest::isBreakingChange)
                .collect(Collectors.toList());
        
        if (breakingPRs.isEmpty()) {
            return "No breaking changes detected.";
        }
        
        StringBuilder breaking = new StringBuilder("Breaking Changes:\n");
        for (PullRequest pr : breakingPRs) {
            breaking.append(String.format("- [#%d] %s\n", pr.getNumber(), pr.getTitle()));
        }
        
        return breaking.toString();
    }
    
    public String buildUpgradeStepsSection(List<PullRequest> pullRequests) {
        StringBuilder upgrade = new StringBuilder("Upgrade Steps:\n");
        
        // Look for migration notes in PR bodies
        for (PullRequest pr : pullRequests) {
            if (pr.getBody() != null && pr.getBody().toLowerCase().contains("migration")) {
                upgrade.append(String.format("- [#%d] %s: Check migration notes\n", pr.getNumber(), pr.getTitle()));
            }
        }
        
        if (upgrade.length() == "Upgrade Steps:\n".length()) {
            upgrade.append("- No specific upgrade steps required");
        }
        
        return upgrade.toString();
    }
}
