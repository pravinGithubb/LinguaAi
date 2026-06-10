package com.aitranslator.app.ui.dictionary;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.data.remote.model.DictionaryResponse;
import com.aitranslator.app.data.repository.AppRepository;

public class DictionaryViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<DictionaryResponse> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> saveMessage = new MutableLiveData<>();

    public DictionaryViewModel(@NonNull Application app) {
        super(app);
        repository = new AppRepository(app);
    }

    public LiveData<DictionaryResponse> getResult() { return result; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getSaveMessage() { return saveMessage; }

    public void lookup(String word, String languageCode) {
        if (word == null || word.trim().isEmpty()) return;
        isLoading.setValue(true);
        repository.lookupWord(word.trim(), languageCode, new AppRepository.DictionaryCallback() {
            @Override public void onSuccess(DictionaryResponse response) {
                result.postValue(response);
                isLoading.postValue(false);
            }
            @Override public void onError(String msg) {
                error.postValue(msg);
                isLoading.postValue(false);
            }
        });
    }

    public void saveToVocabulary(DictionaryResponse response, String language, String translation) {
        VocabularyWord word = new VocabularyWord(
                response.word, language, response.getFirstDefinition(),
                response.getPhonetic(), response.getPartOfSpeech(),
                response.getFirstExample(), translation);
        repository.saveWord(word, new AppRepository.VocabularyCallback() {
            @Override public void onSuccess() { saveMessage.postValue("✅ Saved to vocabulary!"); }
            @Override public void onError(String msg) { saveMessage.postValue(msg); }
        });
    }
}