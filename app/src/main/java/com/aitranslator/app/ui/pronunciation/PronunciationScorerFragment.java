package com.aitranslator.app.ui.pronunciation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentPronunciationScorerBinding;
import com.aitranslator.app.service.SpeechRecognitionManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class PronunciationScorerFragment extends Fragment {

    private FragmentPronunciationScorerBinding binding;
    private PronunciationScorerViewModel viewModel;
    private SpeechRecognitionManager speechManager;
    private PrefsManager prefs;
    private String currentTargetText = "";
    private boolean isListening = false;

    private final ActivityResultLauncher<String> micPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startListening();
            else Toast.makeText(requireContext(),
                "Microphone permission needed for pronunciation scoring", Toast.LENGTH_LONG).show();
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPronunciationScorerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PronunciationScorerViewModel.class);
        prefs = PrefsManager.getInstance(requireContext());
        speechManager = new SpeechRecognitionManager(requireContext());

        AnimUtils.slideInFromLeft(binding.cardPhraseArea, 100);
        AnimUtils.slideInFromRight(binding.cardMicArea, 200);

        binding.tvLanguageHint.setText("Practising " + prefs.getTargetLanguage() + " pronunciation");

        // Observers
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGetPhrase.setEnabled(!loading);
            binding.btnSpeak.setEnabled(!loading && !currentTargetText.isEmpty());
        });

        viewModel.getPracticePhrase().observe(getViewLifecycleOwner(), phrase -> {
            if (phrase == null) return;
            currentTargetText = phrase;
            binding.tvPhraseDisplay.setText(phrase);
            binding.tvPhraseDisplay.setTextColor(requireContext().getColor(com.aitranslator.app.R.color.text_primary));
            binding.tvSpokenResult.setText("Press 🎤 and read the phrase above");
            binding.tvSpokenResult.setVisibility(View.VISIBLE);
            binding.btnSpeak.setEnabled(true);
            binding.cardScoreResult.setVisibility(View.GONE);
            AnimUtils.fadeInScale(binding.tvPhraseDisplay, 0);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            showScoreResult(result);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });

        // Buttons
        binding.btnGetPhrase.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnGetPhrase);
            viewModel.generatePracticePhrase();
        });

        binding.btnSpeak.setOnClickListener(v -> {
            if (isListening) {
                speechManager.stopListening();
                stopListeningUI();
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startListening();
                } else {
                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
                }
            }
        });

        binding.btnCustomPhrase.setOnClickListener(v -> {
            String custom = binding.etCustomPhrase.getText().toString().trim();
            if (custom.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a phrase first", Toast.LENGTH_SHORT).show();
            } else {
                currentTargetText = custom;
                binding.tvPhraseDisplay.setText(custom);
                binding.etCustomPhrase.setText("");
                binding.btnSpeak.setEnabled(true);
                binding.cardScoreResult.setVisibility(View.GONE);
                binding.tvSpokenResult.setText("Press 🎤 and say the phrase above");
                binding.tvSpokenResult.setVisibility(View.VISIBLE);
            }
        });

        // Generate first phrase automatically
        viewModel.generatePracticePhrase();
    }

    private void startListening() {
        isListening = true;
        binding.btnSpeak.setText("⏹  Stop");
        binding.btnSpeak.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFFFF6B6B));
        binding.tvSpokenResult.setText("🎤  Listening…");
        AnimUtils.pulse(binding.cardMicArea);

        speechManager.startListening(prefs.getTargetLanguageCode(),
            new SpeechRecognitionManager.SpeechCallback() {
                @Override public void onReadyForSpeech() {}
                @Override public void onSpeechDetected() {
                    requireActivity().runOnUiThread(() ->
                        binding.tvSpokenResult.setText("🎤  Speech detected…"));
                }
                @Override public void onPartialResult(String partial) {
                    requireActivity().runOnUiThread(() ->
                        binding.tvSpokenResult.setText("Hearing: " + partial));
                }
                @Override public void onResult(String text) {
                    requireActivity().runOnUiThread(() -> {
                        stopListeningUI();
                        binding.tvSpokenResult.setText("You said: \"" + text + "\"");
                        viewModel.scorePronunciation(currentTargetText, text);
                    });
                }
                @Override public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        stopListeningUI();
                        binding.tvSpokenResult.setText("Error: " + message + " — try again");
                    });
                }
                @Override public void onEndOfSpeech() {
                    requireActivity().runOnUiThread(() -> stopListeningUI());
                }
            });
    }

    private void stopListeningUI() {
        isListening = false;
        binding.btnSpeak.setText("🎤  Speak");
        binding.btnSpeak.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFF3D52A0));
    }

    private void showScoreResult(PronunciationScorerViewModel.ScorerResult result) {
        binding.cardScoreResult.setVisibility(View.VISIBLE);

        // Score arc colour
        int score = result.score;
        int color;
        String grade;
        if (score >= 90) { color = 0xFF2ECC71; grade = "Excellent! 🏆"; }
        else if (score >= 75) { color = 0xFF3D52A0; grade = "Very Good! ⭐"; }
        else if (score >= 60) { color = 0xFFFFB347; grade = "Good! 💪"; }
        else if (score >= 40) { color = 0xFFFF9800; grade = "Keep Practising 📚"; }
        else { color = 0xFFFF6B6B; grade = "Needs Work 🎯"; }

        binding.tvScoreNumber.setText(String.valueOf(score));
        binding.tvScoreNumber.setTextColor(color);
        binding.tvScoreGrade.setText(grade);
        binding.tvScoreFeedback.setText(result.feedback);
        binding.tvScoreTips.setText(result.tips);

        AnimUtils.fadeInScale(binding.tvScoreNumber, 0);
        AnimUtils.fadeInUp(binding.cardScoreResult, 100);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechManager != null) speechManager.destroy();
        binding = null;
    }
}
