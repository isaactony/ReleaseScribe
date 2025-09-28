package com.releasescribe.renderer;

import com.releasescribe.model.ReleaseNotes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MarkdownRenderer implements Renderer {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void render(ReleaseNotes releaseNotes, Path outputDir) throws IOException {
        // Ensure output directory exists
        Files.createDirectories(outputDir);
        
        // Render individual files
        renderChangelog(releaseNotes.getChangelogMd(), outputDir.resolve("CHANGELOG.md"));
        renderReleaseNotes(releaseNotes.getReleaseNotesMd(), outputDir.resolve("RELEASE_NOTES.md"));
        renderCustomerHighlights(releaseNotes.getCustomerHighlightsMd(), outputDir.resolve("HIGHLIGHTS.md"));
        renderGitHubReleaseBody(releaseNotes, outputDir.resolve("RELEASE_BODY.md"));
    }
    
    @Override
    public void renderChangelog(String changelogMd, Path outputFile) throws IOException {
        String content = buildChangelogContent(changelogMd);
        Files.writeString(outputFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    @Override
    public void renderReleaseNotes(String releaseNotesMd, Path outputFile) throws IOException {
        String content = buildReleaseNotesContent(releaseNotesMd);
        Files.writeString(outputFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    @Override
    public void renderCustomerHighlights(String customerHighlightsMd, Path outputFile) throws IOException {
        String content = buildCustomerHighlightsContent(customerHighlightsMd);
        Files.writeString(outputFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    @Override
    public void renderGitHubReleaseBody(ReleaseNotes releaseNotes, Path outputFile) throws IOException {
        String content = buildGitHubReleaseBody(releaseNotes);
        Files.writeString(outputFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private String buildChangelogContent(String changelogMd) {
        StringBuilder content = new StringBuilder();
        content.append("# Changelog\n\n");
        content.append("All notable changes to this project will be documented in this file.\n\n");
        content.append("Generated on: ").append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append("\n\n");
        content.append("---\n\n");
        content.append(changelogMd);
        return content.toString();
    }
    
    private String buildReleaseNotesContent(String releaseNotesMd) {
        StringBuilder content = new StringBuilder();
        content.append("# Release Notes\n\n");
        content.append("Generated on: ").append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append("\n\n");
        content.append("---\n\n");
        content.append(releaseNotesMd);
        return content.toString();
    }
    
    private String buildCustomerHighlightsContent(String customerHighlightsMd) {
        StringBuilder content = new StringBuilder();
        content.append("# Customer Highlights\n\n");
        content.append("Generated on: ").append(LocalDateTime.now().format(TIMESTAMP_FORMATTER)).append("\n\n");
        content.append("---\n\n");
        content.append(customerHighlightsMd);
        return content.toString();
    }
    
    private String buildGitHubReleaseBody(ReleaseNotes releaseNotes) {
        StringBuilder content = new StringBuilder();
        
        // Add customer highlights first (most important for GitHub releases)
        if (releaseNotes.getCustomerHighlightsMd() != null && !releaseNotes.getCustomerHighlightsMd().trim().isEmpty()) {
            content.append("## What's New\n\n");
            content.append(releaseNotes.getCustomerHighlightsMd()).append("\n\n");
        }
        
        // Add breaking changes if any
        if (releaseNotes.getBreakingChanges() != null && !releaseNotes.getBreakingChanges().isEmpty()) {
            content.append("## Breaking Changes\n\n");
            for (String breakingChange : releaseNotes.getBreakingChanges()) {
                content.append("- ").append(breakingChange).append("\n");
            }
            content.append("\n");
        }
        
        // Add upgrade steps if any
        if (releaseNotes.getUpgradeSteps() != null && !releaseNotes.getUpgradeSteps().isEmpty()) {
            content.append("## Upgrade Steps\n\n");
            for (String upgradeStep : releaseNotes.getUpgradeSteps()) {
                content.append("- ").append(upgradeStep).append("\n");
            }
            content.append("\n");
        }
        
        // Add full changelog
        if (releaseNotes.getChangelogMd() != null && !releaseNotes.getChangelogMd().trim().isEmpty()) {
            content.append("## Full Changelog\n\n");
            content.append(releaseNotes.getChangelogMd());
        }
        
        return content.toString();
    }
}
