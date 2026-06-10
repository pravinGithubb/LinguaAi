package com.aitranslator.app.ui.voice;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import com.aitranslator.app.data.repository.AppRepository;

public class VoiceViewModel extends AndroidViewModel {

    public enum VoiceState {
        IDLE,           // waiting for user to tap mic
        LISTENING,      // mic open, recording speech
        PROCESSING,     // STT done, calling translation API
        RESULT,         // translation ready
        SPEAKING,       // TTS playing result
        ERROR           // something went wrong
    }

    private final AppRepository repository;
    private final MutableLiveData<VoiceState> state = new MutableLiveData<>(VoiceState.IDLE);
    private final MutableLiveData<String> spokenText = new MutableLiveData<>();
    private final MutableLiveData<String> translatedText = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> partialText = new MutableLiveData<>();

    public VoiceViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
    }

    public LiveData<VoiceState> getState() { return state; }
    public LiveData<String> getSpokenText() { return spokenText; }
    public LiveData<String> getTranslatedText() { return translatedText; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getPartialText() { return partialText; }

    public void onListeningStarted() {
        state.setValue(VoiceState.LISTENING);
        spokenText.setValue("");
        translatedText.setValue("");
    }

    public void onPartialResult(String partial) {
        partialText.setValue(partial);
    }

    public void onSpeechResult(String text, String fromLang, String fromCode,
                                String toLang, String toCode) {
        spokenText.setValue(text);
        state.setValue(VoiceState.PROCESSING);

        repository.translateText(text, fromLang, fromCode, toLang, toCode,
                new AppRepository.TranslateCallback() {
                    @Override
                    public void onSuccess(String result) {
                        translatedText.postValue(result);
                        state.postValue(VoiceState.RESULT);
                        // Auto-save to history
                        repository.insertHistory(new TranslationHistory(
                                text, result, fromLang, toLang, System.currentTimeMillis()));
                    }
                    @Override
                    public void onError(String error) {
                        errorMessage.postValue(error);
                        state.postValue(VoiceState.ERROR);
                    }
                });
    }

    public void onSpeakingStarted() { state.setValue(VoiceState.SPEAKING); }
    public void onSpeakingDone() { state.setValue(VoiceState.IDLE); }

    public void onError(String message) {
        errorMessage.setValue(message);
        state.setValue(VoiceState.ERROR);
    }

    public void reset() {
        state.setValue(VoiceState.IDLE);
        spokenText.setValue("");
        translatedText.setValue("");
        partialText.setValue("");
    }
}
