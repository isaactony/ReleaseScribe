package com.releasescribe.collector;

import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface ScmCollector {
    
    /**
     * Collect pull requests within the specified range
     */
    List<PullRequest> collectPullRequests(String owner, String repo, 
                                         LocalDateTime since, LocalDateTime until);
    
    /**
     * Collect pull requests between tags
     */
    List<PullRequest> collectPullRequestsByTags(String owner, String repo, 
                                               String sinceTag, String untilTag);
    
    /**
     * Collect pull requests between commits/branches
     */
    List<PullRequest> collectPullRequestsByRange(String owner, String repo, 
                                                String base, String head);
    
    /**
     * Collect commits within the specified range
     */
    List<Commit> collectCommits(String owner, String repo, 
                               LocalDateTime since, LocalDateTime until);
    
    /**
     * Collect commits between tags
     */
    List<Commit> collectCommitsByTags(String owner, String repo, 
                                     String sinceTag, String untilTag);
    
    /**
     * Collect commits between commits/branches
     */
    List<Commit> collectCommitsByRange(String owner, String repo, 
                                      String base, String head);
}

