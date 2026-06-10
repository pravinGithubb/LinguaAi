package com.aitranslator.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aitranslator.app.BuildConfig;
import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentSettingsBinding;
import com.aitranslator.app.utils.PrefsManager;

/**
 * Settings tab — Lernix-style hub of preference cards.
 *
 * Each card here links to an existing screen rather than embedding settings
 * inline; this keeps the screen scannable and doesn't duplicate UI that
 * already exists elsewhere (e.g. streak goals live inside
 * StreakChallengeFragment).
 *
 * Pure routing — no business state lives here. Subtitle text is refreshed
 * in onResume so the language pair stays in sync after the user changes it.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private PrefsManager prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());

        binding.cardChangeLanguage.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.changeLanguageFragment));
        binding.cardStreak.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.streakChallengeFragment));
        binding.cardPacks.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.languagePacksFragment));
        binding.cardExport.setOnClickListener(v ->
            Navigation.findNavController(view).navigate(R.id.historyExportFragment));
        binding.cardAbout.setOnClickListener(v ->
            Toast.makeText(requireContext(),
                    "LinguaAI " + BuildConfig.VERSION_NAME + " — built with ❤️",
                    Toast.LENGTH_SHORT).show());

        binding.tvAboutSubtitle.setText("Version " + BuildConfig.VERSION_NAME);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh language subtitle in case the user just changed it.
        if (binding != null && prefs != null) {
            binding.tvLangSubtitle.setText(
                    prefs.getNativeLanguage() + " → " + prefs.getTargetLanguage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
