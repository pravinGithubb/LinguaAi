package com.aitranslator.app.ui.voice;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentVoiceBinding;
import com.aitranslator.app.service.SpeechRecognitionManager;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

import java.util.Random;

public class VoiceFragment extends Fragment {

    private FragmentVoiceBinding binding;
    private VoiceViewModel viewModel;
    private SpeechRecognitionManager speechManager;
    private TtsManager tts;
    private PrefsManager prefs;

    private String fromLang, fromCode, toLang, toCode;
    private boolean isSwapped = false;
    private Handler waveformHandler = new Handler(Looper.getMainLooper());
    private Runnable waveformRunnable;
    private String lastTranslation = "";

    // Permission launcher
    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startVoiceRecognition();
                } else {
                    Toast.makeText(requireContext(),
                            "Microphone permission is required for voice translation",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVoiceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(VoiceViewModel.class);

        // Default: speak in native, translate to target
        fromLang = prefs.getNativeLanguage();
        fromCode = prefs.getNativeLanguageCode();
        toLang = prefs.getTargetLanguage();
        toCode = prefs.getTargetLanguageCode();

        setupLanguageDisplay();
        setupTts();
        setupSpeechManager();
        setupClickListeners();
        observeViewModel();

        // Entrance animation
        AnimUtils.fadeInUp(binding.cardLangSelector, 100);
        AnimUtils.fadeInScale(binding.micArea, 300);
        AnimUtils.fadeInUp(binding.statusCard, 500);
    }

    // ─── Setup ───────────────────────────────────────────────────

    private void setupLanguageDisplay() {
        binding.tvFromFlag.setText(LanguageUtils.getFlagEmoji(fromCode));
        binding.tvFromLang.setText(fromLang);
        binding.tvToFlag.setText(LanguageUtils.getFlagEmoji(toCode));
        binding.tvToLang.setText(toLang);
    }

    private void setupTts() {
        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {}
            @Override public void onError(String msg) {
                Toast.makeText(requireContext(), "TTS error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpeechManager() {
        if (!SpeechRecognitionManager.isAvailable(requireContext())) {
            binding.tvStatus.setText("⚠️ Speech recognition not available on this device");
            binding.btnMic.setAlpha(0.4f);
            binding.btnMic.setClickable(false);
            return;
        }
        speechManager = new SpeechRecognitionManager(requireContext());
    }

    private void setupClickListeners() {

        // Mic tap — start/stop recording
        binding.btnMic.setOnClickListener(v -> {
            if (speechManager == null) return;
            if (speechManager.isListening()) {
                stopListening();
            } else {
                checkMicPermissionAndStart();
            }
        });

        // Swap languages
        binding.btnSwapLangs.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnSwapLangs);

            // Rotate the swap button visually
            binding.btnSwapLangs.animate()
                    .rotation(binding.btnSwapLangs.getRotation() + 180f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            // Swap language values
            String tmpName = fromLang, tmpCode = fromCode;
            fromLang = toLang; fromCode = toCode;
            toLang = tmpName; toCode = tmpCode;
            isSwapped = !isSwapped;

            setupLanguageDisplay();
            viewModel.reset();
            hideResultCards();
        });

        // Replay translation via TTS
        binding.btnReplay.setOnClickListener(v -> {
            if (!lastTranslation.isEmpty()) {
                viewModel.onSpeakingStarted();
                tts.speak(lastTranslation, toCode);
                AnimUtils.pulse(binding.btnReplay);
                // Mark done after estimated speech duration
                binding.btnReplay.postDelayed(() -> viewModel.onSpeakingDone(), 2500);
            }
        });

        // Copy result
        binding.btnCopyResult.setOnClickListener(v -> {
            if (!lastTranslation.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager)
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("voice_translation", lastTranslation));
                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show();
                AnimUtils.pulse(binding.btnCopyResult);
            }
        });

        // History shortcut
        binding.btnViewHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.translateFragment));
    }

    // ─── Observe ViewModel ────────────────────────────────────────

    private void observeViewModel() {

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case IDLE:
                    setMicIdle();
                    binding.tvStatus.setText("Tap mic · Speak · Get instant translation");
                    binding.progressBar.setVisibility(View.GONE);
                    break;

                case LISTENING:
                    setMicRecording();
                    binding.tvStatus.setText("🎤 Listening… tap mic to stop");
                    binding.progressBar.setVisibility(View.GONE);
                    startWaveformAnimation();
                    break;

                case PROCESSING:
                    setMicIdle();
                    stopWaveformAnimation();
                    binding.tvStatus.setText("⚙️ Translating…");
                    binding.progressBar.setVisibility(View.VISIBLE);
                    break;

                case RESULT:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvStatus.setText("✅ Done · Tap mic to translate again");
                    break;

                case SPEAKING:
                    binding.tvStatus.setText("🔊 Speaking translation…");
                    break;

                case ERROR:
                    setMicIdle();
                    stopWaveformAnimation();
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvStatus.setText("Tap mic to try again");
                    break;
            }
        });

        // Partial text (live preview while speaking)
        viewModel.getPartialText().observe(getViewLifecycleOwner(), partial -> {
            if (partial != null && !partial.isEmpty()) {
                binding.tvSpokenText.setText(partial + "…");
                if (binding.cardSpoken.getVisibility() != View.VISIBLE) {
                    binding.cardSpoken.setVisibility(View.VISIBLE);
                    AnimUtils.popIn(binding.cardSpoken);
                }
            }
        });

        // Final spoken text
        viewModel.getSpokenText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                binding.tvSpokenText.setText(text);
                if (binding.cardSpoken.getVisibility() != View.VISIBLE) {
                    binding.cardSpoken.setVisibility(View.VISIBLE);
                    AnimUtils.popIn(binding.cardSpoken);
                }
            }
        });

        // Translation result
        viewModel.getTranslatedText().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isEmpty()) {
                lastTranslation = result;
                binding.tvTranslatedText.setText(result);

                if (binding.cardResult.getVisibility() != View.VISIBLE) {
                    binding.cardResult.setVisibility(View.VISIBLE);
                    AnimUtils.popIn(binding.cardResult);
                }

                // Auto-speak the translation
                viewModel.onSpeakingStarted();
                tts.speak(result, toCode);
                binding.btnReplay.postDelayed(() -> viewModel.onSpeakingDone(), 3000);
            }
        });

        // Errors
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─── Voice Recognition ────────────────────────────────────────

    private void checkMicPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition();
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVoiceRecognition() {
        viewModel.onListeningStarted();
        hideResultCards();

        speechManager.startListening(fromCode, new SpeechRecognitionManager.SpeechCallback() {

            @Override
            public void onReadyForSpeech() {
                // Mic is open
            }

            @Override
            public void onSpeechDetected() {
                // User started talking — waveform already animated
            }

            @Override
            public void onPartialResult(String partial) {
                requireActivity().runOnUiThread(() ->
                        viewModel.onPartialResult(partial));
            }

            @Override
            public void onResult(String text) {
                requireActivity().runOnUiThread(() ->
                        viewModel.onSpeechResult(text, fromLang, fromCode, toLang, toCode));
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    viewModel.onError(message);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onEndOfSpeech() {
                requireActivity().runOnUiThread(() -> stopWaveformAnimation());
            }
        });
    }

    private void stopListening() {
        if (speechManager != null) speechManager.stopListening();
        stopWaveformAnimation();
    }

    // ─── Mic Button Visual States ─────────────────────────────────

    private void setMicRecording() {
        // Red mic button + pulse rings
        binding.btnMic.setBackground(ContextCompat.getDrawable(
                requireContext(), R.drawable.bg_mic_button_recording));
        binding.ivMicIcon.setImageResource(R.drawable.ic_mic);
        binding.tvMicLabel.setText("Tap to stop");
        binding.waveformContainer.setVisibility(View.VISIBLE);
        startPulseAnimation();
    }

    private void setMicIdle() {
        binding.btnMic.setBackground(ContextCompat.getDrawable(
                requireContext(), R.drawable.bg_ripple_mic));
        binding.ivMicIcon.setImageResource(R.drawable.ic_mic);
        binding.tvMicLabel.setText("Tap to speak");
        binding.waveformContainer.setVisibility(View.INVISIBLE);
        stopPulseAnimation();
    }

    // ─── Pulse Ring Animation ─────────────────────────────────────

    private void startPulseAnimation() {
        animatePulseRing(binding.pulseRing1, 0, 1200);
        animatePulseRing(binding.pulseRing2, 400, 1200);
    }

    private void animatePulseRing(View ring, long startDelay, long duration) {
        ring.setAlpha(0.6f);
        ring.setScaleX(0.6f);
        ring.setScaleY(0.6f);
        ring.animate()
                .alpha(0f)
                .scaleX(1.4f)
                .scaleY(1.4f)
                .setStartDelay(startDelay)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    if (speechManager != null && speechManager.isListening()) {
                        animatePulseRing(ring, 0, duration);
                    }
                })
                .start();
    }

    private void stopPulseAnimation() {
        binding.pulseRing1.animate().cancel();
        binding.pulseRing2.animate().cancel();
        binding.pulseRing1.setAlpha(0f);
        binding.pulseRing2.setAlpha(0f);
    }

    // ─── Waveform Animation ───────────────────────────────────────

    private void startWaveformAnimation() {
        View[] bars = {
                binding.bar1, binding.bar2, binding.bar3, binding.bar4,
                binding.bar5, binding.bar6, binding.bar7, binding.bar8
        };
        Random random = new Random();

        waveformRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                for (View bar : bars) {
                    float scale = 0.3f + random.nextFloat() * 0.7f;
                    bar.animate()
                            .scaleY(scale)
                            .setDuration(120)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                }
                waveformHandler.postDelayed(this, 130);
            }
        };
        waveformHandler.post(waveformRunnable);
    }

    private void stopWaveformAnimation() {
        if (waveformRunnable != null) {
            waveformHandler.removeCallbacks(waveformRunnable);
            waveformRunnable = null;
        }
        // Reset bars to normal height
        View[] bars = {
                binding.bar1, binding.bar2, binding.bar3, binding.bar4,
                binding.bar5, binding.bar6, binding.bar7, binding.bar8
        };
        for (View bar : bars) {
            bar.animate().scaleY(1f).setDuration(200).start();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void hideResultCards() {
        binding.cardSpoken.setVisibility(View.GONE);
        binding.cardResult.setVisibility(View.GONE);
        lastTranslation = "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopWaveformAnimation();
        if (speechManager != null) speechManager.destroy();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}
