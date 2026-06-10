package com.aitranslator.app.ui.chat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aitranslator.app.databinding.FragmentChatBinding;
import com.aitranslator.app.service.TtsManager;
import com.aitranslator.app.utils.AnimUtils;
import com.aitranslator.app.utils.PrefsManager;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter adapter;
    private TtsManager tts;
    private PrefsManager prefs;
    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PrefsManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        adapter = new ChatAdapter(prefs.getTargetLanguageCode(), text -> {
            if (tts != null) tts.speak(text, prefs.getTargetLanguageCode());
        });

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(lm);
        binding.rvMessages.setAdapter(adapter);

        tts = new TtsManager(requireContext(), new TtsManager.TtsCallback() {
            @Override public void onReady() {}
            @Override public void onError(String msg) {}
        });

        binding.tvChatTitle.setText("Practice " + prefs.getTargetLanguage());

        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                adapter.submitList(messages);
                if (!messages.isEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
                    // Once a real conversation has started, free up the
                    // top-of-screen real estate the avatar+greeting are
                    // taking. They reappear next session if the user clears
                    // chat (or the list is empty after backing out).
                    binding.avatarContainer.setVisibility(View.GONE);
                    binding.greetingBubble.setVisibility(View.GONE);
                } else {
                    binding.avatarContainer.setVisibility(View.VISIBLE);
                    binding.greetingBubble.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                binding.typingIndicator.setVisibility(View.VISIBLE);
                AnimUtils.popIn(binding.typingIndicator);
                startTypingLoop();
            } else {
                binding.typingIndicator.setVisibility(View.GONE);
                stopTypingLoop();
            }
            binding.btnSend.setEnabled(!loading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
        });

        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); return true; }
            return false;
        });

        // ✕ button in the top-left dismisses the chat (Lernix-style).
        binding.btnClose.setOnClickListener(v ->
            androidx.navigation.Navigation.findNavController(view).navigateUp());
    }

    private void sendMessage() {
        String text = binding.etMessage.getText() != null ?
                binding.etMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;
        binding.etMessage.setText("");
        viewModel.sendMessage(text);
    }

    private void startTypingLoop() {
        typingRunnable = new Runnable() {
            @Override public void run() {
                if (binding != null && binding.typingIndicator.getVisibility() == View.VISIBLE) {
                    AnimUtils.startTypingAnimation(binding.dot1, binding.dot2, binding.dot3);
                    typingHandler.postDelayed(this, 800);
                }
            }
        };
        typingHandler.post(typingRunnable);
    }

    private void stopTypingLoop() {
        if (typingRunnable != null) typingHandler.removeCallbacks(typingRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTypingLoop();
        if (tts != null) tts.shutdown();
        binding = null;
    }
}