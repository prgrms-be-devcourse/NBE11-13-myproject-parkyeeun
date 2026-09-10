package com.repoary.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContentResponse(
        String name,
        String path,
        String type,
        String content,
        String encoding
) {
    public boolean isDirectory() {
        return "dir".equals(type);
    }

    public boolean isFile() {
        return "file".equals(type);
    }
}