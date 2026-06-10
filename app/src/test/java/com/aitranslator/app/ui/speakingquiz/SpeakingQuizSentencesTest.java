package com.aitranslator.app.ui.speakingquiz;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link SpeakingQuizSentences}.
 *
 * Validates the bundled sentence corpus — sizes, content, language
 * dispatch and defensive null handling. Runs on the JVM (no Android
 * runtime needed).
 */
public class SpeakingQuizSentencesTest {

    // ─────────────────────────────────────────────────────────────────────────
    //  English corpus
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void beginnerCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.BEGINNER.length);
    }

    @Test
    public void intermediateCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.INTERMEDIATE.length);
    }

    @Test
    public void advancedCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.ADVANCED.length);
    }

    @Test
    public void noSentenceInEnglishCorpusIsNullOrEmpty() {
        for (String s : SpeakingQuizSentences.BEGINNER) {
            assertNotNull("BEGINNER sentence must not be null", s);
            assertFalse("BEGINNER sentence must not be empty", s.trim().isEmpty());
        }
        for (String s : SpeakingQuizSentences.INTERMEDIATE) {
            assertNotNull("INTERMEDIATE sentence must not be null", s);
            assertFalse("INTERMEDIATE sentence must not be empty", s.trim().isEmpty());
        }
        for (String s : SpeakingQuizSentences.ADVANCED) {
            assertNotNull("ADVANCED sentence must not be null", s);
            assertFalse("ADVANCED sentence must not be empty", s.trim().isEmpty());
        }
    }

    @Test
    public void beginnerSentencesAreShorterThanAdvanced() {
        // Average length proxy — beginners should be shorter on average
        double avgBeginner = averageLength(SpeakingQuizSentences.BEGINNER);
        double avgAdvanced = averageLength(SpeakingQuizSentences.ADVANCED);
        assertTrue(
            "Beginner avg length (" + avgBeginner + ") should be < Advanced (" + avgAdvanced + ")",
            avgBeginner < avgAdvanced
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hindi corpus
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void hindiBeginnerCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.BEGINNER_HI.length);
    }

    @Test
    public void hindiIntermediateCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.INTERMEDIATE_HI.length);
    }

    @Test
    public void hindiAdvancedCorpusHas50Sentences() {
        assertEquals(50, SpeakingQuizSentences.ADVANCED_HI.length);
    }

    @Test
    public void noSentenceInHindiCorpusIsNullOrEmpty() {
        for (String s : SpeakingQuizSentences.BEGINNER_HI) {
            assertNotNull("BEGINNER_HI sentence must not be null", s);
            assertFalse("BEGINNER_HI sentence must not be empty", s.trim().isEmpty());
        }
    }

    @Test
    public void hindiCorpusContainsDevanagari() {
        // Sanity check: at least one sentence must contain a Devanagari character
        boolean hasDevanagari = false;
        for (String s : SpeakingQuizSentences.BEGINNER_HI) {
            for (char c : s.toCharArray()) {
                // Devanagari Unicode block: U+0900..U+097F
                if (c >= 0x0900 && c <= 0x097F) { hasDevanagari = true; break; }
            }
            if (hasDevanagari) break;
        }
        assertTrue("Hindi corpus must contain Devanagari characters", hasDevanagari);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  forLevel() dispatch
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void forLevel_English_Beginner_ReturnsEnglishArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.BEGINNER, "en");
        assertSame(SpeakingQuizSentences.BEGINNER, result);
    }

    @Test
    public void forLevel_English_Intermediate_ReturnsEnglishArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.INTERMEDIATE, "en");
        assertSame(SpeakingQuizSentences.INTERMEDIATE, result);
    }

    @Test
    public void forLevel_English_Advanced_ReturnsEnglishArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.ADVANCED, "en");
        assertSame(SpeakingQuizSentences.ADVANCED, result);
    }

    @Test
    public void forLevel_Hindi_Beginner_ReturnsHindiArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.BEGINNER, "hi");
        assertSame(SpeakingQuizSentences.BEGINNER_HI, result);
    }

    @Test
    public void forLevel_Hindi_Intermediate_ReturnsHindiArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.INTERMEDIATE, "hi");
        assertSame(SpeakingQuizSentences.INTERMEDIATE_HI, result);
    }

    @Test
    public void forLevel_Hindi_Advanced_ReturnsHindiArray() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.ADVANCED, "hi");
        assertSame(SpeakingQuizSentences.ADVANCED_HI, result);
    }

    @Test
    public void forLevel_CaseInsensitiveHindi() {
        // "HI" and "Hi" should both dispatch to Hindi
        String[] upper = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.BEGINNER, "HI");
        assertSame(SpeakingQuizSentences.BEGINNER_HI, upper);
    }

    @Test
    public void forLevel_UnknownLanguage_FallsBackToEnglish() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.BEGINNER, "xx");
        assertSame("Unknown language code should fall back to English BEGINNER",
                SpeakingQuizSentences.BEGINNER, result);
    }

    @Test
    public void forLevel_NullLanguage_FallsBackToEnglish() {
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.BEGINNER, null);
        assertSame("Null language code should fall back to English BEGINNER",
                SpeakingQuizSentences.BEGINNER, result);
    }

    @Test
    public void forLevel_NullLevel_DefaultsToBeginner() {
        String[] result = SpeakingQuizSentences.forLevel(null, "en");
        assertSame("Null level should default to BEGINNER",
                SpeakingQuizSentences.BEGINNER, result);
    }

    @Test
    public void forLevel_LegacyOverload_ReturnsEnglish() {
        // The single-argument back-compat overload should always return English
        String[] result = SpeakingQuizSentences.forLevel(
                SpeakingQuizSentences.Level.ADVANCED);
        assertSame(SpeakingQuizSentences.ADVANCED, result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Uniqueness (no duplicate sentences inside a level)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void englishBeginnerSentencesAreUnique() {
        assertNoDuplicates("BEGINNER", SpeakingQuizSentences.BEGINNER);
    }

    @Test
    public void englishIntermediateSentencesAreUnique() {
        assertNoDuplicates("INTERMEDIATE", SpeakingQuizSentences.INTERMEDIATE);
    }

    @Test
    public void englishAdvancedSentencesAreUnique() {
        assertNoDuplicates("ADVANCED", SpeakingQuizSentences.ADVANCED);
    }

    @Test
    public void hindiBeginnerSentencesAreUnique() {
        assertNoDuplicates("BEGINNER_HI", SpeakingQuizSentences.BEGINNER_HI);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private double averageLength(String[] corpus) {
        int total = 0;
        for (String s : corpus) total += s.length();
        return (double) total / corpus.length;
    }

    private void assertNoDuplicates(String corpusName, String[] corpus) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String s : corpus) {
            if (!seen.add(s)) {
                fail(corpusName + " contains duplicate sentence: \"" + s + "\"");
            }
        }
    }
}
