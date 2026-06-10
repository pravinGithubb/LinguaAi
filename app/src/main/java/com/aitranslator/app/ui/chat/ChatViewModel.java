package com.aitranslator.app.ui.chat;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.ConversationMessage;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;
import java.util.*;

public class ChatViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final PrefsManager prefs;
    private final String sessionId;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final LiveData<List<ConversationMessage>> messages;
    private final List<ConversationMessage> messageHistory = new ArrayList<>();

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
        sessionId = UUID.randomUUID().toString();
        messages = repository.getMessagesBySession(sessionId);
    }

    public LiveData<List<ConversationMessage>> getMessages() { return messages; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void sendMessage(String userText) {
        if (userText == null || userText.trim().isEmpty()) return;
        ConversationMessage userMsg = new ConversationMessage(sessionId, "user", userText.trim(),
                null, System.currentTimeMillis(), prefs.getNativeLanguageCode(), prefs.getTargetLanguageCode());
        repository.insertMessage(userMsg);
        messageHistory.add(userMsg);
        isLoading.setValue(true);

        repository.sendMessageToGemini(userText, prefs.getNativeLanguage(), prefs.getTargetLanguage(),
                new ArrayList<>(messageHistory), new AppRepository.AiResponseCallback() {
                    @Override public void onSuccess(String response) {
                        ConversationMessage aiMsg = new ConversationMessage(sessionId, "ai", response,
                                null, System.currentTimeMillis(), prefs.getTargetLanguageCode(), prefs.getNativeLanguageCode());
                        repository.insertMessage(aiMsg);
                        messageHistory.add(aiMsg);
                        isLoading.postValue(false);
                    }
                    @Override public void onError(String error) {
                        errorMessage.postValue(error); isLoading.postValue(false);
                    }
                });
    }
}