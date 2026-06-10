package com.aitranslator.app.ui.grammar;

import android.content.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentGrammarCheckerBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class GrammarCheckerFragment extends Fragment {

    private FragmentGrammarCheckerBinding binding;
    private GrammarCheckerViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGrammarCheckerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GrammarCheckerViewModel.class);

        PrefsManager prefs = PrefsManager.getInstance(requireContext());
        binding.tvTargetLangHint.setText("Checking " + prefs.getTargetLanguage() + " grammar");

        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        AnimUtils.fadeInUp(binding.cardInput, 100);
        AnimUtils.fadeInUp(binding.btnCheck, 200);

        // Character counter
        binding.etGrammarInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                binding.tvCharCount.setText(s.length() + " / 500 characters");
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnCheck.setEnabled(!loading);
            binding.btnCheck.setText(loading ? "Analysing…" : "✅  Check Grammar");
            if (loading) binding.cardResults.setVisibility(View.GONE);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            binding.cardResults.setVisibility(View.VISIBLE);

            binding.tvCorrectedText.setText(result.correctedText);
            binding.tvErrorsList.setText(result.errorsExplained.isEmpty()
                ? "No errors found! 🎉" : result.errorsExplained);
            binding.tvGrammarScore.setText(result.overallScore);
            binding.tvGrammarTips.setText(result.tips);

            // Score badge colour
            String scoreText = result.overallScore.toLowerCase();
            int color;
            if (scoreText.contains("10/10") || scoreText.contains("9/10")) color = 0xFF2ECC71;
            else if (scoreText.contains("8/10") || scoreText.contains("7/10")) color = 0xFF3D52A0;
            else if (scoreText.contains("6/10") || scoreText.contains("5/10")) color = 0xFFFFB347;
            else color = 0xFFFF6B6B;
            binding.tvGrammarScore.setTextColor(color);

            AnimUtils.fadeInUp(binding.cardResults, 0);
            AnimUtils.slideInFromLeft(binding.cardCorrected, 100);
            AnimUtils.slideInFromRight(binding.cardErrors, 200);
            AnimUtils.fadeInUp(binding.cardTips, 300);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });

        binding.btnCheck.setOnClickListener(v -> {
            String text = binding.etGrammarInput.getText().toString().trim();
            AnimUtils.pulse(binding.btnCheck);
            binding.btnCheck.postDelayed(() -> viewModel.checkGrammar(text), 150);
        });

        binding.btnClearInput.setOnClickListener(v -> {
            binding.etGrammarInput.setText("");
            binding.cardResults.setVisibility(View.GONE);
        });

        binding.btnCopyCorrected.setOnClickListener(v -> {
            String corrected = binding.tvCorrectedText.getText().toString();
            if (!corrected.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Corrected text", corrected));
                Toast.makeText(requireContext(), "Corrected text copied", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnUseAsChatInput.setOnClickListener(v -> {
            // Convenience: copies corrected text so user can paste it into Chat
            String corrected = binding.tvCorrectedText.getText().toString();
            if (!corrected.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Grammar corrected", corrected));
                Toast.makeText(requireContext(), "Copied — go to Chat and paste to practise!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
