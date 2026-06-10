package com.aitranslator.app.ui.conversation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.aitranslator.app.databinding.FragmentConversationModeBinding;
import com.aitranslator.app.service.SpeechRecognitionManager;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;
import java.util.List;

public class ConversationModeFragment extends Fragment {

    private FragmentConversationModeBinding binding;
    private ConversationModeViewModel viewModel;
    private SpeechRecognitionManager speech;
    private TtsManager tts;
    private PrefsManager prefs;
    private boolean isListening = false;

    private final ActivityResultLauncher<String> micPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startListening();
            else Toast.makeText(requireContext(),
                "Microphone permission needed", Toast.LENGTH_LONG).show();
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConversationModeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConversationModeViewModel.class);
        prefs = PrefsManager.getInstance(requireContext());
        speech = new SpeechRecognitionManager(requireContext());
        tts = new TtsManager(requireContext(), null);

        showPicker();
        wireObservers();

        binding.btnSpeak.setOnClickListener(v -> {
            if (isListening) { speech.stopListening(); stopListeningUI(); }
            else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) startListening();
            else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO);
        });

        binding.btnSendType.setOnClickListener(v -> {
            String text = binding.etTypeReply.getText().toString().trim();
            if (!text.isEmpty()) {
                viewModel.sendUserTurn(text);
                binding.etTypeReply.setText("");
            }
        });

        binding.btnReplayLast.setOnClickListener(v -> viewModel.speakLastAi());

        binding.btnEndConversation.setOnClickListener(v -> showPicker());

        binding.btnUseSuggestion.setOnClickListener(v -> {
            String s = binding.tvSuggestion.getText().toString();
            if (!s.isEmpty() && !s.equals("Loading suggestion…")) {
                binding.etTypeReply.setText(s);
            }
        });
    }

    private void showPicker() {
        binding.layoutPicker.setVisibility(View.VISIBLE);
        binding.layoutChat.setVisibility(View.GONE);
        binding.containerScenarios.removeAllViews();

        for (int i = 0; i < ConversationModeViewModel.SCENARIOS.length; i++) {
            String scenario = ConversationModeViewModel.SCENARIOS[i];
            String emoji = ConversationModeViewModel.SCENARIO_EMOJIS[i];
            View card = createScenarioCard(scenario, emoji);
            binding.containerScenarios.addView(card);
            AnimUtils.fadeInUp(card, 50L * i);
        }
    }

    private View createScenarioCard(String scenario, String emoji) {
        com.google.android.material.card.MaterialCardView card =
            new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        card.setRadius(dp(20));
        card.setCardElevation(0);
        card.setStrokeColor(0xFFE5E7EB);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView emojiTv = new TextView(requireContext());
        emojiTv.setText(emoji);
        emojiTv.setTextSize(28f);
        emojiTv.setLayoutParams(new LinearLayout.LayoutParams(dp(48), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(requireContext());
        title.setText(scenario);
        title.setTextSize(16f);
        title.setTextColor(0xFF1A1F36);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        tlp.setMarginStart(dp(12));
        title.setLayoutParams(tlp);

        TextView arrow = new TextView(requireContext());
        arrow.setText("→");
        arrow.setTextSize(20f);
        arrow.setTextColor(0xFF3D52A0);

        row.addView(emojiTv);
        row.addView(title);
        row.addView(arrow);
        card.addView(row);

        card.setOnClickListener(v -> {
            AnimUtils.pulse(card);
            card.postDelayed(() -> {
                binding.layoutPicker.setVisibility(View.GONE);
                binding.layoutChat.setVisibility(View.VISIBLE);
                binding.tvScenarioTitle.setText(emoji + "  " + scenario);
                binding.containerMessages.removeAllViews();
                viewModel.startScenario(scenario);
            }, 150);
        });

        return card;
    }

    private void wireObservers() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), this::renderMessages);

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressThinking.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getTtsText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                tts.speak(text, prefs.getTargetLanguageCode());
            }
        });

        viewModel.getSuggestedReply().observe(getViewLifecycleOwner(), s -> {
            if (s == null || s.isEmpty()) {
                binding.cardSuggestion.setVisibility(View.GONE);
            } else {
                binding.cardSuggestion.setVisibility(View.VISIBLE);
                binding.tvSuggestion.setText(s);
                AnimUtils.fadeInUp(binding.cardSuggestion, 0);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show();
        });
    }

    private void renderMessages(List<ConversationModeViewModel.Message> messages) {
        binding.containerMessages.removeAllViews();
        for (ConversationModeViewModel.Message m : messages) {
            View bubble = createBubble(m);
            binding.containerMessages.addView(bubble);
        }
        binding.scrollMessages.post(() ->
            binding.scrollMessages.fullScroll(View.FOCUS_DOWN));
    }

    private View createBubble(ConversationModeViewModel.Message m) {
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wlp.setMargins(0, dp(4), 0, dp(4));
        wrapper.setLayoutParams(wlp);
        wrapper.setGravity(m.isUser ? android.view.Gravity.END : android.view.Gravity.START);

        com.google.android.material.card.MaterialCardView card =
            new com.google.android.material.card.MaterialCardView(requireContext());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMarginStart(m.isUser ? dp(40) : 0);
        clp.setMarginEnd(m.isUser ? 0 : dp(40));
        card.setLayoutParams(clp);
        card.setRadius(dp(18));
        card.setCardElevation(0);
        card.setCardBackgroundColor(m.isUser ? 0xFF3D52A0 : 0xFFFFFFFF);
        card.setStrokeColor(0xFFE5E7EB);
        card.setStrokeWidth(m.isUser ? 0 : dp(1));

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), dp(10), dp(14), dp(10));

        TextView tv = new TextView(requireContext());
        tv.setText(m.text);
        tv.setTextSize(15f);
        tv.setTextColor(m.isUser ? 0xFFFFFFFF : 0xFF1A1F36);
        col.addView(tv);

        if (!m.isUser && m.hint != null && !m.hint.isEmpty()) {
            TextView hintTv = new TextView(requireContext());
            hintTv.setText(m.hint);
            hintTv.setTextSize(12f);
            hintTv.setTextColor(0xFF6B7280);
            hintTv.setTypeface(null, android.graphics.Typeface.ITALIC);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hlp.topMargin = dp(4);
            hintTv.setLayoutParams(hlp);
            col.addView(hintTv);
        }

        if (!m.isUser) {
            // Tap-to-replay for AI bubbles
            card.setOnClickListener(v -> {
                AnimUtils.pulse(card);
                viewModel.speakMessage(m.text);
            });
        }

        card.addView(col);
        wrapper.addView(card);
        AnimUtils.fadeInUp(wrapper, 0);
        return wrapper;
    }

    private void startListening() {
        isListening = true;
        binding.btnSpeak.setText("⏹  Stop");
        binding.btnSpeak.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF6B6B));
        speech.startListening(prefs.getTargetLanguageCode(),
            new SpeechRecognitionManager.SpeechCallback() {
                @Override public void onReadyForSpeech() {}
                @Override public void onSpeechDetected() {}
                @Override public void onPartialResult(String partial) {
                    requireActivity().runOnUiThread(() -> binding.etTypeReply.setHint("Hearing: " + partial));
                }
                @Override public void onResult(String text) {
                    requireActivity().runOnUiThread(() -> {
                        stopListeningUI();
                        viewModel.sendUserTurn(text);
                    });
                }
                @Override public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        stopListeningUI();
                        Toast.makeText(requireContext(), "Mic error: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
                @Override public void onEndOfSpeech() { /* result will arrive via onResult */ }
            });
    }

    private void stopListeningUI() {
        isListening = false;
        binding.btnSpeak.setText("🎤  Speak");
        binding.btnSpeak.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D52A0));
        binding.etTypeReply.setHint("Or type a reply…");
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speech != null) speech.destroy();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}
