package com.aitranslator.app.ui.story;

import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentAiStoryBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class AiStoryFragment extends Fragment {

    private FragmentAiStoryBinding binding;
    private AiStoryViewModel viewModel;
    private TtsManager tts;
    private boolean isSpeaking = false;
    private String currentStoryText = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAiStoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiStoryViewModel.class);
        tts = new TtsManager(requireContext(), null);

        PrefsManager prefs = PrefsManager.getInstance(requireContext());
        binding.tvReadingLang.setText("Stories in " + prefs.getTargetLanguage());

        setupSpinners();
        AnimUtils.slideInFromLeft(binding.cardControls, 100);
        AnimUtils.fadeInUp(binding.btnGenerateStory, 200);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.tvLoadingMsg.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGenerateStory.setEnabled(!loading);
            if (loading) {
                binding.cardStoryContent.setVisibility(View.GONE);
                isSpeaking = false;
                tts.stop();
                updateSpeakButton(false);
            }
        });

        viewModel.getStory().observe(getViewLifecycleOwner(), story -> {
            if (story == null) return;
            currentStoryText = story.body;
            binding.cardStoryContent.setVisibility(View.VISIBLE);
            binding.tvStoryTitle.setText(story.title.isEmpty()
                ? binding.spinnerGenre.getSelectedItem().toString() : story.title);
            binding.tvLevelBadge.setText(story.level);
            binding.tvStoryBody.setText(story.body);
            binding.tvStoryVocab.setText(story.vocabulary);
            binding.tvStoryQuestions.setText(story.comprehensionQuestions);
            binding.btnSpeakStory.setEnabled(!currentStoryText.isEmpty());

            AnimUtils.fadeInUp(binding.cardStoryContent, 0);
            AnimUtils.slideInFromLeft(binding.cardStoryVocab, 150);
            AnimUtils.slideInFromRight(binding.cardStoryQuestions, 300);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });

        binding.btnGenerateStory.setOnClickListener(v -> {
            String level = binding.spinnerLevel.getSelectedItem().toString();
            String genre = binding.spinnerGenre.getSelectedItem().toString();
            AnimUtils.pulse(binding.btnGenerateStory);
            binding.btnGenerateStory.postDelayed(() ->
                viewModel.generateStory(level, genre), 150);
        });

        binding.btnSpeakStory.setOnClickListener(v -> {
            if (isSpeaking) {
                tts.stop();
                isSpeaking = false;
                updateSpeakButton(false);
            } else if (!currentStoryText.isEmpty()) {
                String langCode = prefs.getTargetLanguageCode();
                tts.speak(currentStoryText, langCode);
                isSpeaking = true;
                updateSpeakButton(true);
            }
        });

        binding.btnCopyStory.setOnClickListener(v -> {
            if (!currentStoryText.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("AI Story", currentStoryText));
                Toast.makeText(requireContext(), "Story copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, AiStoryViewModel.LEVELS);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLevel.setAdapter(levelAdapter);
        binding.spinnerLevel.setSelection(1); // Elementary default

        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, AiStoryViewModel.GENRES);
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerGenre.setAdapter(genreAdapter);
    }

    private void updateSpeakButton(boolean speaking) {
        binding.btnSpeakStory.setText(speaking ? "⏹  Stop" : "🔊  Read Aloud");
        binding.btnSpeakStory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            speaking ? 0xFFFF6B6B : 0xFF3D52A0));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}
