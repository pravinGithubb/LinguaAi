package com.aitranslator.app.ui.wordofday;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentWordOfDayBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

public class WordOfDayFragment extends Fragment {
    private FragmentWordOfDayBinding binding;
    private WordOfDayViewModel viewModel;
    private TtsManager tts;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWordOfDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WordOfDayViewModel.class);
        PrefsManager prefs = PrefsManager.getInstance(requireContext());

        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {}
            @Override public void onError(String msg) {}
        });

        AnimUtils.fadeInUp(binding.cardWord, 200);
        AnimUtils.fadeInUp(binding.cardExample, 350);
        AnimUtils.fadeInUp(binding.btnSave, 500);

        viewModel.getWordOfDay().observe(getViewLifecycleOwner(), word -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.tvWord.setText(word.word);
            binding.tvPhonetic.setText(word.phonetic);
            binding.tvPartOfSpeech.setText(word.partOfSpeech);
            binding.tvDefinition.setText(word.definition);
            binding.tvExample.setText(word.exampleSentence.isEmpty() ?
                    "No example available" : "\"" + word.exampleSentence + "\"");
            binding.tvTranslation.setText(word.translation);
            binding.cardWord.setVisibility(View.VISIBLE);
            binding.cardExample.setVisibility(View.VISIBLE);
            binding.btnSave.setVisibility(View.VISIBLE);
            AnimUtils.fadeInScale(binding.cardWord, 0);

            binding.btnListen.setOnClickListener(v -> {
                tts.speak(word.word, prefs.getTargetLanguageCode());
                AnimUtils.pulse(binding.btnListen);
            });

            binding.btnSave.setOnClickListener(v -> {
                AnimUtils.pulse(binding.btnSave);
                viewModel.saveToVocabulary(word);
            });
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });

        viewModel.loadWordOfDay(prefs.getTargetLanguage());
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}