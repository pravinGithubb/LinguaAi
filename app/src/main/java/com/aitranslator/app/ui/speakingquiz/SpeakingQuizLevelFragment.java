package com.aitranslator.app.ui.speakingquiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentSpeakingQuizLevelBinding;
import com.aitranslator.app.utils.AnimUtils;

/**
 * First step of the Speaking Quiz flow — the user picks Beginner /
 * Intermediate / Advanced. The chosen level is passed to
 * {@link SpeakingQuizFragment} as a String Bundle argument under
 * {@link #ARG_LEVEL}.
 *
 * Pure presentation: no ViewModel needed at this step, no network, no DB.
 */
public class SpeakingQuizLevelFragment extends Fragment {

    /** Bundle key used to pass the chosen level to the quiz fragment. */
    public static final String ARG_LEVEL = "level";

    private FragmentSpeakingQuizLevelBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpeakingQuizLevelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Staggered entrance — matches the rest of the app's vibe.
        AnimUtils.slideInFromLeft(binding.cardBeginner, 100);
        AnimUtils.slideInFromRight(binding.cardIntermediate, 200);
        AnimUtils.slideInFromLeft(binding.cardAdvanced, 300);

        binding.cardBeginner.setOnClickListener(v -> launch(SpeakingQuizSentences.Level.BEGINNER, v));
        binding.cardIntermediate.setOnClickListener(v -> launch(SpeakingQuizSentences.Level.INTERMEDIATE, v));
        binding.cardAdvanced.setOnClickListener(v -> launch(SpeakingQuizSentences.Level.ADVANCED, v));
    }

    private void launch(SpeakingQuizSentences.Level level, View source) {
        AnimUtils.pulse(source);
        Bundle args = new Bundle();
        args.putString(ARG_LEVEL, level.name());
        // Small delay lets the pulse animation breathe before the screen transition.
        source.postDelayed(() ->
            Navigation.findNavController(source)
                      .navigate(R.id.action_speaking_level_to_quiz, args), 150);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
