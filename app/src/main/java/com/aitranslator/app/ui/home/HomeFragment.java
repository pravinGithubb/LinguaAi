package com.aitranslator.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentHomeBinding;
import com.aitranslator.app.utils.AnimUtils;

/**
 * Home tab — Lernix-inspired layout.
 *
 * Surfaces only the four primary actions:
 *   • AI Chat Tutor      → chatFragment
 *   • Vocabulary         → vocabularyListFragment
 *   • Translate          → translateFragment
 *   • Practice           → speakingQuizLevelFragment
 *
 * Everything else lives behind the "More features" pill at the bottom,
 * which opens {@link FeatureBottomSheet}.
 *
 * No business logic here — this is pure routing + entrance animations.
 * The previous incarnation of this fragment held streak/session/XP stats
 * and ~20 feature cards; that's all moved out now to keep the home screen
 * as calm as the Lernix reference.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show the user's current target-language flag so they know
        // "what they're learning right now" at a glance, and let them
        // tap it to open the Change Language screen quickly.
        com.aitranslator.app.utils.PrefsManager prefs =
                com.aitranslator.app.utils.PrefsManager.getInstance(requireContext());
        binding.tvLangFlag.setText(
                com.aitranslator.app.utils.LanguageUtils.getFlagEmoji(
                        prefs.getTargetLanguageCode()));
        binding.cardLangFlag.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardLangFlag);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.changeLanguageFragment), 150);
        });

        // Staggered entrance — hero, then row, then practice, then pill.
        AnimUtils.fadeInUp(binding.cardAiChat,      120);
        AnimUtils.slideInFromLeft(binding.cardVocabulary,  240);
        AnimUtils.slideInFromRight(binding.cardTranslate,   240);
        AnimUtils.fadeInUp(binding.cardPractice,    360);
        AnimUtils.slideInFromLeft(binding.cardWordOfDay, 420);
        AnimUtils.slideInFromRight(binding.cardQuiz,     420);
        AnimUtils.fadeInUp(binding.cardMoreOptions, 540);

        // ── Click handlers ────────────────────────────────────────────────
        // The hero card and its inner CTA both go to chat.
        View.OnClickListener openChat = v -> {
            AnimUtils.pulse(binding.cardAiChat);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.chatFragment), 150);
        };
        binding.cardAiChat.setOnClickListener(openChat);
        binding.btnLearnAi.setOnClickListener(openChat);

        binding.cardVocabulary.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardVocabulary);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.vocabularyListFragment), 150);
        });

        binding.cardTranslate.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardTranslate);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.translateFragment), 150);
        });

        binding.cardPractice.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardPractice);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.speakingQuizLevelFragment), 150);
        });

        binding.cardWordOfDay.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardWordOfDay);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.wordOfDayFragment), 150);
        });

        binding.cardQuiz.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardQuiz);
            v.postDelayed(() ->
                Navigation.findNavController(view).navigate(R.id.quizFragment), 150);
        });

        binding.cardMoreOptions.setOnClickListener(v -> {
            AnimUtils.pulse(binding.cardMoreOptions);
            new FeatureBottomSheet().show(getParentFragmentManager(), "features");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
