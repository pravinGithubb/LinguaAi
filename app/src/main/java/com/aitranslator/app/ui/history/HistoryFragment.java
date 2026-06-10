package com.aitranslator.app.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.QuizResult;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentHistoryBinding;
import com.aitranslator.app.utils.PrefsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * History tab — surfaces three quick-glance stat cards (streak / sessions /
 * total XP) and a "Recent activity" feed combining translation history and
 * quiz results.
 *
 * Activity rows are built dynamically in code rather than via a
 * RecyclerView adapter, because the volume is small (we cap at 20 rows
 * total) and a vertical LinearLayout inside a NestedScrollView is much
 * simpler to maintain. If history grows large in future, swap the
 * container for a RecyclerView without touching anything else.
 *
 * The "Open full history & export" button defers to the existing
 * HistoryExportFragment so we don't reinvent the export pipeline.
 */
public class HistoryFragment extends Fragment {

    /** Maximum activity rows to render (keeps scroll length sane). */
    private static final int MAX_ROWS = 20;

    private FragmentHistoryBinding binding;
    private PrefsManager prefs;
    private AppRepository repo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());
        repo  = new AppRepository(requireActivity().getApplication());

        // Stats
        binding.tvStreak.setText(String.valueOf(prefs.getStreak()));
        binding.tvSessions.setText(String.valueOf(prefs.getTotalSessions()));
        binding.tvXp.setText(String.valueOf(prefs.getTotalXp()));

        // Quiz results — observe LiveData and append rows.
        repo.getRecentQuizResults().observe(getViewLifecycleOwner(), this::renderQuizRows);

        // Translation history — observe and append rows below quiz rows.
        repo.getAllHistory().observe(getViewLifecycleOwner(), this::renderTranslationRows);

        // Export button → existing fragment in nav graph.
        binding.btnExport.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.historyExportFragment));
    }

    /** Re-rendered every time quiz results change. */
    private void renderQuizRows(List<QuizResult> results) {
        // We always rebuild the whole list from scratch on either feed
        // changing — keeps the merge logic trivial.
        rebuildList(results, lastTranslations);
        updateWeeklyChart(results);
    }

    /**
     * Aggregates XP earned per day for the last 7 days and pushes it into
     * the bar chart. Translation history isn't included here because
     * translations don't currently award XP — feel free to extend if you
     * wire up XP for translations later.
     *
     * The XpBarChartView expects two parallel 7-element arrays:
     *   - xpPerDay[i]      — total XP earned that day
     *   - dayTimestamps[i] — start-of-day millis for that day's bar label
     * Index 0 = oldest day, index 6 = today.
     */
    private void updateWeeklyChart(List<QuizResult> quiz) {
        if (binding == null) return;

        int[]  perDay   = new int[7];
        long[] dayStarts = new long[7];

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();
        long dayMs = 24L * 60 * 60 * 1000;

        for (int i = 0; i < 7; i++) {
            // Index 0 = 6 days ago, index 6 = today.
            dayStarts[i] = startOfToday - (6 - i) * dayMs;
        }

        if (quiz != null) {
            for (QuizResult q : quiz) {
                long delta = startOfToday - (q.timestamp - (q.timestamp % dayMs));
                int daysAgo = (int) (delta / dayMs);
                if (daysAgo >= 0 && daysAgo < 7) {
                    perDay[6 - daysAgo] += q.xpEarned;
                }
            }
        }

        binding.xpChart.setData(perDay, dayStarts);
    }

    /** Re-rendered every time translation history changes. */
    private List<TranslationHistory> lastTranslations = java.util.Collections.emptyList();
    private List<QuizResult> lastQuiz = java.util.Collections.emptyList();
    private void renderTranslationRows(List<TranslationHistory> history) {
        lastTranslations = history;
        rebuildList(lastQuiz, history);
    }

    /** Combines both feeds into a chronological view, capped at MAX_ROWS. */
    private void rebuildList(List<QuizResult> quizzes, List<TranslationHistory> translations) {
        if (binding == null) return;
        lastQuiz = quizzes != null ? quizzes : java.util.Collections.emptyList();
        lastTranslations = translations != null ? translations : java.util.Collections.emptyList();

        LinearLayout container = binding.listContainer;
        container.removeAllViews();

        // Build a tiny tagged-row list and sort by timestamp desc.
        java.util.List<Row> all = new java.util.ArrayList<>();
        for (QuizResult q : lastQuiz) {
            all.add(new Row(q.timestamp,
                    "🎯 Quiz",
                    q.correctAnswers + "/" + q.totalQuestions
                            + "   •   +" + q.xpEarned + " XP   •   " + q.language));
        }
        for (TranslationHistory t : lastTranslations) {
            all.add(new Row(t.timestamp,
                    "🌐 " + t.fromLanguage + " → " + t.toLanguage,
                    truncate(t.originalText, 60)));
        }
        java.util.Collections.sort(all, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        if (all.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvEmpty.setVisibility(View.GONE);

        int count = Math.min(MAX_ROWS, all.size());
        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        for (int i = 0; i < count; i++) {
            Row r = all.get(i);
            container.addView(makeRow(r.title, r.subtitle, fmt.format(new Date(r.timestamp))));
        }
    }

    /** Builds one history row card. */
    private View makeRow(String title, String subtitle, String when) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_history_row, binding.listContainer, false);
        ((TextView) row.findViewById(R.id.tv_row_title)).setText(title);
        ((TextView) row.findViewById(R.id.tv_row_subtitle)).setText(subtitle);
        ((TextView) row.findViewById(R.id.tv_row_when)).setText(when);
        return row;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Tiny row holder used during sort/merge. */
    private static final class Row {
        final long timestamp;
        final String title, subtitle;
        Row(long ts, String t, String s) { timestamp = ts; title = t; subtitle = s; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
