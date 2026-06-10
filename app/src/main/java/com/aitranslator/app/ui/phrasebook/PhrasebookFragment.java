package com.aitranslator.app.ui.phrasebook;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.data.local.entity.Phrase;
import com.aitranslator.app.databinding.FragmentPhrasebookBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.util.List;

public class PhrasebookFragment extends Fragment {

    private FragmentPhrasebookBinding binding;
    private PhrasebookViewModel viewModel;
    private TtsManager tts;
    private PrefsManager prefs;
    private LiveData<List<Phrase>> currentSource;
    private final Observer<List<Phrase>> phrasesObserver = this::renderPhrases;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPhrasebookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PhrasebookViewModel.class);
        prefs = PrefsManager.getInstance(requireContext());
        tts = new TtsManager(requireContext(), null);

        binding.tvLangIndicator.setText("📚 " + prefs.getTargetLanguage() + " phrases");

        renderCategoryChips();
        observeAll();
        AnimUtils.fadeInUp(binding.layoutCategories, 100);
        AnimUtils.fadeInUp(binding.layoutPhraseList, 200);

        binding.btnGenerate.setOnClickListener(v -> {
            String cat = viewModel.getCurrentCategory().getValue();
            if (cat == null || cat.isEmpty()) {
                Toast.makeText(requireContext(), "Pick a category first", Toast.LENGTH_SHORT).show();
                return;
            }
            AnimUtils.pulse(binding.btnGenerate);
            viewModel.generatePhrasesForCategory(cat);
        });

        binding.btnFavorites.setOnClickListener(v -> {
            viewModel.showFavorites();
            highlightChip(null, true);
        });
    }

    private void observeAll() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGenerate.setEnabled(!loading);
        });

        viewModel.getStatus().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // Switch sources when category or favorites flag changes
        viewModel.getCurrentCategory().observe(getViewLifecycleOwner(), cat -> {
            if (cat == null || cat.isEmpty()) return;
            switchSource(viewModel.getPhrasesForCategory(cat));
            binding.tvCurrentCategory.setText(cat);
        });

        viewModel.getShowFavoritesOnly().observe(getViewLifecycleOwner(), fav -> {
            if (Boolean.TRUE.equals(fav)) {
                switchSource(viewModel.getFavorites());
                binding.tvCurrentCategory.setText("⭐ Favorites");
            }
        });

        // Default load: all phrases
        switchSource(viewModel.getAllPhrases());
        binding.tvCurrentCategory.setText("All phrases");
    }

    private void switchSource(LiveData<List<Phrase>> newSource) {
        if (currentSource != null) currentSource.removeObserver(phrasesObserver);
        currentSource = newSource;
        currentSource.observe(getViewLifecycleOwner(), phrasesObserver);
    }

    private void renderCategoryChips() {
        binding.containerCategories.removeAllViews();
        for (int i = 0; i < PhrasebookViewModel.CATEGORIES.length; i++) {
            String cat = PhrasebookViewModel.CATEGORIES[i];
            String emoji = PhrasebookViewModel.CATEGORY_EMOJIS[i];
            View chip = createCategoryChip(cat, emoji);
            binding.containerCategories.addView(chip);
        }
    }

    private View createCategoryChip(String category, String emoji) {
        com.google.android.material.card.MaterialCardView card =
            new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setStrokeColor(0xFFE5E7EB);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);
        card.setTag(category);

        TextView tv = new TextView(requireContext());
        tv.setText(emoji + "  " + category);
        tv.setTextSize(13f);
        tv.setTextColor(0xFF1A1F36);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));
        card.addView(tv);

        card.setOnClickListener(v -> {
            viewModel.selectCategory(category);
            highlightChip(card, false);
            AnimUtils.pulse(card);
        });
        return card;
    }

    private void highlightChip(View activeChip, boolean isFavorites) {
        for (int i = 0; i < binding.containerCategories.getChildCount(); i++) {
            View c = binding.containerCategories.getChildAt(i);
            com.google.android.material.card.MaterialCardView cv =
                (com.google.android.material.card.MaterialCardView) c;
            boolean isActive = !isFavorites && c == activeChip;
            cv.setCardBackgroundColor(isActive ? 0xFF3D52A0 : 0xFFFFFFFF);
            ((TextView) cv.getChildAt(0)).setTextColor(isActive ? 0xFFFFFFFF : 0xFF1A1F36);
        }
    }

    private void renderPhrases(List<Phrase> phrases) {
        binding.containerPhrases.removeAllViews();
        if (phrases == null || phrases.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No phrases yet. Pick a category above and tap '✨ Generate'.");
            empty.setTextSize(14f);
            empty.setTextColor(0xFF6B7280);
            empty.setPadding(dp(20), dp(40), dp(20), dp(40));
            empty.setGravity(android.view.Gravity.CENTER);
            binding.containerPhrases.addView(empty);
            binding.tvPhraseCount.setText("");
            return;
        }
        binding.tvPhraseCount.setText(phrases.size() + " phrase" + (phrases.size() == 1 ? "" : "s"));
        int delay = 0;
        for (Phrase p : phrases) {
            View row = createPhraseRow(p);
            binding.containerPhrases.addView(row);
            AnimUtils.fadeInUp(row, delay);
            delay += 50;
        }
    }

    private View createPhraseRow(Phrase p) {
        com.google.android.material.card.MaterialCardView card =
            new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeColor(0xFFE5E7EB);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(8), dp(12));

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView target = new TextView(requireContext());
        target.setText(p.phraseTarget);
        target.setTextSize(15f);
        target.setTextColor(0xFF1A1F36);
        target.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(target);

        TextView nat = new TextView(requireContext());
        nat.setText(p.phraseNative);
        nat.setTextSize(13f);
        nat.setTextColor(0xFF6B7280);
        col.addView(nat);

        if (p.phonetic != null && !p.phonetic.isEmpty()) {
            TextView phon = new TextView(requireContext());
            phon.setText("/" + p.phonetic + "/");
            phon.setTextSize(11f);
            phon.setTextColor(0xFF3D52A0);
            phon.setTypeface(null, android.graphics.Typeface.ITALIC);
            col.addView(phon);
        }

        // Action buttons
        ImageButton btnPlay = new ImageButton(requireContext());
        btnPlay.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnPlay.setBackground(null);
        btnPlay.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        btnPlay.setColorFilter(0xFF3D52A0);
        btnPlay.setOnClickListener(v -> {
            tts.speak(p.phraseTarget, prefs.getTargetLanguageCode());
            AnimUtils.pulse(btnPlay);
        });

        ImageButton btnFav = new ImageButton(requireContext());
        btnFav.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnFav.setBackground(null);
        btnFav.setImageResource(p.isFavorite ?
            com.aitranslator.app.R.drawable.ic_star_filled :
            com.aitranslator.app.R.drawable.ic_star_outline);
        btnFav.setColorFilter(p.isFavorite ? 0xFFFFB347 : 0xFF6B7280);
        btnFav.setOnClickListener(v -> {
            viewModel.toggleFavorite(p);
            AnimUtils.pulse(btnFav);
        });

        ImageButton btnCopy = new ImageButton(requireContext());
        btnCopy.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        btnCopy.setBackground(null);
        btnCopy.setImageResource(com.aitranslator.app.R.drawable.ic_copy);
        btnCopy.setColorFilter(0xFF6B7280);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Phrase", p.phraseTarget));
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show();
        });

        row.addView(col);
        row.addView(btnPlay);
        row.addView(btnFav);
        row.addView(btnCopy);
        card.addView(row);

        // Long press to delete
        card.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete phrase?")
                .setMessage("\"" + p.phraseTarget + "\"")
                .setPositiveButton("Delete", (d, w) -> viewModel.deletePhrase(p))
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });

        return card;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}
