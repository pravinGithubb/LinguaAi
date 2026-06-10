package com.aitranslator.app.ui.langpacks;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.aitranslator.app.databinding.FragmentLanguagePacksBinding;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.LanguageUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.util.*;

public class LanguagePacksFragment extends Fragment {

    private FragmentLanguagePacksBinding binding;
    private LanguagePackManager packManager;
    private PrefsManager prefs;

    // Curated list of supported offline languages
    private static final String[][] OFFLINE_LANGUAGES = {
        {"en", "English"}, {"es", "Spanish"}, {"fr", "French"}, {"de", "German"},
        {"it", "Italian"}, {"pt", "Portuguese"}, {"nl", "Dutch"}, {"ru", "Russian"},
        {"ja", "Japanese"}, {"zh", "Chinese"}, {"ko", "Korean"}, {"ar", "Arabic"},
        {"hi", "Hindi"}, {"tr", "Turkish"}, {"pl", "Polish"}, {"vi", "Vietnamese"},
        {"th", "Thai"}, {"id", "Indonesian"}, {"sv", "Swedish"}, {"no", "Norwegian"},
        {"da", "Danish"}, {"fi", "Finnish"}, {"cs", "Czech"}, {"el", "Greek"}
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLanguagePacksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        packManager = new LanguagePackManager();
        prefs = PrefsManager.getInstance(requireContext());

        binding.tvCurrentTarget.setText("Target: " + prefs.getTargetLanguage());

        binding.switchWifiOnly.setChecked(true);
        binding.switchWifiOnly.setOnCheckedChangeListener((b, checked) -> { /* applied at download */ });

        AnimUtils.fadeInUp(binding.cardInfo, 100);
        AnimUtils.fadeInUp(binding.layoutPackList, 200);

        binding.btnTestOffline.setOnClickListener(v -> testOfflineTranslation());

        refreshList();
    }

    private void refreshList() {
        binding.containerPacks.removeAllViews();
        TextView loadingTv = new TextView(requireContext());
        loadingTv.setText("Checking installed packs…");
        loadingTv.setTextColor(0xFF6B7280);
        loadingTv.setPadding(dp(20), dp(20), dp(20), dp(20));
        binding.containerPacks.addView(loadingTv);

        packManager.listDownloadedModels(downloaded -> {
            requireActivity().runOnUiThread(() -> renderPacks(downloaded));
        });
    }

    private void renderPacks(Set<String> downloaded) {
        binding.containerPacks.removeAllViews();
        binding.tvDownloadedCount.setText(downloaded.size() + " downloaded");

        int delay = 0;
        for (String[] lang : OFFLINE_LANGUAGES) {
            String code = lang[0];
            String name = lang[1];
            boolean installed = downloaded.contains(code);
            View row = createPackRow(code, name, installed);
            binding.containerPacks.addView(row);
            AnimUtils.fadeInUp(row, delay);
            delay += 30;
        }
    }

    private View createPackRow(String code, String name, boolean installed) {
        com.google.android.material.card.MaterialCardView card =
            new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lp);
        card.setRadius(dp(14));
        card.setCardElevation(0);
        card.setStrokeColor(installed ? 0xFF2ECC71 : 0xFFE5E7EB);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(installed ? 0xFFE8FAF0 : 0xFFFFFFFF);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));

        TextView flag = new TextView(requireContext());
        flag.setText(LanguageUtils.getFlagEmoji(code));
        flag.setTextSize(22f);
        flag.setLayoutParams(new LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(requireContext());
        title.setText(name);
        title.setTextSize(15f);
        title.setTextColor(0xFF1A1F36);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText(installed ? "✓ Available offline" : "Tap to download (~30 MB)");
        subtitle.setTextSize(12f);
        subtitle.setTextColor(installed ? 0xFF2ECC71 : 0xFF6B7280);

        col.addView(title);
        col.addView(subtitle);

        com.google.android.material.button.MaterialButton actionBtn =
            new com.google.android.material.button.MaterialButton(requireContext());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(36));
        actionBtn.setLayoutParams(blp);
        actionBtn.setMinWidth(dp(80));
        actionBtn.setPadding(dp(12), 0, dp(12), 0);
        actionBtn.setTextSize(12f);
        actionBtn.setCornerRadius(dp(10));
        actionBtn.setText(installed ? "Remove" : "Download");
        actionBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            installed ? 0xFFFF6B6B : 0xFF3D52A0));

        actionBtn.setOnClickListener(v -> {
            if (installed) confirmDelete(code, name);
            else startDownload(code, name);
        });

        row.addView(flag);
        row.addView(col);
        row.addView(actionBtn);
        card.addView(row);
        return card;
    }

    private void startDownload(String code, String name) {
        boolean wifiOnly = binding.switchWifiOnly.isChecked();
        Toast.makeText(requireContext(), "Starting download for " + name + "…", Toast.LENGTH_SHORT).show();
        packManager.downloadPack(code, wifiOnly, (success, msg) -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(),
                    success ? name + " downloaded ✓" : name + ": " + msg,
                    Toast.LENGTH_LONG).show();
                if (success) refreshList();
            });
        });
    }

    private void confirmDelete(String code, String name) {
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove " + name + "?")
            .setMessage("This will free up ~30 MB. You can re-download anytime.")
            .setPositiveButton("Remove", (d, w) -> {
                packManager.deletePack(code, (success, msg) -> {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        if (success) refreshList();
                    });
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void testOfflineTranslation() {
        String fromCode = prefs.getTargetLanguageCode();
        String toCode = prefs.getNativeLanguageCode();
        String testText = "Hello, how are you?";

        if (fromCode.equals("en") && toCode.equals("en")) {
            Toast.makeText(requireContext(), "Pick a non-English target language first", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(requireContext(), "Testing offline translation…", Toast.LENGTH_SHORT).show();
        binding.tvOfflineTestResult.setText("Translating offline…");
        binding.tvOfflineTestResult.setVisibility(View.VISIBLE);

        // Try both directions
        packManager.translateOffline(testText, "en", fromCode,
            new LanguagePackManager.TranslateCallback() {
                @Override public void onSuccess(String translation) {
                    requireActivity().runOnUiThread(() -> {
                        binding.tvOfflineTestResult.setText("✓ \"" + testText + "\" → \"" + translation + "\"");
                        binding.tvOfflineTestResult.setTextColor(0xFF2ECC71);
                    });
                }
                @Override public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        binding.tvOfflineTestResult.setText("✗ " + error);
                        binding.tvOfflineTestResult.setTextColor(0xFFFF6B6B);
                    });
                }
            });
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
