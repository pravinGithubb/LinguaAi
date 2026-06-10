package com.aitranslator.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.aitranslator.app.utils.PrefsManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented tests for {@link PrefsManager}.
 *
 * Uses a real SharedPreferences store backed by the test app's context.
 * Each test clears the prefs first so there's no leftover state from
 * a previous run.
 *
 * How to run: right-click → Run 'PrefsManagerTest'
 * (Requires a connected device or running emulator.)
 */
@RunWith(AndroidJUnit4.class)
public class PrefsManagerTest {

    private PrefsManager prefs;

    @Before
    public void setUp() {
        Context ctx = ApplicationProvider.getApplicationContext();
        // Clear all prefs before each test to prevent bleed-through.
        ctx.getSharedPreferences("lingua_ai_prefs", Context.MODE_PRIVATE)
           .edit().clear().commit();
        // Force PrefsManager singleton to refresh with the clean prefs.
        // Reflection trick: reset the static instance so getInstance
        // returns a fresh object pointing at the cleared prefs.
        try {
            java.lang.reflect.Field f = PrefsManager.class.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (Exception e) {
            // If we can't reset the singleton, the tests will still work
            // on a fresh device/emulator where no prefs file exists yet.
        }
        prefs = PrefsManager.getInstance(ctx);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Language prefs
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void nativeLanguage_DefaultIsEnglish() {
        assertEquals("English", prefs.getNativeLanguage());
        assertEquals("en", prefs.getNativeLanguageCode());
    }

    @Test
    public void targetLanguage_DefaultIsSpanish() {
        assertEquals("Spanish", prefs.getTargetLanguage());
        assertEquals("es", prefs.getTargetLanguageCode());
    }

    @Test
    public void setNativeLanguage_CanBeReadBack() {
        prefs.setNativeLanguage("French", "fr");
        assertEquals("French", prefs.getNativeLanguage());
        assertEquals("fr",     prefs.getNativeLanguageCode());
    }

    @Test
    public void setTargetLanguage_CanBeReadBack() {
        prefs.setTargetLanguage("Hindi", "hi");
        assertEquals("Hindi", prefs.getTargetLanguage());
        assertEquals("hi",    prefs.getTargetLanguageCode());
    }

    @Test
    public void setTargetLanguage_Hindi_CodeIsHi() {
        // Critical for Speaking Quiz Hindi corpus dispatch
        prefs.setTargetLanguage("Hindi", "hi");
        assertEquals("hi", prefs.getTargetLanguageCode());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Onboarding
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void onboarding_DefaultIsFalse() {
        assertFalse("Onboarding must not be done by default",
                prefs.isOnboardingDone());
    }

    @Test
    public void setOnboardingDone_CanBeReadBack() {
        prefs.setOnboardingDone();
        assertTrue("Onboarding should be flagged as done",
                prefs.isOnboardingDone());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Streak
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void streak_DefaultIsZero() {
        assertEquals(0, prefs.getStreak());
    }

    @Test
    public void incrementStreak_IncrementsBy1() {
        prefs.incrementStreak();
        assertEquals(1, prefs.getStreak());
        prefs.incrementStreak();
        assertEquals(2, prefs.getStreak());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  XP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void xp_DefaultIsZero() {
        assertEquals(0, prefs.getTotalXp());
    }

    @Test
    public void addXp_AccumulatesCorrectly() {
        prefs.addXp(50);
        assertEquals(50, prefs.getTotalXp());
        prefs.addXp(30);
        assertEquals(80, prefs.getTotalXp());
    }

    @Test
    public void addXp_Zero_DoesNotChange() {
        prefs.addXp(100);
        prefs.addXp(0);
        assertEquals(100, prefs.getTotalXp());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sessions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void sessions_DefaultIsZero() {
        assertEquals(0, prefs.getTotalSessions());
    }

    @Test
    public void incrementSessions_IncrementsBy1() {
        prefs.incrementSessions();
        assertEquals(1, prefs.getTotalSessions());
        prefs.incrementSessions();
        assertEquals(2, prefs.getTotalSessions());
    }
}
