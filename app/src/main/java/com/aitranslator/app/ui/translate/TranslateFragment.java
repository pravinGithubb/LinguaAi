package com.aitranslator.app.ui.translate;
import android.content.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aitranslator.app.R;
import com.aitranslator.app.databinding.FragmentTranslateBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

public class TranslateFragment extends Fragment {
    private FragmentTranslateBinding binding;
    private TranslateViewModel viewModel;
    private TtsManager tts;
    private PrefsManager prefs;
    private String[] languageNames;
    private String fromLang, fromCode, toLang, toCode;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTranslateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(TranslateViewModel.class);
        languageNames = LanguageUtils.getLanguageNames();

        fromLang = prefs.getNativeLanguage(); fromCode = prefs.getNativeLanguageCode();
        toLang = prefs.getTargetLanguage(); toCode = prefs.getTargetLanguageCode();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, languageNames);
        binding.spinnerFrom.setAdapter(adapter);
        binding.spinnerTo.setAdapter(adapter);
        binding.spinnerFrom.setText(fromLang, false);
        binding.spinnerTo.setText(toLang, false);

        binding.spinnerFrom.setOnItemClickListener((p, v, pos, id) -> { fromLang = languageNames[pos]; fromCode = LanguageUtils.getCode(fromLang); });
        binding.spinnerTo.setOnItemClickListener((p, v, pos, id) -> { toLang = languageNames[pos]; toCode = LanguageUtils.getCode(toLang); });

        binding.btnSwap.setOnClickListener(v -> {
            AnimUtils.pulse(binding.btnSwap);
            String tn = fromLang, tc = fromCode; fromLang = toLang; fromCode = toCode; toLang = tn; toCode = tc;
            binding.spinnerFrom.setText(fromLang, false); binding.spinnerTo.setText(toLang, false);
            String inp = binding.etInput.getText() != null ? binding.etInput.getText().toString() : "";
            String out = binding.tvOutput.getText().toString();
            if (!out.isEmpty()) { binding.etInput.setText(out); binding.tvOutput.setText(inp); }
        });

        binding.btnTranslate.setOnClickListener(v -> {
            String input = binding.etInput.getText() != null ? binding.etInput.getText().toString().trim() : "";
            if (input.isEmpty()) { Toast.makeText(requireContext(), "Enter text to translate", Toast.LENGTH_SHORT).show(); return; }
            AnimUtils.pulse(binding.btnTranslate);
            viewModel.translate(input, fromLang, fromCode, toLang, toCode);
        });

        // Show/hide clear button as user types
        binding.etInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnClear.setOnClickListener(v -> { binding.etInput.setText(""); binding.outputCard.setVisibility(View.GONE); });

        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {} @Override public void onError(String msg) {}
        });

        binding.btnSpeakInput.setOnClickListener(v -> {
            String t = binding.etInput.getText() != null ? binding.etInput.getText().toString().trim() : "";
            if (!t.isEmpty() && tts != null) tts.speak(t, fromCode);
        });
        binding.btnSpeakOutput.setOnClickListener(v -> { if (tts != null) tts.speak(binding.tvOutput.getText().toString(), toCode); });
        binding.btnCopy.setOnClickListener(v -> {
            ClipboardManager cb = (ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("translation", binding.tvOutput.getText()));
            Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show();
            AnimUtils.pulse(binding.btnCopy);
            binding.tvSavedLabel.setVisibility(View.VISIBLE);
            binding.tvSavedLabel.postDelayed(() -> { if (binding != null) binding.tvSavedLabel.setVisibility(View.GONE); }, 2000);
        });

        viewModel.getTranslatedText().observe(getViewLifecycleOwner(), result -> {
            binding.tvOutput.setText(result);
            binding.outputCard.setVisibility(View.VISIBLE);
            AnimUtils.popIn(binding.outputCard);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnTranslate.setEnabled(!loading);
            binding.btnTranslate.setText(loading ? "Translating…" : "Translate");
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });

        HistoryAdapter historyAdapter = new HistoryAdapter(
                item -> viewModel.toggleFavorite(item),
                item -> viewModel.deleteHistory(item.id));
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(historyAdapter);
        viewModel.getHistory().observe(getViewLifecycleOwner(), historyAdapter::submitList);

        binding.btnExportHistory.setOnClickListener(v ->
            androidx.navigation.Navigation.findNavController(v)
                .navigate(R.id.action_translate_to_history_export));
    }

    @Override public void onDestroyView() { super.onDestroyView(); if (tts != null) tts.shutdown(); binding = null; }
}