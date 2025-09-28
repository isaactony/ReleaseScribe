package com.releasescribe.collector;

import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitCollector implements ScmCollector {
    
    private static final Pattern CONVENTIONAL_COMMIT = Pattern.compile(
        "^(feat|fix|docs|style|refactor|perf|test|chore|security)(\\(([^)]+)\\))?: (.+)$"
    );
    
    private final String repoPath;
    
    public GitCollector(String repoPath) {
        this.repoPath = repoPath;
    }
    
    @Override
    public List<PullRequest> collectPullRequests(String owner, String repo, 
                                               LocalDateTime since, LocalDateTime until) {
        // Git collector doesn't have PR information, return empty list
        return new ArrayList<>();
    }
    
    @Override
    public List<PullRequest> collectPullRequestsByTags(String owner, String repo, 
                                                      String sinceTag, String untilTag) {
        // Git collector doesn't have PR information, return empty list
        return new ArrayList<>();
    }
    
    @Override
    public List<PullRequest> collectPullRequestsByRange(String owner, String repo, 
                                                       String base, String head) {
        // Git collector doesn't have PR information, return empty list
        return new ArrayList<>();
    }
    
    @Override
    public List<Commit> collectCommits(String owner, String repo, 
                                      LocalDateTime since, LocalDateTime until) {
        try (Repository repository = openRepository()) {
            Git git = new Git(repository);
            
            LogCommand logCommand = git.log();
            
            // Note: JGit LogCommand doesn't have setSince/setUntil methods
            // We'll filter commits manually after fetching them
            // logCommand.setSince(java.sql.Timestamp.valueOf(since));
            // logCommand.setUntil(java.sql.Timestamp.valueOf(until));
            
            Iterable<RevCommit> commits = logCommand.call();
            
            List<Commit> result = new ArrayList<>();
            for (RevCommit revCommit : commits) {
                Commit commit = parseCommit(revCommit);
                
                // Filter by date range manually
                if (commit.getDate().isAfter(since.minusSeconds(1)) && 
                    commit.getDate().isBefore(until.plusSeconds(1))) {
                    result.add(commit);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits", e);
        }
    }
    
    @Override
    public List<Commit> collectCommitsByTags(String owner, String repo, 
                                            String sinceTag, String untilTag) {
        try (Repository repository = openRepository()) {
            Git git = new Git(repository);
            
            ObjectId sinceId = repository.resolve(sinceTag);
            ObjectId untilId = repository.resolve(untilTag);
            
            if (sinceId == null || untilId == null) {
                throw new RuntimeException("Could not resolve tags: " + sinceTag + " or " + untilTag);
            }
            
            LogCommand logCommand = git.log();
            logCommand.addRange(sinceId, untilId);
            
            Iterable<RevCommit> commits = logCommand.call();
            
            List<Commit> result = new ArrayList<>();
            for (RevCommit revCommit : commits) {
                Commit commit = parseCommit(revCommit);
                result.add(commit);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits by tags", e);
        }
    }
    
    @Override
    public List<Commit> collectCommitsByRange(String owner, String repo, 
                                             String base, String head) {
        try (Repository repository = openRepository()) {
            Git git = new Git(repository);
            
            ObjectId baseId = repository.resolve(base);
            ObjectId headId = repository.resolve(head);
            
            if (baseId == null || headId == null) {
                throw new RuntimeException("Could not resolve refs: " + base + " or " + head);
            }
            
            LogCommand logCommand = git.log();
            logCommand.addRange(baseId, headId);
            
            Iterable<RevCommit> commits = logCommand.call();
            
            List<Commit> result = new ArrayList<>();
            for (RevCommit revCommit : commits) {
                Commit commit = parseCommit(revCommit);
                result.add(commit);
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to collect commits by range", e);
        }
    }
    
    private Repository openRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(new File(repoPath, ".git"))
                     .readEnvironment()
                     .findGitDir()
                     .build();
    }
    
    private Commit parseCommit(RevCommit revCommit) {
        Commit commit = new Commit();
        commit.setHash(revCommit.getId().getName());
        commit.setMessage(revCommit.getFullMessage());
        commit.setAuthor(revCommit.getAuthorIdent().getName());
        commit.setDate(LocalDateTime.ofInstant(
            revCommit.getAuthorIdent().getWhen().toInstant(), 
            ZoneOffset.UTC
        ));
        
        // Parse conventional commit format
        String message = revCommit.getShortMessage();
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
            
            commit.setComponent(scope);
        } else {
            // Fallback heuristics
            if (message.toLowerCase().startsWith("feat") || 
                message.toLowerCase().contains("feature")) {
                commit.setCategory("feature");
            } else if (message.toLowerCase().startsWith("fix") || 
                      message.toLowerCase().contains("bug")) {
                commit.setCategory("fix");
            } else if (message.toLowerCase().contains("security") || 
                      message.toLowerCase().contains("cve")) {
                commit.setCategory("security");
            } else if (message.toLowerCase().contains("perf") || 
                      message.toLowerCase().contains("performance")) {
                commit.setCategory("perf");
            } else {
                commit.setCategory("other");
            }
        }
        
        return commit;
    }
}
