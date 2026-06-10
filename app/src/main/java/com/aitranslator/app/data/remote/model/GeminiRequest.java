package com.aitranslator.app.data.remote.model;
import java.util.List;

public class GeminiRequest {
    public List<Content> contents;
    public GenerationConfig generationConfig;

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
        this.generationConfig = new GenerationConfig();
    }

    public static class Content {
        public String role;
        public List<Part> parts;
        public Content(String role, String text) {
            this.role = role;
            this.parts = List.of(new Part(text));
        }
    }

    public static class Part {
        public String text;
        public Part(String text) { this.text = text; }
    }

    public static class GenerationConfig {
        public int maxOutputTokens = 1024;
        public float temperature = 0.75f;
        public float topP = 0.9f;
    }
}