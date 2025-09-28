package com.releasescribe.publisher;

import com.releasescribe.model.ReleaseNotes;

import java.io.IOException;
import java.nio.file.Path;

public interface Publisher {
    
    /**
     * Publish release notes to the target platform
     */
    void publish(ReleaseNotes releaseNotes, String version, String tag) throws IOException;
    
    /**
     * Check if publisher is configured and ready
     */
    boolean isConfigured();
    
    /**
     * Get publisher name
     */
    String getName();
}
