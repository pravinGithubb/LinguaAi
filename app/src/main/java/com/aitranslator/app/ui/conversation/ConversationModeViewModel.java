package com.aitranslator.app.ui.conversation;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;
import java.util.*;

public class ConversationModeViewModel extends AndroidViewModel {

    public static class Message {
        public final String text;
        public final boolean isUser;  // true = learner, false = AI partner
        public final String hint;     // optional translation hint shown under bubble
        Message(String text, boolean isUser, String hint) {
            this.text = text; this.isUser = isUser; this.hint = hint;
        }
    }

    public static final String[] SCENARIOS = {
        "At a Restaurant",
        "Hotel Check-in",
        "Shopping at a Market",
        "Asking for Directions",
        "Doctor's Appointment",
        "Job Interview",
        "Making New Friends",
        "At the Airport",
        "Booking a Train Ticket",
        "Calling Customer Support"
    };
    public static final String[] SCENARIO_EMOJIS = {
        "🍽️","🏨","🛒","🗺️","🏥","💼","🎉","✈️","🚆","📞"
    };

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> ttsText = new MutableLiveData<>();
    private final MutableLiveData<String> suggestedReply = new MutableLiveData<>();

    public LiveData<List<Message>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getTtsText() { return ttsText; }
    public LiveData<String> getSuggestedReply() { return suggestedReply; }

    private final AppRepository repository;
    private final PrefsManager prefs;
    private String currentScenario = "";
    private final List<String> turnHistory = new ArrayList<>();

    public ConversationModeViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    public String getCurrentScenario() { return currentScenario; }

    public void startScenario(String scenario) {
        currentScenario = scenario;
        turnHistory.clear();
        messages.setValue(new ArrayList<>());
        suggestedReply.setValue("");
        isLoading.setValue(true);

        String lang = prefs.getTargetLanguage();
        String native_ = prefs.getNativeLanguage();

        String prompt = "You are a native " + lang + " speaker role-playing in this scenario: \""
            + scenario + "\". The learner speaks " + native_ + " and is practising " + lang + ".\n\n"
            + "Open the conversation. Reply in this EXACT format (two lines):\n"
            + "TARGET: [your opening line in " + lang + ", short, natural, 1-2 sentences]\n"
            + "HINT: [the same line translated to " + native_ + " in parentheses]";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                String target = extract(response, "TARGET:", "HINT:");
                String hint = extract(response, "HINT:", null);
                addAiMessage(target, hint);
                turnHistory.add("AI: " + target);
                generateSuggestedReply();
                isLoading.postValue(false);
            }
            @Override public void onError(String err) {
                error.postValue("Could not start: " + err);
                isLoading.postValue(false);
            }
        });
    }

    public void sendUserTurn(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;
        addUserMessage(userText, "");
        turnHistory.add("USER: " + userText);
        isLoading.setValue(true);
        suggestedReply.setValue("");

        String lang = prefs.getTargetLanguage();
        String native_ = prefs.getNativeLanguage();

        StringBuilder convo = new StringBuilder();
        for (String t : turnHistory) convo.append(t).append("\n");

        String prompt = "You are a native " + lang + " speaker role-playing scenario: \""
            + currentScenario + "\". Stay in character. Reply naturally to continue the conversation.\n\n"
            + "Conversation so far:\n" + convo + "\n"
            + "Reply in this EXACT format (two lines):\n"
            + "TARGET: [your reply in " + lang + ", short, 1-2 sentences]\n"
            + "HINT: [the same line translated to " + native_ + "]";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                String target = extract(response, "TARGET:", "HINT:");
                String hint = extract(response, "HINT:", null);
                addAiMessage(target, hint);
                turnHistory.add("AI: " + target);
                generateSuggestedReply();
                isLoading.postValue(false);
            }
            @Override public void onError(String err) {
                error.postValue("AI error: " + err);
                isLoading.postValue(false);
            }
        });
    }

    /** Asks Gemini for a likely user reply suggestion (helps when stuck). */
    private void generateSuggestedReply() {
        if (turnHistory.isEmpty()) return;
        String lang = prefs.getTargetLanguage();
        String last = turnHistory.get(turnHistory.size() - 1);

        String prompt = "Given this conversation in scenario \"" + currentScenario + "\":\n"
            + last + "\n"
            + "Suggest ONE short natural reply the " + lang + " learner could say next, in " + lang + ".\n"
            + "Reply with ONLY the suggested phrase, no labels, no quotes, nothing else.";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                suggestedReply.postValue(response.trim().replaceAll("^\"|\"$", ""));
            }
            @Override public void onError(String err) { /* silent fail */ }
        });
    }

    public void speakLastAi() {
        List<Message> list = messages.getValue();
        if (list == null) return;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (!list.get(i).isUser) {
                ttsText.setValue(list.get(i).text);
                return;
            }
        }
    }

    public void speakMessage(String text) {
        ttsText.setValue(text);
    }

    private void addAiMessage(String text, String hint) {
        List<Message> list = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        list.add(new Message(text, false, hint));
        messages.postValue(list);
        ttsText.postValue(text);  // auto-speak AI line
    }

    private void addUserMessage(String text, String hint) {
        List<Message> list = new ArrayList<>(messages.getValue() != null ? messages.getValue() : new ArrayList<>());
        list.add(new Message(text, true, hint));
        messages.setValue(list);
    }

    private String extract(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) return text.trim();
        s += start.length();
        if (end != null) {
            int e = text.indexOf(end, s);
            return e > s ? text.substring(s, e).trim() : text.substring(s).trim();
        }
        // strip optional trailing parens
        String result = text.substring(s).trim();
        return result.replaceAll("^\\(|\\)$", "").trim();
    }
}
