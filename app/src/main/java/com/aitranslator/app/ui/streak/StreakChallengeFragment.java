package com.aitranslator.app.ui.streak;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.data.local.entity.QuizResult;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentStreakChallengeBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.text.SimpleDateFormat;
import java.util.*;

public class StreakChallengeFragment extends Fragment {

    private FragmentStreakChallengeBinding binding;
    private PrefsManager prefs;
    private AppRepository repository;

    // Badge thresholds (streak days)
    private static final int[] BADGE_THRESHOLDS = {3, 7, 14, 30, 60, 100};
    private static final String[] BADGE_NAMES = {"Starter", "Week Warrior", "Fortnight", "Monthly", "Bi-Monthly", "Century"};
    private static final String[] BADGE_EMOJI = {"🌱", "⚡", "🔥", "⭐", "💎", "👑"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStreakChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());
        repository = new AppRepository(requireActivity().getApplication());

        loadData();

        AnimUtils.slideInFromLeft(binding.cardCurrentStreak, 100);
        AnimUtils.slideInFromRight(binding.cardWeeklyGoal, 200);
        AnimUtils.fadeInUp(binding.cardBadges, 300);
        AnimUtils.fadeInUp(binding.cardXpProgress, 400);

        binding.btnSetGoal3.setOnClickListener(v -> setGoal(3));
        binding.btnSetGoal5.setOnClickListener(v -> setGoal(5));
        binding.btnSetGoal7.setOnClickListener(v -> setGoal(7));
    }

    private void loadData() {
        int streak = prefs.getStreak();
        int goal = prefs.getWeeklyGoalDays();
        int xp = prefs.getTotalXp();

        // Current streak card
        binding.tvStreakNumber.setText(String.valueOf(streak));
        binding.tvStreakLabel.setText(streak == 1 ? "day streak" : "days streak");
        binding.tvStreakMotivation.setText(getStreakMessage(streak));

        // Weekly goal progress
        int daysThisWeek = Math.min(streak, 7);
        binding.tvGoalProgress.setText(daysThisWeek + " / " + goal + " days this week");
        binding.progressWeeklyGoal.setMax(goal);
        binding.progressWeeklyGoal.setProgress(Math.min(daysThisWeek, goal));
        highlightGoalButton(goal);

        if (daysThisWeek >= goal) {
            binding.tvGoalStatus.setText("🎉 Weekly goal achieved!");
            binding.tvGoalStatus.setTextColor(0xFF2ECC71);
        } else {
            int remaining = goal - daysThisWeek;
            binding.tvGoalStatus.setText(remaining + " more day" + (remaining == 1 ? "" : "s") + " to reach your goal");
            binding.tvGoalStatus.setTextColor(0xFF6B7280);
        }

        // XP card
        binding.tvXpAmount.setText(xp + " XP");
        binding.tvXpLevel.setText("Level " + (xp / 100 + 1));
        int xpInLevel = xp % 100;
        binding.progressXp.setMax(100);
        binding.progressXp.setProgress(xpInLevel);
        binding.tvXpNextLevel.setText(xpInLevel + " / 100 XP to next level");

        // Badges
        renderBadges(streak);

        // Quiz history
        repository.getRecentQuizResults().observe(getViewLifecycleOwner(), results -> {
            if (results == null || results.isEmpty()) {
                binding.tvQuizHistory.setText("No quizzes completed yet. Take a quiz to earn XP!");
                return;
            }
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                QuizResult r = results.get(i);
                int pct = r.totalQuestions > 0 ? (r.correctAnswers * 100 / r.totalQuestions) : 0;
                sb.append(sdf.format(new Date(r.timestamp)))
                  .append("  ").append(r.correctAnswers).append("/").append(r.totalQuestions)
                  .append("  (").append(pct).append("%)  +").append(r.xpEarned).append(" XP\n");
            }
            binding.tvQuizHistory.setText(sb.toString().trim());
        });
    }

    private void renderBadges(int streak) {
        StringBuilder earned = new StringBuilder();
        StringBuilder upcoming = new StringBuilder();
        boolean foundNext = false;
        for (int i = 0; i < BADGE_THRESHOLDS.length; i++) {
            if (streak >= BADGE_THRESHOLDS[i]) {
                earned.append(BADGE_EMOJI[i]).append(" ").append(BADGE_NAMES[i]).append("\n");
            } else if (!foundNext) {
                int needed = BADGE_THRESHOLDS[i] - streak;
                upcoming.append("Next: ").append(BADGE_EMOJI[i]).append(" ").append(BADGE_NAMES[i])
                        .append(" in ").append(needed).append(" day").append(needed == 1 ? "" : "s");
                foundNext = true;
            }
        }
        binding.tvEarnedBadges.setText(earned.length() > 0 ? earned.toString().trim() : "Keep going to earn your first badge!");
        binding.tvUpcomingBadge.setText(upcoming.length() > 0 ? upcoming.toString() : "🏆 All badges earned!");
    }

    private void setGoal(int days) {
        prefs.setWeeklyGoalDays(days);
        highlightGoalButton(days);
        int daysThisWeek = Math.min(prefs.getStreak(), 7);
        binding.tvGoalProgress.setText(daysThisWeek + " / " + days + " days this week");
        binding.progressWeeklyGoal.setMax(days);
        binding.progressWeeklyGoal.setProgress(Math.min(daysThisWeek, days));
        AnimUtils.pulse(days == 3 ? binding.btnSetGoal3 : days == 5 ? binding.btnSetGoal5 : binding.btnSetGoal7);
    }

    private void highlightGoalButton(int goal) {
        int active = 0xFF3D52A0;
        int inactive = 0xFFE5E7EB;
        binding.btnSetGoal3.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(goal == 3 ? active : inactive));
        binding.btnSetGoal5.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(goal == 5 ? active : inactive));
        binding.btnSetGoal7.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(goal == 7 ? active : inactive));
        binding.btnSetGoal3.setTextColor(goal == 3 ? 0xFFFFFFFF : 0xFF6B7280);
        binding.btnSetGoal5.setTextColor(goal == 5 ? 0xFFFFFFFF : 0xFF6B7280);
        binding.btnSetGoal7.setTextColor(goal == 7 ? 0xFFFFFFFF : 0xFF6B7280);
    }

    private String getStreakMessage(int streak) {
        if (streak == 0) return "Start your streak today! 🚀";
        if (streak < 3) return "Great start! Keep it going 💪";
        if (streak < 7) return "Building momentum! 🔥";
        if (streak < 14) return "One week streak! Amazing! ⭐";
        if (streak < 30) return "Two weeks strong! You're dedicated 🏅";
        return "Legendary dedication! 👑 " + streak + " days!";
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
