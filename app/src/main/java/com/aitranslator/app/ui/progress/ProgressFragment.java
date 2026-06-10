package com.aitranslator.app.ui.progress;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.databinding.FragmentProgressBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

public class ProgressFragment extends Fragment {
    private FragmentProgressBinding binding;
    private PrefsManager prefs;
    private AppRepository repository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());
        repository = new AppRepository(requireActivity().getApplication());

        int streak = prefs.getStreak();
        int sessions = prefs.getTotalSessions();

        binding.tvStreakValue.setText(streak + " days");
        binding.tvSessionsValue.setText(String.valueOf(sessions));
        binding.tvMotivation.setText(getMotivation(streak));
        binding.tvNativeLang.setText(prefs.getNativeLanguage());
        binding.tvTargetLang.setText(prefs.getTargetLanguage());
        binding.tvNativeFlag.setText(LanguageUtils.getFlagEmoji(prefs.getNativeLanguageCode()));
        binding.tvTargetFlag.setText(LanguageUtils.getFlagEmoji(prefs.getTargetLanguageCode()));
        binding.tvTip.setText(getDailyTip(sessions));

        // FIX 3: Live vocabulary stats from Room DB
        repository.getAllVocabulary().observe(getViewLifecycleOwner(), words -> {
            int total = words.size();
            long mastered = words.stream().filter(w -> w.masteryLevel >= 3).count();
            binding.tvVocabTotal.setText(String.valueOf(total));
            binding.tvVocabMastered.setText(mastered + " mastered");
        });

        // Animate cards in
        AnimUtils.fadeInUp(binding.tvMotivation, 0);
        AnimUtils.slideInFromLeft(binding.tvStreakValue, 200);
        AnimUtils.slideInFromRight(binding.tvSessionsValue, 200);
        AnimUtils.fadeInUp(binding.cardVocabStats, 300);
        AnimUtils.fadeInUp(view.findViewById(com.aitranslator.app.R.id.tv_native_lang), 400);
    }

    private String getMotivation(int streak) {
        if (streak == 0) return "Start your first session today! 🚀";
        if (streak < 3) return "Great start! Keep it up! 💪";
        if (streak < 7) return "You're on a roll — " + streak + " days strong! 🔥";
        if (streak < 30) return "Amazing! " + streak + "-day streak — you're dedicated! ⭐";
        return "Legendary! " + streak + " days — you're a polyglot! 🏆";
    }

    private String getDailyTip(int sessions) {
        String[] tips = {
            "Practice for 10 minutes daily to build lasting fluency!",
            "Try thinking in your target language for 5 minutes today.",
            "Watch a short video in " + prefs.getTargetLanguage() + " for listening practice.",
            "Reading out loud improves both pronunciation and retention.",
            "Use new vocabulary in a sentence to remember it better!"
        };
        return tips[sessions % tips.length];
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
