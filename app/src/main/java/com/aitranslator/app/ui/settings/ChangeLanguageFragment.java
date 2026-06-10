package com.aitranslator.app.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.aitranslator.app.databinding.FragmentChangeLanguageBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

/**
 * Lets the user change their native + target (learning) language at any time
 * after onboarding. Reuses the same {@link AutoCompleteTextView} +
 * {@link ArrayAdapter} pattern as {@code OnboardingActivity} so the input
 * behaviour is identical.
 *
 * Pure presentation: no ViewModel, no DB, no network. Persistence happens
 * via {@link PrefsManager}, which all downstream consumers (Speaking Quiz,
 * Translate, Chat, etc.) already read on demand — so no app restart is
 * needed for the change to take effect.
 */
public class ChangeLanguageFragment extends Fragment {

    private FragmentChangeLanguageBinding binding;
    private PrefsManager prefs;

    /** Working copy — only committed to PrefsManager when the user taps Save. */
    private String selectedNative;
    private String selectedTarget;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangeLanguageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = PrefsManager.getInstance(requireContext());
        selectedNative = prefs.getNativeLanguage();
        selectedTarget = prefs.getTargetLanguage();

        // Staggered entrance — matches the rest of the app's vibe.
        AnimUtils.slideInFromLeft(binding.cardNative, 100);
        AnimUtils.slideInFromRight(binding.cardTarget, 200);
        AnimUtils.fadeInUp(binding.btnSave, 320);

        // Build the dropdown adapter.
        String[] languages = LanguageUtils.getLanguageNames();
        FlagLanguageAdapter adapter = new FlagLanguageAdapter(
                requireContext(),
                languages);

        AutoCompleteTextView nativeSpinner = binding.spinnerNative;
        AutoCompleteTextView targetSpinner = binding.spinnerTarget;

        nativeSpinner.setAdapter(adapter);
        targetSpinner.setAdapter(adapter);

        // Pre-fill with the current selection (without filtering the adapter).
        nativeSpinner.setText(selectedNative, false);
        targetSpinner.setText(selectedTarget, false);

        nativeSpinner.setOnItemClickListener((parent, v, pos, id) -> selectedNative = languages[pos]);
        targetSpinner.setOnItemClickListener((parent, v, pos, id) -> selectedTarget = languages[pos]);

        binding.btnSave.setOnClickListener(v -> save(v));
    }

    private void save(View source) {
        if (selectedNative == null || selectedTarget == null) {
            Toast.makeText(requireContext(),
                    "Please pick both languages", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedNative.equals(selectedTarget)) {
            Toast.makeText(requireContext(),
                    "Please choose two different languages", Toast.LENGTH_SHORT).show();
            return;
        }

        AnimUtils.pulse(binding.btnSave);

        prefs.setNativeLanguage(selectedNative, LanguageUtils.getCode(selectedNative));
        prefs.setTargetLanguage(selectedTarget, LanguageUtils.getCode(selectedTarget));

        Toast.makeText(requireContext(),
                "Saved — now learning " + selectedTarget, Toast.LENGTH_SHORT).show();

        // Pop back to home after a beat so the pulse animation can play.
        source.postDelayed(() -> {
            if (isAdded() && getView() != null) {
                Navigation.findNavController(getView()).navigateUp();
            }
        }, 250);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
