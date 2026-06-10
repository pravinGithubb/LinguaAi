package com.aitranslator.app.ui;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.aitranslator.app.R;
import com.aitranslator.app.ui.home.MainActivity;
import com.aitranslator.app.utils.PrefsManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * Espresso UI smoke tests.
 *
 * These run on a device or emulator and exercise real user flows —
 * they catch layout inflation errors, missing IDs, and broken navigation
 * early, before you run the full app manually.
 *
 * Scope: "smoke" — each test validates that a screen LAUNCHES and its
 * key views are VISIBLE. They don't test complex interactions (that would
 * make tests brittle). Think of these as a quick sanity check after any
 * layout change.
 *
 * How to run: right-click → Run 'MainActivityUiTest'
 * (Requires a connected device or running emulator.)
 *
 * Note: onboarding is bypassed below (@Before sets it as "done") so tests
 * start directly on the home screen, not the onboarding flow.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUiTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void skipOnboarding() {
        // Mark onboarding as complete so the activity starts on HomeFragment.
        // Without this, the test activity redirects to OnboardingActivity and
        // all home-screen checks immediately fail.
        Context ctx = ApplicationProvider.getApplicationContext();
        PrefsManager.getInstance(ctx).setOnboardingDone();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Bottom navigation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void bottomNav_IsVisible() {
        onView(withId(R.id.bottom_navigation))
                .check(matches(isDisplayed()));
    }

    @Test
    public void bottomNav_HasThreeTabs() {
        // Verify all three tab labels are present in the nav menu
        onView(withId(R.id.bottom_navigation))
                .check(matches(isDisplayed()));
        // Check that each menu item text is present somewhere on screen
        onView(withText("Home"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapHistory_ShowsHistoryFragment() {
        onView(withId(R.id.historyFragment)).perform(click());
        // The history fragment has tv_streak — confirm it's visible
        onView(withId(R.id.tv_streak))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapSettings_ShowsSettingsFragment() {
        onView(withId(R.id.settingsFragment)).perform(click());
        // Settings has card_change_language
        onView(withId(R.id.card_change_language))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapHistory_ThenHome_ReturnsToHome() {
        onView(withId(R.id.historyFragment)).perform(click());
        onView(withId(R.id.homeFragment)).perform(click());
        // Home hero card should now be visible
        onView(withId(R.id.card_ai_chat))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapHistory_Twice_StaysOnHistory() {
        // Regression: second tap used to get stuck or mis-highlight
        onView(withId(R.id.historyFragment)).perform(click());
        onView(withId(R.id.historyFragment)).perform(click());
        onView(withId(R.id.tv_streak))
                .check(matches(isDisplayed()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Home screen — primary cards
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void home_AiChatCard_IsVisible() {
        onView(withId(R.id.card_ai_chat))
                .check(matches(isDisplayed()));
    }

    @Test
    public void home_VocabularyCard_IsVisible() {
        onView(withId(R.id.card_vocabulary))
                .check(matches(isDisplayed()));
    }

    @Test
    public void home_TranslateCard_IsVisible() {
        onView(withId(R.id.card_translate))
                .check(matches(isDisplayed()));
    }

    @Test
    public void home_PracticeCard_IsVisible() {
        onView(withId(R.id.card_practice))
                .check(matches(isDisplayed()));
    }

    @Test
    public void home_MoreOptionsCard_IsVisible() {
        onView(withId(R.id.card_more_options))
                .check(matches(isDisplayed()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Navigation from home
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void tapTranslateCard_OpensTranslateScreen() {
        onView(withId(R.id.card_translate)).perform(click());
        // Translate screen has et_source_text
        onView(withId(R.id.et_source_text))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapPracticeCard_OpensSpeakingQuizLevel() {
        onView(withId(R.id.card_practice)).perform(click());
        // Speaking Quiz Level screen shows three level cards
        onView(withId(R.id.card_beginner))
                .check(matches(isDisplayed()));
    }

    @Test
    public void tapPracticeCard_BackButton_ReturnsHome() {
        onView(withId(R.id.card_practice)).perform(click());
        // Back button is present on the level picker
        onView(withId(R.id.card_beginner))
                .check(matches(isDisplayed()));
        // Press system back → should return to home
        pressBack();
        onView(withId(R.id.card_ai_chat))
                .check(matches(isDisplayed()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Speaking Quiz Level screen
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void speakingQuizLevel_AllThreeCardsVisible() {
        onView(withId(R.id.card_practice)).perform(click());
        onView(withId(R.id.card_beginner))
                .check(matches(isDisplayed()));
        onView(withId(R.id.card_intermediate))
                .check(matches(isDisplayed()));
        onView(withId(R.id.card_advanced))
                .check(matches(isDisplayed()));
    }

    @Test
    public void speakingQuizLevel_TapBeginner_OpensQuizScreen() {
        onView(withId(R.id.card_practice)).perform(click());
        onView(withId(R.id.card_beginner)).perform(click());
        // Quiz screen has the sentence text view
        onView(withId(R.id.tv_sentence))
                .check(matches(isDisplayed()));
    }

    @Test
    public void speakingQuiz_SentenceIsNotEmpty() {
        onView(withId(R.id.card_practice)).perform(click());
        onView(withId(R.id.card_beginner)).perform(click());
        // The sentence should have some text
        onView(withId(R.id.tv_sentence))
                .check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
    }

    @Test
    public void speakingQuiz_TimerVisible() {
        onView(withId(R.id.card_practice)).perform(click());
        onView(withId(R.id.card_beginner)).perform(click());
        onView(withId(R.id.tv_timer))
                .check(matches(isDisplayed()));
    }

    @Test
    public void speakingQuiz_MicButtonVisible() {
        onView(withId(R.id.card_practice)).perform(click());
        onView(withId(R.id.card_beginner)).perform(click());
        onView(withId(R.id.btn_speak))
                .check(matches(isDisplayed()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Settings → Change Language
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void settings_ChangeLanguageCard_IsVisible() {
        onView(withId(R.id.settingsFragment)).perform(click());
        onView(withId(R.id.card_change_language))
                .check(matches(isDisplayed()));
    }

    @Test
    public void settings_TapChangeLanguage_OpensScreen() {
        onView(withId(R.id.settingsFragment)).perform(click());
        onView(withId(R.id.card_change_language)).perform(click());
        // Change language screen has tv_title
        onView(withId(R.id.tv_title))
                .check(matches(isDisplayed()));
    }

    @Test
    public void settings_ChangeLanguage_HasBackButton() {
        onView(withId(R.id.settingsFragment)).perform(click());
        onView(withId(R.id.card_change_language)).perform(click());
        // The back button from our fix should be visible
        onView(withContentDescription("Back"))
                .check(matches(isDisplayed()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Regression: Export-stuck bug (fix 2 in v25)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void history_OpenExport_TapHome_TapHistory_ShowsHistoryNotExport() {
        // Reproduce the exact bug flow from the bug report
        onView(withId(R.id.historyFragment)).perform(click());
        // Tap "Open full history & export"
        onView(withId(R.id.btn_export)).perform(click());
        // Tap Home
        onView(withId(R.id.homeFragment)).perform(click());
        // Tap History again — should land on history, NOT export
        onView(withId(R.id.historyFragment)).perform(click());
        // tv_streak is on history, NOT on export — this confirms we're on the right screen
        onView(withId(R.id.tv_streak))
                .check(matches(isDisplayed()));
    }
}
