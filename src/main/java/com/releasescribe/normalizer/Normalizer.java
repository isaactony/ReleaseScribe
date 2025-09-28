package com.releasescribe.normalizer;

import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Normalizer {
    
    /**
     * Normalize and categorize pull requests
     */
    List<PullRequest> normalizePullRequests(List<PullRequest> pullRequests, 
                                          Map<String, List<String>> labelMapping,
                                          Map<String, List<String>> componentPaths);
    
    /**
     * Normalize and categorize commits
     */
    List<Commit> normalizeCommits(List<Commit> commits, 
                                Map<String, List<String>> componentPaths);
    
    /**
     * Detect component from file paths
     */
    String detectComponent(List<String> changedFiles, 
                          Map<String, List<String>> componentPaths);
    
    /**
     * Categorize based on labels and heuristics
     */
    String categorize(String title, String body, List<String> labels, 
                     Map<String, List<String>> labelMapping);
    
    /**
     * Remove duplicates and merge similar items
     */
    List<PullRequest> deduplicatePullRequests(List<PullRequest> pullRequests);
    
    /**
     * Remove duplicates and merge similar items
     */
    List<Commit> deduplicateCommits(List<Commit> commits);
}
