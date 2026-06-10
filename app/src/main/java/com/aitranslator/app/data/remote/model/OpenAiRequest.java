package com.aitranslator.app.data.remote.model;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Chat Completions request body.
 *
 * Schema: https://platform.openai.com/docs/api-reference/chat/create
 *
 * Designed to be constructable from a {@link GeminiRequest} so the
 * {@link com.aitranslator.app.data.remote.LlmGateway} can transparently
 * convert one into the other when falling back. See
 * {@link #fromGemini(GeminiRequest)}.
 */
public class OpenAiRequest {

    public String model;
    public List<Message> messages;
    public Integer max_tokens;
    public Float temperature;
    public Float top_p;

    /** OpenAI's cheapest mainstream model — $0.10 in / $0.40 out per 1M tokens. */
    public static String defaultModel() { return "gpt-4.1-nano"; }

    public OpenAiRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    /**
     * Translates a Gemini request into an OpenAI request so callers don't
     * have to maintain two prompt-building paths. Generation parameters
     * (max tokens, temperature, top_p) are copied across so output length
     * and creativity stay consistent across providers.
     */
    public static OpenAiRequest fromGemini(GeminiRequest gemini) {
        List<Message> messages = new ArrayList<>();
        if (gemini != null && gemini.contents != null) {
            for (GeminiRequest.Content c : gemini.contents) {
                String role = mapRole(c.role);
                StringBuilder text = new StringBuilder();
                if (c.parts != null) {
                    for (GeminiRequest.Part p : c.parts) {
                        if (p.text != null) text.append(p.text);
                    }
                }
                messages.add(new Message(role, text.toString()));
            }
        }

        OpenAiRequest req = new OpenAiRequest(defaultModel(), messages);
        if (gemini != null && gemini.generationConfig != null) {
            req.max_tokens  = gemini.generationConfig.maxOutputTokens;
            req.temperature = gemini.generationConfig.temperature;
            req.top_p       = gemini.generationConfig.topP;
        }
        return req;
    }

    /** Gemini uses "user" / "model"; OpenAI uses "user" / "assistant". */
    private static String mapRole(String geminiRole) {
        if ("model".equalsIgnoreCase(geminiRole)) return "assistant";
        if (geminiRole == null || geminiRole.isEmpty()) return "user";
        return geminiRole;
    }

    public static class Message {
        public String role;     // "system", "user", or "assistant"
        public String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
