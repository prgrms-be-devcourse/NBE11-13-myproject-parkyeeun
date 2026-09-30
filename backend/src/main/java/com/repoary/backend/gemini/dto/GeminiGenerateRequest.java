package com.repoary.backend.gemini.dto;

import java.util.List;

public record GeminiGenerateRequest(
        Content systemInstruction,
        List<Content> contents
) {

    public static GeminiGenerateRequest of(
            String systemInstruction,
            String prompt
    ) {
        return new GeminiGenerateRequest(
                Content.of(systemInstruction),
                List.of(Content.of(prompt))
        );
    }

    public record Content(List<Part> parts) {
        public static Content of(String text) {
            return new Content(List.of(new Part(text)));
        }
    }

    public record Part(String text) {
    }
}
