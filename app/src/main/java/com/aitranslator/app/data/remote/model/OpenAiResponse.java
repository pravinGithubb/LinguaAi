package com.aitranslator.app.data.remote.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Chat Completions response body.
 *
 * Schema: https://platform.openai.com/docs/api-reference/chat/object
 *
 * Provides {@link #toGemini()} so the {@link com.aitranslator.app.data.remote.LlmGateway}
 * can deliver fallback responses in the same shape every consumer in
 * AppRepository already expects (a {@link GeminiResponse}). This way
 * the rest of the codebase stays oblivious to which provider answered.
 */
public class OpenAiResponse {

    public List<Choice> choices;

    /** Convenience: pull the first message's text, or empty string. */
    public String getTextContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice c = choices.get(0);
            if (c.message != null && c.message.content != null) {
                return c.message.content;
            }
        }
        return "";
    }

    /**
     * Wraps the OpenAI text output in a {@link GeminiResponse} so existing
     * consumers (which all use {@link GeminiResponse#getTextContent()})
     * see no difference between Gemini and OpenAI replies.
     */
    public GeminiResponse toGemini() {
        GeminiResponse g = new GeminiResponse();
        g.candidates = new ArrayList<>();

        GeminiResponse.Candidate cand = new GeminiResponse.Candidate();
        cand.content = new GeminiResponse.Content();
        cand.content.role = "model";
        cand.content.parts = new ArrayList<>();

        GeminiResponse.Part part = new GeminiResponse.Part();
        part.text = getTextContent();
        cand.content.parts.add(part);

        // Approximate Gemini's finishReason naming for any downstream
        // code that inspects it. OpenAI uses "stop" / "length" / "content_filter";
        // Gemini uses "STOP" / "MAX_TOKENS" / "SAFETY".
        if (choices != null && !choices.isEmpty()) {
            String finish = choices.get(0).finish_reason;
            if ("length".equalsIgnoreCase(finish))           cand.finishReason = "MAX_TOKENS";
            else if ("content_filter".equalsIgnoreCase(finish)) cand.finishReason = "SAFETY";
            else                                             cand.finishReason = "STOP";
        }

        g.candidates.add(cand);
        return g;
    }

    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason;
    }

    public static class Message {
        public String role;
        public String content;
    }
}
