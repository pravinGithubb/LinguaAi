package com.aitranslator.app.ui.lesson;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentDailyLessonBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class DailyLessonFragment extends Fragment {

    private FragmentDailyLessonBinding binding;
    private DailyLessonViewModel viewModel;

    private static final String[] TOPICS = {
        "Greetings & Introductions",
        "Numbers & Counting",
        "Food & Restaurants",
        "Travel & Directions",
        "Shopping & Money",
        "Family & Relationships",
        "Time & Dates",
        "Weather & Seasons",
        "Work & Professions",
        "Health & Body",
        "Hobbies & Free Time",
        "At the Hotel",
        "Emergency Phrases",
        "Emotions & Feelings",
        "Nature & Environment"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDailyLessonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DailyLessonViewModel.class);

        setupTopicSpinner();

        AnimUtils.slideInFromLeft(binding.cardTopicPicker, 100);
        AnimUtils.fadeInUp(binding.btnGenerateLesson, 200);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnGenerateLesson.setEnabled(!loading);
            binding.tvLoadingMsg.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) {
                binding.cardLessonContent.setVisibility(View.GONE);
            }
        });

        viewModel.getLesson().observe(getViewLifecycleOwner(), lesson -> {
            if (lesson == null) return;
            binding.cardLessonContent.setVisibility(View.VISIBLE);
            binding.tvLessonTitle.setText(lesson.title.isEmpty()
                ? binding.spinnerTopic.getSelectedItem().toString() : lesson.title);
            binding.tvLessonIntro.setText(lesson.intro);
            binding.tvVocabSection.setText(lesson.vocabularySection);
            binding.tvGrammarSection.setText(lesson.grammarSection);
            binding.tvPracticeSection.setText(lesson.practiceSection);
            binding.tvLessonTip.setText("💡 " + lesson.tip);

            AnimUtils.fadeInUp(binding.cardLessonContent, 0);
            AnimUtils.slideInFromLeft(binding.cardVocab, 100);
            AnimUtils.slideInFromRight(binding.cardGrammar, 200);
            AnimUtils.slideInFromLeft(binding.cardPractice, 300);
            AnimUtils.fadeInUp(binding.cardTip, 400);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                binding.tvLoadingMsg.setText(err);
                binding.tvLoadingMsg.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        binding.btnGenerateLesson.setOnClickListener(v -> {
            String topic = binding.spinnerTopic.getSelectedItem().toString();
            AnimUtils.pulse(binding.btnGenerateLesson);
            binding.btnGenerateLesson.postDelayed(() -> viewModel.generateLesson(topic), 150);
        });
    }

    private void setupTopicSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            TOPICS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTopic.setAdapter(adapter);

        // Pick a topic based on day of week for a "daily" feel
        int dayOfWeek = new java.util.GregorianCalendar().get(java.util.Calendar.DAY_OF_WEEK);
        binding.spinnerTopic.setSelection(dayOfWeek % TOPICS.length);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
