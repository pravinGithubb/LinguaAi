package com.aitranslator.app.ui.quiz;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentQuizBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class QuizFragment extends Fragment {

    private FragmentQuizBinding binding;
    private QuizViewModel viewModel;
    private PrefsManager prefs;
    private boolean answerLocked = false;
    private static final int QUESTION_COUNT = 10;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        prefs = PrefsManager.getInstance(requireContext());

        showStartScreen();

        // Observers
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.tvLoadingMsg.setVisibility(View.VISIBLE);
                binding.tvLoadingMsg.setText("Generating quiz with AI… 🤖");
                binding.layoutQuestion.setVisibility(View.GONE);
                binding.layoutStart.setVisibility(View.GONE);
                binding.layoutResult.setVisibility(View.GONE);
            }
        });

        viewModel.getCurrentQuestion().observe(getViewLifecycleOwner(), question -> {
            if (question == null) return;
            binding.tvLoadingMsg.setVisibility(View.GONE);
            binding.layoutQuestion.setVisibility(View.VISIBLE);
            binding.layoutResult.setVisibility(View.GONE);
            renderQuestion(question);
        });

        viewModel.getQuizFinished().observe(getViewLifecycleOwner(), finished -> {
            if (Boolean.TRUE.equals(finished)) showResultScreen();
        });

        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.tvLoadingMsg.setVisibility(View.VISIBLE);
                binding.tvLoadingMsg.setText(msg);
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showStartScreen() {
        binding.layoutStart.setVisibility(View.VISIBLE);
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.tvLoadingMsg.setVisibility(View.GONE);

        binding.tvXpTotal.setText("Total XP: " + prefs.getTotalXp());
        binding.tvQuizLang.setText("Language: " + prefs.getTargetLanguage());

        AnimUtils.fadeInUp(binding.layoutStart, 100);
        AnimUtils.fadeInScale(binding.btnStartQuiz, 300);

        binding.btnStartQuiz.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnStartQuiz);
            binding.btnStartQuiz.postDelayed(() -> viewModel.startQuiz(QUESTION_COUNT), 150);
        });
    }

    private void renderQuestion(QuizViewModel.QuizQuestion q) {
        answerLocked = false;

        // Progress indicator
        int idx = viewModel.getCurrentIndex() + 1;
        int total = viewModel.getTotalQuestions();
        binding.tvQuestionProgress.setText("Question " + idx + " of " + total);
        binding.progressQuiz.setMax(total);
        binding.progressQuiz.setProgress(idx);

        // Word prompt
        binding.tvWordPrompt.setText(q.word);
        binding.tvExampleSentence.setText(q.exampleSentence.isEmpty() ? "" : "📝 " + q.exampleSentence);

        // Reset all option buttons
        MaterialButton[] btns = {
            binding.btnOption1, binding.btnOption2,
            binding.btnOption3, binding.btnOption4
        };
        for (int i = 0; i < btns.length && i < q.options.size(); i++) {
            btns[i].setText(q.options.get(i));
            btns[i].setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF3D52A0)); // primary
            btns[i].setEnabled(true);
        }

        binding.tvAnswerFeedback.setVisibility(View.GONE);
        binding.btnNextQuestion.setVisibility(View.GONE);

        AnimUtils.fadeInUp(binding.cardQuestion, 0);
        AnimUtils.fadeInScale(binding.cardOptions, 100);

        // Wire click listeners
        for (MaterialButton btn : btns) {
            btn.setOnClickListener(v -> {
                if (answerLocked) return;
                answerLocked = true;
                handleAnswer(btn, btns, q.correctAnswer);
            });
        }
    }

    private void handleAnswer(MaterialButton selected, MaterialButton[] all, String correct) {
        boolean isCorrect = viewModel.submitAnswer(selected.getText().toString());

        // Colour feedback
        for (MaterialButton btn : all) {
            btn.setEnabled(false);
            String text = btn.getText().toString();
            if (text.equals(correct)) {
                btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2ECC71)); // green
            } else if (btn == selected && !isCorrect) {
                btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF6B6B)); // red
            }
        }

        binding.tvAnswerFeedback.setVisibility(View.VISIBLE);
        if (isCorrect) {
            binding.tvAnswerFeedback.setText("✅  Correct! +10 XP");
            binding.tvAnswerFeedback.setTextColor(0xFF2ECC71);
        } else {
            binding.tvAnswerFeedback.setText("❌  Incorrect — the answer was: " + correct);
            binding.tvAnswerFeedback.setTextColor(0xFFFF6B6B);
        }
        AnimUtils.popIn(binding.tvAnswerFeedback);

        // Auto-advance or show Next button
        new Handler().postDelayed(() -> {
            binding.btnNextQuestion.setVisibility(View.VISIBLE);
            AnimUtils.popIn(binding.btnNextQuestion);
        }, 600);

        binding.btnNextQuestion.setOnClickListener(v -> viewModel.nextQuestion());
    }

    private void showResultScreen() {
        binding.layoutQuestion.setVisibility(View.GONE);
        binding.layoutStart.setVisibility(View.GONE);
        binding.layoutResult.setVisibility(View.VISIBLE);

        int correct = viewModel.getCorrectCount();
        int total = viewModel.getTotalQuestions();
        int xp = viewModel.calculateXp();
        int pct = total > 0 ? (correct * 100 / total) : 0;

        String grade;
        String emoji;
        if (pct >= 90) { grade = "Excellent!"; emoji = "🏆"; }
        else if (pct >= 70) { grade = "Well done!"; emoji = "⭐"; }
        else if (pct >= 50) { grade = "Good effort!"; emoji = "💪"; }
        else { grade = "Keep practising!"; emoji = "📚"; }

        binding.tvResultEmoji.setText(emoji);
        binding.tvResultGrade.setText(grade);
        binding.tvResultScore.setText(correct + " / " + total + " correct");
        binding.tvResultXp.setText("+" + xp + " XP earned");
        binding.tvTotalXp.setText("Total XP: " + prefs.getTotalXp());

        AnimUtils.fadeInUp(binding.layoutResult, 0);
        AnimUtils.fadeInScale(binding.tvResultEmoji, 200);

        binding.btnRetakeQuiz.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnRetakeQuiz);
            binding.btnRetakeQuiz.postDelayed(() -> {
                binding.layoutResult.setVisibility(View.GONE);
                viewModel.startQuiz(QUESTION_COUNT);
            }, 150);
        });

        binding.btnQuitQuiz.setOnClickListener(v -> showStartScreen());
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
