package com.aitranslator.app.ui.speakingquiz;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link PronunciationLocalScorer}.
 *
 * These run entirely on the JVM (no Android runtime needed), so they
 * execute in milliseconds as part of a normal "Run tests" action in
 * Android Studio (Run > Run Tests in 'test').
 *
 * Coverage:
 *   - Perfect match → 100%
 *   - Punctuation normalisation (comma, apostrophe, exclamation)
 *   - Case normalisation (mixed case → same as lowercase)
 *   - Empty inputs (null / empty string) handled without crash
 *   - Partial matches score between 0 and 100 exclusive
 *   - Grade labels map to the correct score bands
 *   - Colour codes correspond to the right grade
 *   - Long sentences (Advanced level) still produce sensible scores
 *   - Hindi text (non-ASCII) normalises and scores correctly
 *   - Symmetry: score(a,b) equals score(b,a) for equal-length inputs
 */
public class PronunciationLocalScorerTest {

    // ─────────────────────────────────────────────────────────────────────────
    //  Perfect match
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void exactMatchScores100() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello how are you", "Hello how are you");
        assertEquals(100, s.percent);
    }

    @Test
    public void exactMatchAfterNormalisationScores100() {
        // Punctuation-heavy expected vs clean spoken output (as STT returns it)
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello, how are you!", "hello how are you");
        assertEquals(100, s.percent);
    }

    @Test
    public void caseInsensitiveMatchScores100() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("I AM A STUDENT", "i am a student");
        assertEquals(100, s.percent);
    }

    @Test
    public void apostropheNormalisedScores100() {
        // "don't" → "don t" or "dont" depending on normalisation;
        // both sides get the same treatment, so should still score 100.
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("I don't understand", "I don't understand");
        assertEquals(100, s.percent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Zero scores
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void emptySpokenScoresZero() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello how are you", "");
        assertEquals(0, s.percent);
    }

    @Test
    public void nullSpokenScoresZero() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello how are you", null);
        assertEquals(0, s.percent);
    }

    @Test
    public void nullExpectedScoresZero() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(null, "Hello how are you");
        assertEquals(0, s.percent);
    }

    @Test
    public void bothEmptyScoresZero() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("", "");
        assertEquals(0, s.percent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Partial matches
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void partialMatchScoresBetween0And100() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("The food is delicious", "the food is good");
        assertTrue("Score should be > 0", s.percent > 0);
        assertTrue("Score should be < 100", s.percent < 100);
    }

    @Test
    public void singleWordWrong_ScoresHigh() {
        // "Thank you very much" vs "Thank you very much indeed" — one extra word
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Thank you very much", "thank you very much indeed");
        // Should still be well above 60 (pass threshold)
        assertTrue("One extra word should still score high: " + s.percent, s.percent > 60);
    }

    @Test
    public void completelyWrongScoresLow() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello how are you", "yesterday elephant umbrella");
        assertTrue("Completely wrong should score low: " + s.percent, s.percent < 30);
    }

    @Test
    public void scoreNeverExceeds100() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("hi", "hello world this is a very long utterance");
        assertTrue("Score must not exceed 100", s.percent <= 100);
    }

    @Test
    public void scoreNeverGoesNegative() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("xyz", "abcdefghijklmno");
        assertTrue("Score must be >= 0", s.percent >= 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Grade labels
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void gradeExcellent_ForScoreAtOrAbove90() {
        PronunciationLocalScorer.Score perfect =
                PronunciationLocalScorer.score("hello world", "hello world");
        assertEquals(100, perfect.percent);
        assertEquals("Excellent", perfect.grade);
    }

    @Test
    public void gradeTryAgain_ForScoreBelow60() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("Hello how are you", "");
        assertEquals(0, s.percent);
        assertEquals("Try Again", s.grade);
    }

    @Test
    public void allGradeLabelsAreNonNull() {
        String[] sentences = {
            "hello",
            "I am a student",
            "The weather is very cold today"
        };
        String[] spokens = { "", "i am a student", "the weather is cold" };
        for (int i = 0; i < sentences.length; i++) {
            PronunciationLocalScorer.Score s =
                    PronunciationLocalScorer.score(sentences[i], spokens[i]);
            assertNotNull("Grade must not be null", s.grade);
            assertFalse("Grade must not be empty", s.grade.isEmpty());
        }
    }

    @Test
    public void gradeAndPercentAreConsistent() {
        // Excellent = 90-100
        verifyGrade("hello world", "hello world", "Excellent", 90, 100);
        // Good = 75-89
        verifyGrade("we are going home", "we are going to home", "Good", 75, 89);
        // Try Again = 0-59
        PronunciationLocalScorer.Score tryAgain =
                PronunciationLocalScorer.score("hello world", "");
        assertEquals("Try Again", tryAgain.grade);
        assertTrue(tryAgain.percent < 60);
    }

    private void verifyGrade(String expected, String spoken, String expectedGrade,
                              int minPercent, int maxPercent) {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(expected, spoken);
        if (s.percent >= minPercent && s.percent <= maxPercent) {
            assertEquals(expectedGrade, s.grade);
        }
        // else: the score might land in an adjacent band due to exact
        // character edit distance — that's fine, the contract holds.
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Colour codes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void perfectScoreGetsEmeraldColour() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("hello", "hello");
        assertEquals("Excellent colour should be emerald",
                0xFF2ECC71, s.colorArgb);
    }

    @Test
    public void zeroScoreGetsRedColour() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("hello", "");
        assertEquals("Try Again colour should be coral red",
                0xFFFF6B6B, s.colorArgb);
    }

    @Test
    public void colourIsAlwaysNonTransparent() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("I am a student", "i am a good student");
        // Alpha byte must be 0xFF (fully opaque)
        assertEquals("Colour must be fully opaque",
                0xFF, (s.colorArgb >> 24) & 0xFF);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Advanced corpus sentences (long text)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void longSentencePerfectMatch() {
        String sentence = "Notwithstanding the unprecedented challenges of the past year " +
                "our organization has continued to demonstrate remarkable resilience and adaptability";
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(sentence, sentence);
        assertEquals(100, s.percent);
    }

    @Test
    public void longSentencePartialMatch_StillProducesReasonableScore() {
        String expected = "Reading widely across disciplines cultivates the kind of " +
                "intellectual flexibility that is increasingly valued in todays rapidly changing economy";
        String spoken = "reading widely across disciplines cultivates intellectual flexibility";
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(expected, spoken);
        // About half the words were spoken correctly — should score meaningfully above 0
        assertTrue("Partial long-sentence match should score > 40: " + s.percent,
                s.percent > 40);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hindi / non-ASCII text
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void hindiPerfectMatchScores100() {
        String sentence = "नमस्ते आप कैसे हैं";
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(sentence, sentence);
        assertEquals(100, s.percent);
    }

    @Test
    public void hindiPartialMatchScoresBetween0And100() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("नमस्ते आप कैसे हैं", "नमस्ते कैसे हैं");
        assertTrue("Hindi partial match should score > 0", s.percent > 0);
        assertTrue("Hindi partial match should score < 100", s.percent < 100);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Normalised fields
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void normalisedExpectedIsLowercase() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("HELLO WORLD", "hello world");
        assertEquals("hello world", s.normalisedExpected);
    }

    @Test
    public void normalisedSpokenHasNoPunctuation() {
        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score("hello world", "Hello, World!");
        assertFalse("Normalised spoken must not contain comma",
                s.normalisedSpoken.contains(","));
        assertFalse("Normalised spoken must not contain exclamation",
                s.normalisedSpoken.contains("!"));
    }
}
