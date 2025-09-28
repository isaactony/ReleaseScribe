package com.releasescribe.renderer;

import com.releasescribe.model.ReleaseNotes;

import java.io.IOException;
import java.nio.file.Path;

public interface Renderer {
    
    /**
     * Render release notes to files
     */
    void render(ReleaseNotes releaseNotes, Path outputDir) throws IOException;
    
    /**
     * Render changelog to file
     */
    void renderChangelog(String changelogMd, Path outputFile) throws IOException;
    
    /**
     * Render release notes to file
     */
    void renderReleaseNotes(String releaseNotesMd, Path outputFile) throws IOException;
    
    /**
     * Render customer highlights to file
     */
    void renderCustomerHighlights(String customerHighlightsMd, Path outputFile) throws IOException;
    
    /**
     * Render GitHub release body to file
     */
    void renderGitHubReleaseBody(ReleaseNotes releaseNotes, Path outputFile) throws IOException;
}
