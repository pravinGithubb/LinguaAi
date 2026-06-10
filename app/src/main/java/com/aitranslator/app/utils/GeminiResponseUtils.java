package com.aitranslator.app.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust extraction of structured data from free-form Gemini responses.
 *
 * Gemini routinely wraps JSON in markdown fences, prefixes responses with
 * "Sure, here is …" sentences, uses smart quotes from auto-correct, and decorates
 * section headers with bold (**HEADER**) or hashes (## HEADER). These helpers
 * tolerate all of that.
 */
public final class GeminiResponseUtils {

    private GeminiResponseUtils() {}

    // ────────────────────────────────────────────────────────────────────────
    //  JSON EXTRACTION
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Pulls the first complete JSON value (object or array) out of any text.
     * Strips markdown fences, normalises smart quotes, and scans for balanced
     * brackets so trailing prose after the JSON is ignored.
     *
     * @return clean JSON string ready for {@link JsonParser#parseString}
     * @throws IllegalArgumentException if no JSON value is found
     */
    public static String extractJson(String raw) {
        if (raw == null) throw new IllegalArgumentException("Empty response");
        String s = stripFences(raw);
        s = normaliseQuotes(s);

        // Find the first '[' or '{' — whichever comes first
        int objIdx = s.indexOf('{');
        int arrIdx = s.indexOf('[');
        int start;
        char open, close;
        if (arrIdx >= 0 && (objIdx < 0 || arrIdx < objIdx)) {
            start = arrIdx; open = '['; close = ']';
        } else if (objIdx >= 0) {
            start = objIdx; open = '{'; close = '}';
        } else {
            throw new IllegalArgumentException("No JSON object or array found in response");
        }

        // Walk the string keeping a depth counter; respect string-literal escapes
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return s.substring(start, i + 1);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON value in response");
    }

    /** Convenience: extract JSON and parse it. */
    public static JsonElement parseJson(String raw) {
        return JsonParser.parseString(extractJson(raw));
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SECTION EXTRACTION (for HEADER: ... HEADER: ... formatted prompts)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Extract the body following a named header. Stops at the first occurrence
     * of any other header in {@code allHeaders} that comes after this one in
     * the text (so caller doesn't need to know section order). Header matching
     * is case-insensitive and tolerates markdown bold ({@code **TITLE:**}),
     * markdown headings ({@code ## TITLE}) and trailing punctuation.
     *
     * @param header     the section to extract (without the colon, e.g. "STORY")
     * @param allHeaders every possible header in the response, in any order
     * @return trimmed section body, or empty string if header not found
     */
    public static String extractSection(String raw, String header, String... allHeaders) {
        if (raw == null || header == null) return "";
        Matcher m = headerPattern(header).matcher(raw);
        if (!m.find()) return "";
        int contentStart = m.end();

        // Find the nearest *next* header in the response
        int contentEnd = raw.length();
        for (String other : allHeaders) {
            if (other == null || other.equalsIgnoreCase(header)) continue;
            Matcher om = headerPattern(other).matcher(raw);
            if (om.find(contentStart) && om.start() < contentEnd) {
                contentEnd = om.start();
            }
        }
        return cleanSection(raw.substring(contentStart, contentEnd));
    }

    /**
     * Build a regex matching a header like "TITLE", with optional markdown
     * bold/heading wrappers and a trailing colon or em dash.
     *
     * Matches: TITLE:  **TITLE:**  ## TITLE  Title — etc.
     */
    private static Pattern headerPattern(String header) {
        // Quote header for safe insertion into regex
        String h = Pattern.quote(header);
        // Leading: line start or whitespace; optional ##/** wrappers
        // Trailing: optional ** or *, then colon or em-dash, then optional whitespace
        return Pattern.compile(
            "(?i)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:\\*\\*)?" + h + "(?:\\*\\*)?\\s*[:\\-—]\\s*",
            Pattern.MULTILINE
        );
    }

    private static String cleanSection(String body) {
        if (body == null) return "";
        return body
            // trim outer brackets sometimes added by the LLM
            .replaceAll("^\\s*\\[", "")
            .replaceAll("\\]\\s*$", "")
            .trim();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────────────────

    private static String stripFences(String s) {
        // ```json ... ```  /  ``` ... ```
        return s
            .replaceAll("(?is)```\\s*json\\s*", "")
            .replaceAll("```", "")
            .trim();
    }

    /** Replace fancy / curly quotes with plain ASCII quotes so JSON parses. */
    private static String normaliseQuotes(String s) {
        return s
            .replace('\u201C', '"').replace('\u201D', '"')
            .replace('\u2018', '\'').replace('\u2019', '\'');
    }

    /** Pull the first integer 0-100 out of a string, defaulting if none found. */
    public static int extractScore(String text, int defaultScore) {
        if (text == null) return defaultScore;
        Matcher m = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 0 && n <= 100) return n;
        }
        return defaultScore;
    }
}
