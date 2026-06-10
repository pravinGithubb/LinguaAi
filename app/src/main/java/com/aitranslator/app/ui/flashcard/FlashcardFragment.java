package com.aitranslator.app.ui.flashcard;
import android.animation.*;
import android.os.Bundle;
import android.view.*;
import android.view.animation.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.R;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.databinding.FragmentFlashcardBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;

public class FlashcardFragment extends Fragment {
    private FragmentFlashcardBinding binding;
    private FlashcardViewModel viewModel;
    private TtsManager tts;
    private boolean isShowingFront = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FlashcardViewModel.class);

        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {}
            @Override public void onError(String msg) {}
        });

        // Entrance animations
        AnimUtils.fadeInUp(binding.progressRow, 0);
        AnimUtils.fadeInScale(binding.cardContainer, 200);
        AnimUtils.fadeInUp(binding.btnRow, 400);

        // Tap card to flip
        binding.cardContainer.setOnClickListener(v -> flipCard());

        binding.btnKnown.setOnClickListener(v -> {
            AnimUtils.slideInFromRight(binding.cardContainer, 0);
            binding.cardContainer.postDelayed(() -> {
                viewModel.markKnown();
                isShowingFront = true;
            }, 200);
        });

        binding.btnUnknown.setOnClickListener(v -> {
            AnimUtils.slideInFromLeft(binding.cardContainer, 0);
            binding.cardContainer.postDelayed(() -> {
                viewModel.markUnknown();
                isShowingFront = true;
            }, 200);
        });

        binding.btnSpeak.setOnClickListener(v -> {
            VocabularyWord word = viewModel.getCurrentCard().getValue();
            if (word != null && tts != null) tts.speak(word.word, word.language);
            AnimUtils.pulse(binding.btnSpeak);
        });

        // Observe
        viewModel.getCurrentCard().observe(getViewLifecycleOwner(), this::displayCard);

        viewModel.getIsDeckEmpty().observe(getViewLifecycleOwner(), empty -> {
            if (empty) showSessionComplete();
        });

        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            binding.tvProgress.setText(stats.remaining + " cards left");
            binding.progressBar.setMax(stats.total);
            binding.progressBar.setProgress(stats.known + stats.unknown);
            binding.tvKnown.setText("✅ " + stats.known);
            binding.tvUnknown.setText("❌ " + stats.unknown);
        });

        viewModel.loadDeck();
    }

    private void displayCard(VocabularyWord word) {
        if (word == null) return;
        isShowingFront = true;
        binding.tvCardFront.setText(word.word);
        binding.tvPhonetic.setText(word.phonetic);
        binding.tvCardBack.setText(word.definition);
        binding.tvExample.setText(word.exampleSentence.isEmpty() ? "" : "\"" + word.exampleSentence + "\"");
        binding.tvPartOfSpeech.setText(word.partOfSpeech);
        // Show front
        binding.frontSide.setVisibility(View.VISIBLE);
        binding.backSide.setVisibility(View.GONE);
        binding.tvTapHint.setText("Tap card to see definition");
        binding.tvTapHint.setVisibility(View.VISIBLE);
    }

    private void flipCard() {
        float scale = requireContext().getResources().getDisplayMetrics().density;
        binding.cardContainer.setCameraDistance(8000 * scale);

        if (isShowingFront) {
            // Flip to back
            ObjectAnimator flipOut = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", 0f, 90f);
            flipOut.setDuration(200);
            flipOut.setInterpolator(new AccelerateInterpolator());
            flipOut.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    binding.frontSide.setVisibility(View.GONE);
                    binding.backSide.setVisibility(View.VISIBLE);
                    binding.tvTapHint.setVisibility(View.GONE);
                    ObjectAnimator flipIn = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", -90f, 0f);
                    flipIn.setDuration(200);
                    flipIn.setInterpolator(new DecelerateInterpolator());
                    flipIn.start();
                }
            });
            flipOut.start();
            isShowingFront = false;
        } else {
            // Flip back to front
            ObjectAnimator flipOut = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", 0f, 90f);
            flipOut.setDuration(200);
            flipOut.setInterpolator(new AccelerateInterpolator());
            flipOut.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    binding.frontSide.setVisibility(View.VISIBLE);
                    binding.backSide.setVisibility(View.GONE);
                    binding.tvTapHint.setVisibility(View.VISIBLE);
                    ObjectAnimator flipIn = ObjectAnimator.ofFloat(binding.cardContainer, "rotationY", -90f, 0f);
                    flipIn.setDuration(200);
                    flipIn.setInterpolator(new DecelerateInterpolator());
                    flipIn.start();
                }
            });
            flipOut.start();
            isShowingFront = true;
        }
    }

    private void showSessionComplete() {
        binding.flashcardContent.setVisibility(View.GONE);
        binding.sessionCompleteLayout.setVisibility(View.VISIBLE);
        AnimUtils.fadeInScale(binding.sessionCompleteLayout, 0);
        binding.tvFinalKnown.setText(String.valueOf(viewModel.getKnownCount()));
        binding.tvFinalUnknown.setText(String.valueOf(viewModel.getUnknownCount()));
        binding.tvFinalTotal.setText(viewModel.getDeckSize() + " cards reviewed");
        binding.btnStudyAgain.setOnClickListener(v -> {
            binding.flashcardContent.setVisibility(View.VISIBLE);
            binding.sessionCompleteLayout.setVisibility(View.GONE);
            viewModel.loadDeck();
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}