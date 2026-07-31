package com.repoary.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommitDetailResponse(
        String sha,

        @JsonAlias("html_url")
        String htmlUrl,

        List<ChangedFile> files
) {

    public GitHubCommitDetailResponse {
        files = files == null ? List.of() : List.copyOf(files);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChangedFile(
            String filename,
            String status,
            int additions,
            int deletions,
            int changes,

            @JsonAlias("previous_filename")
            String previousFilename
    ) {
    }
}