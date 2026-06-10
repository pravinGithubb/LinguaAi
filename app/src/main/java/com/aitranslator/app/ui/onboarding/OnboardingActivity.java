package com.aitranslator.app.ui.onboarding;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.aitranslator.app.databinding.ActivityOnboardingBinding;
import com.aitranslator.app.ui.home.MainActivity;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;

public class OnboardingActivity extends AppCompatActivity {
    private ActivityOnboardingBinding binding;
    private PrefsManager prefs;
    private String selectedNativeLang = "English";
    private String selectedTargetLang = "Spanish";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PrefsManager.getInstance(this);
        if (prefs.isOnboardingDone()) { startMain(); return; }

        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Entrance animations
        AnimUtils.fadeInScale(binding.tvHeroEmoji, 0);
        AnimUtils.fadeInUp(binding.tvTitle, 150);
        AnimUtils.fadeInUp(binding.tvSubtitle, 250);
        AnimUtils.fadeInUp(binding.cardLangs, 400);
        AnimUtils.fadeInUp(binding.btnGetStarted, 550);
        AnimUtils.fadeInUp(binding.tvTagline, 650);

        String[] languages = LanguageUtils.getLanguageNames();
        com.aitranslator.app.utils.FlagLanguageAdapter adapter =
                new com.aitranslator.app.utils.FlagLanguageAdapter(this, languages);

        AutoCompleteTextView spinnerNative = binding.spinnerNative;
        AutoCompleteTextView spinnerTarget = binding.spinnerTarget;
        spinnerNative.setAdapter(adapter);
        spinnerTarget.setAdapter(adapter);
        spinnerNative.setText("English", false);
        spinnerTarget.setText("Spanish", false);

        spinnerNative.setOnItemClickListener((p, v, pos, id) -> selectedNativeLang = languages[pos]);
        spinnerTarget.setOnItemClickListener((p, v, pos, id) -> selectedTargetLang = languages[pos]);

        binding.btnGetStarted.setOnClickListener(v -> {
            if (selectedNativeLang.equals(selectedTargetLang)) {
                Toast.makeText(this, "Please choose two different languages!", Toast.LENGTH_SHORT).show();
                return;
            }
            AnimUtils.pulse(binding.btnGetStarted);
            prefs.setNativeLanguage(selectedNativeLang, LanguageUtils.getCode(selectedNativeLang));
            prefs.setTargetLanguage(selectedTargetLang, LanguageUtils.getCode(selectedTargetLang));
            prefs.setOnboardingDone();
            binding.btnGetStarted.postDelayed(this::startMain, 200);
        });
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}