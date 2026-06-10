package com.aitranslator.app.data.remote.model;
import java.util.List;

public class GeminiResponse {
    public List<Candidate> candidates;

    public String getTextContent() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate c = candidates.get(0);
            if (c.content != null && c.content.parts != null && !c.content.parts.isEmpty()) {
                return c.content.parts.get(0).text;
            }
        }
        return "";
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
    }

    public static class Content {
        public List<Part> parts;
        public String role;
    }

    public static class Part {
        public String text;
    }
}