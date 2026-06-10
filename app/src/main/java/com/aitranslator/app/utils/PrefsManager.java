package com.aitranslator.app.utils;
import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREFS_NAME = "lingua_ai_prefs";
    private static PrefsManager instance;
    private final SharedPreferences prefs;

    public static PrefsManager getInstance(Context context) {
        if (instance == null) instance = new PrefsManager(context.getApplicationContext());
        return instance;
    }

    private PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setNativeLanguage(String name, String code) {
        prefs.edit().putString("native_name", name).putString("native_code", code).apply();
    }
    public void setTargetLanguage(String name, String code) {
        prefs.edit().putString("target_name", name).putString("target_code", code).apply();
    }

    public String getNativeLanguage() { return prefs.getString("native_name", "English"); }
    public String getNativeLanguageCode() { return prefs.getString("native_code", "en"); }
    public String getTargetLanguage() { return prefs.getString("target_name", "Spanish"); }
    public String getTargetLanguageCode() { return prefs.getString("target_code", "es"); }

    public boolean isOnboardingDone() { return prefs.getBoolean("onboarding_done", false); }
    public void setOnboardingDone() { prefs.edit().putBoolean("onboarding_done", true).apply(); }

    public int getStreak() { return prefs.getInt("streak", 0); }
    public void incrementStreak() { prefs.edit().putInt("streak", getStreak() + 1).apply(); }
    public int getTotalSessions() { return prefs.getInt("sessions", 0); }
    public void incrementSessions() { prefs.edit().putInt("sessions", getTotalSessions() + 1).apply(); }
    public String getLastActiveDate() { return prefs.getString("last_active", ""); }
    public void setLastActiveDate(String date) { prefs.edit().putString("last_active", date).apply(); }

    // ── XP / Quiz (Batch 3) ────────────────────────────────────────────────
    public int getTotalXp() { return prefs.getInt("total_xp", 0); }
    public void addXp(int amount) { prefs.edit().putInt("total_xp", getTotalXp() + amount).apply(); }

    // ── Weekly streak goal (Batch 3) ────────────────────────────────────────
    public int getWeeklyGoalDays() { return prefs.getInt("weekly_goal_days", 5); }
    public void setWeeklyGoalDays(int days) { prefs.edit().putInt("weekly_goal_days", days).apply(); }
}
