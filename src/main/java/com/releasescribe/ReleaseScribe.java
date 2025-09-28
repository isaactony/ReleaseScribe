package com.releasescribe;

import com.releasescribe.client.AnthropicReleaseNotesClient;
import com.releasescribe.collector.GitHubCollector;
import com.releasescribe.collector.GitCollector;
import com.releasescribe.collector.ScmCollector;
import com.releasescribe.config.ConfigLoader;
import com.releasescribe.config.ReleaseScribeConfig;
import com.releasescribe.model.Commit;
import com.releasescribe.model.PullRequest;
import com.releasescribe.model.ReleaseNotes;
import com.releasescribe.normalizer.DefaultNormalizer;
import com.releasescribe.normalizer.Normalizer;
import com.releasescribe.prompt.PromptBuilder;
import com.releasescribe.publisher.ConfluencePublisher;
import com.releasescribe.publisher.GitHubReleasePublisher;
import com.releasescribe.publisher.Publisher;
import com.releasescribe.publisher.SlackPublisher;
import com.releasescribe.renderer.MarkdownRenderer;
import com.releasescribe.renderer.Renderer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "relnotes",
    mixinStandardHelpOptions = true,
    version = "ReleaseScribe 1.0.0",
    description = "AI-powered release notes generator using Anthropic Claude"
)
public class ReleaseScribe implements Callable<Integer> {

    @Option(
        names = {"--provider"},
        description = "SCM provider: github, gitlab, git",
        defaultValue = "github"
    )
    private String provider;

    @Option(
        names = {"--owner"},
        description = "Repository owner/organization"
    )
    private String owner;

    @Option(
        names = {"--repo"},
        description = "Repository name"
    )
    private String repo;

    @Option(
        names = {"--since-tag"},
        description = "Start tag (e.g., v1.2.0)"
    )
    private String sinceTag;

    @Option(
        names = {"--until-tag"},
        description = "End tag (e.g., v1.3.0)"
    )
    private String untilTag;

    @Option(
        names = {"--since"},
        description = "Start date (YYYY-MM-DD)"
    )
    private String sinceDate;

    @Option(
        names = {"--until"},
        description = "End date (YYYY-MM-DD)"
    )
    private String untilDate;

    @Option(
        names = {"--base"},
        description = "Base branch/commit"
    )
    private String base;

    @Option(
        names = {"--head"},
        description = "Head branch/commit"
    )
    private String head;

    @Option(
        names = {"--out-dir"},
        description = "Output directory",
        defaultValue = "dist"
    )
    private String outDir;

    @Option(
        names = {"--config"},
        description = "Configuration file path",
        defaultValue = ".relnotes.yml"
    )
    private String configFile;

    @Option(
        names = {"--publish-github-release"},
        description = "Publish to GitHub Release",
        defaultValue = "false"
    )
    private boolean publishGitHubRelease;

    @Option(
        names = {"--publish-confluence"},
        description = "Publish to Confluence",
        defaultValue = "false"
    )
    private boolean publishConfluence;

    @Option(
        names = {"--publish-slack"},
        description = "Publish to Slack",
        defaultValue = "false"
    )
    private boolean publishSlack;

    @Option(
        names = {"--dry-run"},
        description = "Show what would be done without executing",
        defaultValue = "false"
    )
    private boolean dryRun;

    @Option(
        names = {"--verbose"},
        description = "Enable verbose output",
        defaultValue = "false"
    )
    private boolean verbose;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ReleaseScribe()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("ReleaseScribe - AI-powered release notes generator");
        
        // Validate required parameters
        if (owner == null || repo == null) {
            System.err.println("Error: --owner and --repo are required");
            return 1;
        }
        
        // Load configuration
        ReleaseScribeConfig config = ConfigLoader.loadConfig(Paths.get(configFile));
        if (verbose) {
            System.out.println("Loaded configuration from: " + configFile);
        }
        
        // Initialize components
        ScmCollector collector = createScmCollector();
        Normalizer normalizer = new DefaultNormalizer();
        PromptBuilder promptBuilder = new PromptBuilder();
        AnthropicReleaseNotesClient anthropicClient = new AnthropicReleaseNotesClient(System.getenv("ANTHROPIC_API_KEY"));
        Renderer renderer = new MarkdownRenderer();
        
        try {
            // Collect data
            System.out.println("Collecting pull requests and commits...");
            List<PullRequest> pullRequests = collectPullRequests(collector);
            List<Commit> commits = collectCommits(collector);
            
            if (pullRequests.isEmpty() && commits.isEmpty()) {
                System.out.println("No pull requests or commits found in the specified range");
                return 0;
            }
            
            System.out.println("Found " + pullRequests.size() + " pull requests and " + commits.size() + " commits");
            
            // Normalize data
            System.out.println("Normalizing and categorizing data...");
            pullRequests = normalizer.normalizePullRequests(pullRequests, config.getLabelMapping(), 
                    config.getConventions().getComponentPaths());
            commits = normalizer.normalizeCommits(commits, config.getConventions().getComponentPaths());
            
            // Deduplicate
            pullRequests = normalizer.deduplicatePullRequests(pullRequests);
            commits = normalizer.deduplicateCommits(commits);
            
            // Build prompts
            String range = buildRangeDescription();
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(owner, repo, range, 
                    config.getLabelMapping(), pullRequests, commits);
            
            if (verbose) {
                System.out.println("System prompt length: " + systemPrompt.length());
                System.out.println("User prompt length: " + userPrompt.length());
            }
            
            // Generate release notes
            System.out.println("Generating release notes with Claude...");
            ReleaseNotes releaseNotes = anthropicClient.generateReleaseNotesWithRetry(systemPrompt, userPrompt, 3);
            
            // Render files
            System.out.println("Rendering output files...");
            Path outputPath = Paths.get(outDir);
            renderer.render(releaseNotes, outputPath);
            
            // Publish if requested
            if (publishGitHubRelease || publishConfluence || publishSlack) {
                System.out.println("Publishing release notes...");
                publishReleaseNotes(releaseNotes, config);
            }
            
            System.out.println("Release notes generated successfully!");
            System.out.println("Output directory: " + outputPath.toAbsolutePath());
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        } finally {
            anthropicClient.close();
        }
    }
    
    private ScmCollector createScmCollector() {
        switch (provider.toLowerCase()) {
            case "github":
                String githubToken = System.getenv("GITHUB_TOKEN");
                if (githubToken == null) {
                    throw new IllegalStateException("GITHUB_TOKEN environment variable is required for GitHub provider");
                }
                return new GitHubCollector(githubToken);
            case "git":
                return new GitCollector(".");
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
    
    private List<PullRequest> collectPullRequests(ScmCollector collector) {
        if (sinceTag != null && untilTag != null) {
            return collector.collectPullRequestsByTags(owner, repo, sinceTag, untilTag);
        } else if (sinceDate != null && untilDate != null) {
            LocalDateTime since = LocalDateTime.parse(sinceDate + "T00:00:00");
            LocalDateTime until = LocalDateTime.parse(untilDate + "T23:59:59");
            return collector.collectPullRequests(owner, repo, since, until);
        } else if (base != null && head != null) {
            return collector.collectPullRequestsByRange(owner, repo, base, head);
        } else {
            throw new IllegalArgumentException("Must specify either --since-tag/--until-tag, --since/--until, or --base/--head");
        }
    }
    
    private List<Commit> collectCommits(ScmCollector collector) {
        if (sinceTag != null && untilTag != null) {
            return collector.collectCommitsByTags(owner, repo, sinceTag, untilTag);
        } else if (sinceDate != null && untilDate != null) {
            LocalDateTime since = LocalDateTime.parse(sinceDate + "T00:00:00");
            LocalDateTime until = LocalDateTime.parse(untilDate + "T23:59:59");
            return collector.collectCommits(owner, repo, since, until);
        } else if (base != null && head != null) {
            return collector.collectCommitsByRange(owner, repo, base, head);
        } else {
            throw new IllegalArgumentException("Must specify either --since-tag/--until-tag, --since/--until, or --base/--head");
        }
    }
    
    private String buildRangeDescription() {
        if (sinceTag != null && untilTag != null) {
            return sinceTag + ".." + untilTag;
        } else if (sinceDate != null && untilDate != null) {
            return sinceDate + " to " + untilDate;
        } else if (base != null && head != null) {
            return base + ".." + head;
        } else {
            return "unknown range";
        }
    }
    
    private void publishReleaseNotes(ReleaseNotes releaseNotes, ReleaseScribeConfig config) throws IOException {
        String version = untilTag != null ? untilTag : "unknown";
        String tag = untilTag != null ? untilTag : "unknown";
        
        List<Publisher> publishers = new ArrayList<>();
        
        if (publishGitHubRelease) {
            String githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken != null) {
                publishers.add(new GitHubReleasePublisher(githubToken, owner, repo));
            } else {
                System.err.println("Warning: GITHUB_TOKEN not set, skipping GitHub Release publishing");
            }
        }
        
        if (publishConfluence) {
            String confluenceUrl = System.getenv("CONFLUENCE_URL");
            String confluenceUsername = System.getenv("CONFLUENCE_USERNAME");
            String confluenceToken = System.getenv("CONFLUENCE_TOKEN");
            String confluenceSpace = System.getenv("CONFLUENCE_SPACE");
            String confluenceParentPage = System.getenv("CONFLUENCE_PARENT_PAGE");
            
            if (confluenceUrl != null && confluenceUsername != null && confluenceToken != null && 
                confluenceSpace != null && confluenceParentPage != null) {
                publishers.add(new ConfluencePublisher(confluenceUrl, confluenceUsername, confluenceToken, 
                        confluenceSpace, confluenceParentPage));
            } else {
                System.err.println("Warning: Confluence environment variables not set, skipping Confluence publishing");
            }
        }
        
        if (publishSlack) {
            String slackWebhook = System.getenv("SLACK_WEBHOOK_URL");
            String slackChannel = System.getenv("SLACK_CHANNEL");
            String slackUsername = System.getenv("SLACK_USERNAME");
            
            if (slackWebhook != null) {
                publishers.add(new SlackPublisher(slackWebhook, slackChannel, slackUsername));
            } else {
                System.err.println("Warning: SLACK_WEBHOOK_URL not set, skipping Slack publishing");
            }
        }
        
        for (Publisher publisher : publishers) {
            if (publisher.isConfigured()) {
                System.out.println("Publishing to " + publisher.getName() + "...");
                publisher.publish(releaseNotes, version, tag);
            } else {
                System.err.println("Warning: " + publisher.getName() + " publisher not configured");
            }
        }
    }
}

