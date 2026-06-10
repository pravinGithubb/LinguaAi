package com.aitranslator.app.ui.speakingquiz;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.QuizResult;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentSpeakingQuizBinding;
import com.aitranslator.app.service.SpeechRecognitionManager;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

import java.util.Collections;
import java.util.Locale;

/**
 * The actual Speaking Quiz screen.
 *
 * Flow per sentence:
 *   1. TTS reads the sentence at the user's chosen speed (Normal/Slow/Very Slow).
 *   2. User taps the mic, speaks, STT returns text.
 *   3. {@link PronunciationLocalScorer} scores the recognised text against
 *      the expected sentence (Levenshtein, 0–100).
 *   4. Score circle appears; user can Try Again or Next.
 *   5. Timer (60s) auto-advances if the user dawdles.
 *
 * Session-level:
 *   - On the LAST sentence (or when the user backs out), we save a single
 *     {@link QuizResult} row aggregating all attempts and award XP for
 *     attempts scoring >=60.
 *   - We reuse the existing QuizResult entity rather than inventing a new
 *     table, so this feature requires zero Room migration.
 *
 * Lifecycle care:
 *   - SpeechRecognitionManager and TtsManager are created in onViewCreated
 *     and torn down in onDestroyView (matches the rest of the project).
 *   - The CountDownTimer is cancelled aggressively to avoid leaking the
 *     fragment.
 */
public class SpeakingQuizFragment extends Fragment {

    /** How many sentences make up one session. Capped at the corpus size. */
    private static final int SESSION_SENTENCES = 10;

    /** Per-sentence time budget in milliseconds. */
    private static final long PER_SENTENCE_MILLIS = 60_000L;

    /** XP awarded per sentence the user scores >=60% on. Mirrors the AI Quiz reward. */
    private static final int XP_PER_GOOD_ATTEMPT = 10;

    /** Score threshold (inclusive) for an attempt to count as "passed". */
    private static final int PASS_THRESHOLD = 60;

    private FragmentSpeakingQuizBinding binding;
    private SpeechRecognitionManager speech;
    private TtsManager tts;
    private PrefsManager prefs;
    private AppRepository repo;

    private SpeakingQuizSentences.Level level;
    private String[] sessionSentences;     // size = min(SESSION_SENTENCES, corpus.length)
    private int currentIndex = 0;
    private int passedCount = 0;
    private boolean resultSaved = false;

    private boolean isListening = false;
    private CountDownTimer timer;

    private final ActivityResultLauncher<String> micPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    Toast.makeText(requireContext(),
                            "Microphone permission is required for the Speaking Quiz",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpeakingQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(requireContext());
        // The project instantiates AppRepository directly per the existing
        // convention (no singleton, the repo is cheap to construct).
        repo  = new AppRepository(requireActivity().getApplication());
        speech = new SpeechRecognitionManager(requireContext());
        // TtsManager initialises asynchronously; we don't block on it.
        tts = new TtsManager(requireContext(), null);

        // Resolve the chosen level from args (defensive default to BEGINNER).
        Bundle args = getArguments();
        String levelName = (args != null) ? args.getString(SpeakingQuizLevelFragment.ARG_LEVEL) : null;
        try {
            level = (levelName != null)
                    ? SpeakingQuizSentences.Level.valueOf(levelName)
                    : SpeakingQuizSentences.Level.BEGINNER;
        } catch (IllegalArgumentException e) {
            level = SpeakingQuizSentences.Level.BEGINNER;
        }

        // Build a randomised session subset matching the user's target language.
        // Falls back to English for any language without a bundled corpus.
        String[] full = SpeakingQuizSentences.forLevel(level, prefs.getTargetLanguageCode());
        int n = Math.min(SESSION_SENTENCES, full.length);
        sessionSentences = pickRandom(full, n);

        // Wire up controls.
        binding.progressBar.setMax(sessionSentences.length);

        binding.chipNormal.setOnClickListener(v   -> setSpeed(1.0f));
        binding.chipSlow.setOnClickListener(v     -> setSpeed(0.75f));
        binding.chipVerySlow.setOnClickListener(v -> setSpeed(0.55f));

        binding.btnSpeak.setOnClickListener(v -> {
            if (isListening) {
                speech.stopListening();
                stopListeningUI();
            } else {
                requestMicAndListen();
            }
        });

        // 🔊 Listen — manual TTS playback. Plays the expected sentence
        // in the user's target language. Decoupled from sentence-load
        // and from the mic button so the rating flow stays unambiguous.
        binding.btnListen.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnListen);
            speakCurrent();
        });

        binding.btnNext.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnNext);
            advance();
        });

        binding.btnTryAgain.setOnClickListener(v -> {
            // Re-attempt the same sentence — hide the score, reset timer.
            binding.cardScore.setVisibility(View.GONE);
            startTimer();
            requestMicAndListen();
        });

        // First sentence
        showCurrentSentence(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Session bookkeeping
    // ─────────────────────────────────────────────────────────────────────────

    private void showCurrentSentence(boolean firstTime) {
        binding.tvSentence.setText(sessionSentences[currentIndex]);
        binding.tvSentence.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary));
        binding.tvProgressCount.setText(
                String.format(Locale.US, "%d/%d", currentIndex + 1, sessionSentences.length));
        binding.progressBar.setProgress(currentIndex + 1);
        binding.cardScore.setVisibility(View.GONE);
        binding.tvSpoken.setText("");

        if (firstTime) {
            AnimUtils.slideInFromLeft(binding.cardSentence, 100);
        } else {
            AnimUtils.fadeInUp(binding.cardSentence, 0);
        }

        // The TTS used to play here automatically, but users perceived
        // that as the app "repeating their words" rather than rating their
        // pronunciation. The user now plays the model pronunciation
        // manually via the 🔊 Listen button. This makes the rating flow
        // unambiguous: tap Listen → hear, tap Speak → be rated.
        startTimer();
    }

    private void advance() {
        // Already on the last sentence? Finish the session.
        if (currentIndex >= sessionSentences.length - 1) {
            finishSession();
            return;
        }
        currentIndex++;
        showCurrentSentence(false);
    }

    /** Persists the session result + XP. Idempotent — safe to call twice. */
    private void finishSession() {
        if (resultSaved) return;
        resultSaved = true;
        cancelTimer();

        int xp = passedCount * XP_PER_GOOD_ATTEMPT;
        prefs.addXp(xp);
        prefs.incrementSessions();

        QuizResult row = new QuizResult(
                System.currentTimeMillis(),
                sessionSentences.length,
                passedCount,
                xp,
                prefs.getTargetLanguage());
        repo.saveQuizResult(row);

        Toast.makeText(requireContext(),
                "Session complete! " + passedCount + "/" + sessionSentences.length
                        + " passed   •   +" + xp + " XP",
                Toast.LENGTH_LONG).show();

        // Pop back to the level picker so the user can pick another round.
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                if (getView() != null) {
                    androidx.navigation.Navigation.findNavController(getView()).navigateUp();
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Speed chips + TTS
    // ─────────────────────────────────────────────────────────────────────────

    private void setSpeed(float rate) {
        // Visually highlight the selected chip. The 'rate' parameter is
        // currently informational only — TtsManager doesn't yet expose
        // setSpeechRate. The user re-plays the sentence at the chosen
        // (visual) speed by tapping the 🔊 Listen button afterward.
        applyChipState(binding.chipNormal,    rate == 1.0f);
        applyChipState(binding.chipSlow,      rate == 0.75f);
        applyChipState(binding.chipVerySlow,  rate == 0.55f);
    }

    private void applyChipState(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_primary);
            chip.setTextColor(0xFFFFFFFF);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_primary_outline);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }
    }

    private void speakCurrent() {
        if (tts == null || !tts.isReady()) {
            // TTS may not be initialised yet on the very first call — the
            // user can tap a speed chip again to re-trigger once it warms up.
            return;
        }
        // Note: the existing TtsManager doesn't expose setSpeechRate, so the
        // speed chips currently affect the *visual* selection only. A future
        // TtsManager.speak(text, lang, rate) overload would let us honour
        // the chosen speed. Keeping the chips wired up so the API is ready.
        tts.speak(sessionSentences[currentIndex], prefs.getTargetLanguageCode());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Mic + STT
    // ─────────────────────────────────────────────────────────────────────────

    private void requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        isListening = true;
        binding.btnSpeak.setBackgroundTintList(ColorStateList.valueOf(0xFFFF6B6B));
        binding.tvSpeakLabel.setText("Listening…");
        AnimUtils.pulse(binding.btnSpeak);

        speech.startListening(prefs.getTargetLanguageCode(),
                new SpeechRecognitionManager.SpeechCallback() {
                    @Override public void onReadyForSpeech() { /* no-op */ }
                    @Override public void onSpeechDetected() { /* no-op */ }
                    @Override public void onPartialResult(String partial) {
                        // Could show partial text — kept quiet for a cleaner UX.
                    }
                    @Override public void onResult(String text) {
                        if (!isAdded() || binding == null) return;
                        requireActivity().runOnUiThread(() -> {
                            stopListeningUI();
                            handleSpokenResult(text);
                        });
                    }
                    @Override public void onError(String message) {
                        if (!isAdded() || binding == null) return;
                        requireActivity().runOnUiThread(() -> {
                            stopListeningUI();
                            // Show the score card with a 0% / Try Again
                            // state so the user sees clear feedback rather
                            // than nothing happening. Without this, an STT
                            // failure (very common — "no match", "audio
                            // timeout", "no permission", etc.) would leave
                            // the screen looking frozen because the score
                            // card stays hidden and the "couldn't hear you"
                            // text lives INSIDE that hidden card.
                            binding.tvSentence.setTextColor(0xFFFF6B6B);
                            binding.tvScorePercent.setText("0%");
                            binding.tvScoreGrade.setText("TRY AGAIN");
                            binding.scoreCircleBg.setBackgroundTintList(
                                    ColorStateList.valueOf(0xFFFF6B6B));
                            binding.tvSpoken.setText(
                                    "Couldn't hear you — " + message
                                    + ". Tap the mic to try again.");
                            binding.cardScore.setVisibility(View.VISIBLE);
                            AnimUtils.fadeInScale(binding.cardScore, 0);
                        });
                    }
                    @Override public void onEndOfSpeech() {
                        if (!isAdded() || binding == null) return;
                        requireActivity().runOnUiThread(this::stopUiOnly);
                    }
                    private void stopUiOnly() { stopListeningUI(); }
                });
    }

    private void stopListeningUI() {
        isListening = false;
        if (binding == null) return;
        binding.btnSpeak.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
        binding.tvSpeakLabel.setText("Speak");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Scoring + result rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSpokenResult(String spoken) {
        cancelTimer();

        PronunciationLocalScorer.Score s =
                PronunciationLocalScorer.score(sessionSentences[currentIndex], spoken);

        // Update sentence colouring: green if passed, red if not.
        binding.tvSentence.setTextColor(
                s.percent >= PASS_THRESHOLD ? 0xFF2ECC71 : 0xFFFF6B6B);

        // Score circle
        binding.tvScorePercent.setText(s.percent + "%");
        binding.tvScoreGrade.setText(s.grade.toUpperCase(Locale.US));
        binding.scoreCircleBg.setBackgroundTintList(ColorStateList.valueOf(s.colorArgb));
        binding.tvSpoken.setText("You said: \"" + spoken + "\"");

        binding.cardScore.setVisibility(View.VISIBLE);
        AnimUtils.fadeInScale(binding.cardScore, 0);

        // Track for the session aggregate.
        if (s.percent >= PASS_THRESHOLD) {
            passedCount++;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Timer
    // ─────────────────────────────────────────────────────────────────────────

    private void startTimer() {
        cancelTimer();
        timer = new CountDownTimer(PER_SENTENCE_MILLIS, 1_000L) {
            @Override public void onTick(long ms) {
                if (binding == null) return;
                long total = ms / 1_000L;
                long mm = total / 60;
                long ss = total % 60;
                binding.tvTimer.setText(String.format(Locale.US, "%02d:%02d", mm, ss));
            }
            @Override public void onFinish() {
                if (binding == null) return;
                binding.tvTimer.setText("00:00");
                // Auto-advance when time runs out.
                advance();
            }
        }.start();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a new array containing `n` random elements from `source`. */
    private static String[] pickRandom(String[] source, int n) {
        java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(source));
        Collections.shuffle(list);
        return list.subList(0, Math.min(n, list.size())).toArray(new String[0]);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onPause() {
        super.onPause();
        cancelTimer();
        if (speech != null) speech.stopListening();
        if (tts != null) tts.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Persist whatever we have if the user backs out mid-session.
        if (!resultSaved && currentIndex > 0) {
            // Save partial progress so XP isn't lost.
            int xp = passedCount * XP_PER_GOOD_ATTEMPT;
            if (xp > 0) {
                prefs.addXp(xp);
                prefs.incrementSessions();
                repo.saveQuizResult(new QuizResult(
                        System.currentTimeMillis(),
                        currentIndex + 1,
                        passedCount,
                        xp,
                        prefs.getTargetLanguage()));
            }
            resultSaved = true;
        }
        cancelTimer();
        if (speech != null) { speech.destroy(); speech = null; }
        if (tts != null)    { tts.shutdown();   tts = null;    }
        binding = null;
    }
}
