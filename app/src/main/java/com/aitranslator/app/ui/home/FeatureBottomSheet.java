package com.aitranslator.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.aitranslator.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bottom sheet showing the secondary feature catalog.
 *
 * The Lernix-style home screen surfaces only four primary actions
 * (Chat, Vocabulary, Translate, Practice). Everything else lives here so
 * the home screen stays calm and discoverable while no functionality is
 * lost.
 *
 * Each tile is a small clickable card with an emoji icon, title and a
 * one-line subtitle. Taps navigate to the relevant fragment in the nav
 * graph and dismiss the sheet.
 *
 * Layout note: I'm building rows in code (two tiles per row) rather than
 * adding the GridLayout dependency just for this one screen. That keeps
 * the build.gradle untouched.
 */
public class FeatureBottomSheet extends BottomSheetDialogFragment {

    /** All the catalog entries. Add a new line here to add a new tile. */
    private static final List<Feature> FEATURES = Arrays.asList(
            // Voice + speech
            new Feature("🎤", "Voice Translate", "Speak → translate",   R.id.voiceFragment),
            new Feature("📖", "Word of the Day", "Daily new word",      R.id.wordOfDayFragment),
            new Feature("📚", "My Vocabulary",   "Saved words list",    R.id.vocabularyListFragment),
            // Tools
            new Feature("📷", "Camera OCR",      "Scan & translate text", R.id.cameraOcrFragment),
            new Feature("📄", "PDF Reader",      "Translate PDF pages", R.id.pdfReaderFragment),
            // Learn & practice
            new Feature("❓", "Quiz",            "10 AI questions",     R.id.quizFragment),
            new Feature("📝", "Daily Lesson",    "Structured topics",   R.id.dailyLessonFragment),
            new Feature("🔥", "Streak Challenge", "Goals & badges",     R.id.streakChallengeFragment),
            // Skills
            new Feature("✏️", "Grammar Checker", "Fix your writing",    R.id.grammarCheckerFragment),
            new Feature("🗣️", "Pronunciation",   "Score your speech",   R.id.pronunciationScorerFragment),
            new Feature("📖", "AI Story",        "Read & learn",        R.id.aiStoryFragment),
            // Travel & offline
            new Feature("💬", "Conversation",    "Role-play scenarios", R.id.conversationModeFragment),
            new Feature("🧳", "Phrasebook",      "Travel phrases",      R.id.phrasebookFragment),
            new Feature("📦", "Language Packs",  "Offline translation", R.id.languagePacksFragment),
            // Media
            // Misc
            new Feature("📜", "History & Export", "Save/share progress", R.id.historyExportFragment)
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_features, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout grid = view.findViewById(R.id.grid_features);

        // The bottom sheet is launched from the home fragment, so we need
        // to navigate using the parent activity's NavController, not one
        // owned by this dialog (BottomSheetDialogFragment lives in its own
        // window).
        NavController nav = Navigation.findNavController(
                requireActivity(), R.id.nav_host_fragment);

        // Build rows of two tiles each.
        List<View> rowChildren = new ArrayList<>(2);
        for (int i = 0; i < FEATURES.size(); i++) {
            Feature f = FEATURES.get(i);
            View tile = makeTile(f, grid, nav);
            rowChildren.add(tile);
            boolean rowFull = rowChildren.size() == 2;
            boolean lastTile = i == FEATURES.size() - 1;
            if (rowFull || lastTile) {
                grid.addView(makeRow(rowChildren));
                rowChildren.clear();
            }
        }
    }

    /** A horizontal LinearLayout holding 1-2 tiles with equal weight. */
    private LinearLayout makeRow(List<View> children) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(10);
        row.setLayoutParams(rp);
        for (View v : children) row.addView(v);
        // If the row only has one tile, pad it with a spacer so it doesn't stretch.
        if (children.size() == 1) {
            View spacer = new View(requireContext());
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            spacer.setLayoutParams(sp);
            row.addView(spacer);
        }
        return row;
    }

    /** Inflates a single tile and wires its click handler. */
    private View makeTile(Feature f, ViewGroup parent, NavController nav) {
        View tile = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_feature_tile, parent, false);

        // Equal-weight columns inside the row.
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        // Inner gap between the two tiles in a row.
        if (parent.getChildCount() % 2 == 0) lp.rightMargin = dp(6);
        else                                  lp.leftMargin  = dp(6);
        tile.setLayoutParams(lp);

        ((TextView) tile.findViewById(R.id.tv_emoji)).setText(f.emoji);
        ((TextView) tile.findViewById(R.id.tv_title)).setText(f.title);
        ((TextView) tile.findViewById(R.id.tv_subtitle)).setText(f.subtitle);

        tile.setOnClickListener(v -> {
            try {
                nav.navigate(f.destinationId);
            } catch (Exception ex) {
                // Defensive — if a destination id was renamed, swallow rather
                // than crash. The user gets a no-op tap, which is preferable.
            }
            dismiss();
        });
        return tile;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** Tiny POJO describing one tile. */
    private static final class Feature {
        final String emoji, title, subtitle;
        final int destinationId;
        Feature(String emoji, String title, String subtitle, int destinationId) {
            this.emoji = emoji;
            this.title = title;
            this.subtitle = subtitle;
            this.destinationId = destinationId;
        }
    }
}
