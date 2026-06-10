package com.aitranslator.app.ui.dictionary;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.BottomSheetDictionaryBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class DictionaryBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetDictionaryBinding binding;
    private DictionaryViewModel viewModel;
    private TtsManager tts;
    private String word, languageCode, nativeLanguage;

    public static final String TAG = "DictionaryBottomSheet";

    public static DictionaryBottomSheet newInstance(String word, String languageCode, String nativeLanguage) {
        DictionaryBottomSheet sheet = new DictionaryBottomSheet();
        Bundle args = new Bundle();
        args.putString("word", word); args.putString("code", languageCode);
        args.putString("native", nativeLanguage);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BottomSheetDictionaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            word = getArguments().getString("word", "");
            languageCode = getArguments().getString("code", "en");
            nativeLanguage = getArguments().getString("native", "English");
        }

        viewModel = new ViewModelProvider(this).get(DictionaryViewModel.class);
        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {}
            @Override public void onError(String msg) {}
        });

        binding.tvSearchWord.setText(word);
        binding.progressBar.setVisibility(View.VISIBLE);
        viewModel.lookup(word, languageCode);

        viewModel.getResult().observe(getViewLifecycleOwner(), r -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.contentGroup.setVisibility(View.VISIBLE);
            AnimUtils.fadeInUp(binding.contentGroup, 0);

            binding.tvWord.setText(r.word);
            binding.tvPhonetic.setText(r.getPhonetic());
            binding.tvPartOfSpeech.setText(r.getPartOfSpeech());
            binding.tvDefinition.setText(r.getFirstDefinition());

            String example = r.getFirstExample();
            if (!example.isEmpty()) {
                binding.tvExample.setText("\"" + example + "\"");
                binding.tvExample.setVisibility(View.VISIBLE);
                binding.labelExample.setVisibility(View.VISIBLE);
            }

            binding.btnListen.setOnClickListener(v -> {
                tts.speak(r.word, languageCode);
                AnimUtils.pulse(binding.btnListen);
            });

            binding.btnSave.setOnClickListener(v -> {
                AnimUtils.pulse(binding.btnSave);
                viewModel.saveToVocabulary(r, languageCode, "");
            });
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvDefinition.setText("Could not find definition for \"" + word + "\"");
                binding.contentGroup.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}