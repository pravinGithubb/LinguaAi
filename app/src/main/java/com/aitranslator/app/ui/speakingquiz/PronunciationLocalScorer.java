package com.aitranslator.app.ui.speakingquiz;

/**
 * Local string-similarity scorer for the Speaking Quiz.
 *
 * Compares the device STT result against the expected sentence and returns a
 * 0-100 similarity score. Runs entirely on-device — no Gemini call, no
 * network, instant feedback (Levenshtein on strings under 200 chars is
 * effectively free).
 *
 * Design notes:
 *   - We normalise both strings (lowercase, strip punctuation, collapse
 *     whitespace) before comparison so that "Hello, world!" and "hello world"
 *     score 100, not 90.
 *   - We use *character-level* edit distance rather than word-level. Word
 *     distance is unforgiving for homophone substitutions and STT word-split
 *     quirks ("ice cream" vs "icecream"). Character distance degrades
 *     gracefully and rewards being close-but-not-perfect.
 *   - For the very short Beginner phrases this still works well because the
 *     character distance scales naturally with the reference length.
 */
public final class PronunciationLocalScorer {

    private PronunciationLocalScorer() { /* no instances */ }

    /** Per-attempt result returned to the UI layer. */
    public static final class Score {
        public final int percent;          // 0..100
        public final String grade;         // "Excellent" / "Good" / "Fair" / "Try Again"
        public final int colorArgb;        // matches the grade — fed straight into the score circle
        public final String normalisedSpoken;
        public final String normalisedExpected;

        Score(int percent, String grade, int colorArgb,
              String normalisedSpoken, String normalisedExpected) {
            this.percent = percent;
            this.grade = grade;
            this.colorArgb = colorArgb;
            this.normalisedSpoken = normalisedSpoken;
            this.normalisedExpected = normalisedExpected;
        }
    }

    /** Public entry point — score a recognised utterance against an expected sentence. */
    public static Score score(String expected, String spoken) {
        String e = normalise(expected);
        String s = normalise(spoken);

        int percent;
        if (e.isEmpty()) {
            // Defensive: nothing to compare against, treat as zero.
            percent = 0;
        } else if (s.isEmpty()) {
            percent = 0;
        } else {
            int distance = levenshtein(e, s);
            int maxLen   = Math.max(e.length(), s.length());
            // similarity = 1 - distance/maxLen, clamped to [0,1], then *100
            double sim = 1.0 - ((double) distance / (double) maxLen);
            if (sim < 0) sim = 0;
            percent = (int) Math.round(sim * 100.0);
        }

        return new Score(percent, gradeFor(percent), colourFor(percent), s, e);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Lowercase, strip non-letter/digit/space, collapse whitespace, trim. */
    private static String normalise(String in) {
        if (in == null) return "";
        String lower = in.toLowerCase();
        StringBuilder sb = new StringBuilder(lower.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                lastWasSpace = false;
            } else if (Character.isWhitespace(c) || c == '-' || c == '\'') {
                // treat hyphens and apostrophes as soft separators
                if (!lastWasSpace && sb.length() > 0) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            }
            // everything else (punctuation, symbols) silently dropped
        }
        // Trim trailing space if any
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') end--;
        return sb.substring(0, end);
    }

    /**
     * Standard iterative Levenshtein with two rolling rows.
     * O(n*m) time, O(min(n,m)) space. Plenty fast for sub-300-char sentences.
     */
    private static int levenshtein(String a, String b) {
        // Make `a` the shorter string to minimise the row buffer.
        if (a.length() > b.length()) { String t = a; a = b; b = t; }
        int n = a.length();
        int m = b.length();

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int i = 0; i <= n; i++) prev[i] = i;

        for (int j = 1; j <= m; j++) {
            curr[0] = j;
            for (int i = 1; i <= n; i++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[i] = Math.min(
                        Math.min(curr[i - 1] + 1,  // insert
                                 prev[i]     + 1), // delete
                        prev[i - 1] + cost         // substitute
                );
            }
            // swap rows
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[n];
    }

    private static String gradeFor(int p) {
        if (p >= 90) return "Excellent";
        if (p >= 75) return "Good";
        if (p >= 60) return "Fair";
        return "Try Again";
    }

    /** Match the existing PronunciationScorerFragment colour palette. */
    private static int colourFor(int p) {
        if (p >= 90) return 0xFF2ECC71;  // emerald
        if (p >= 75) return 0xFF3D52A0;  // primary
        if (p >= 60) return 0xFFFFB347;  // gold
        return 0xFFFF6B6B;               // accent (try again)
    }
}
