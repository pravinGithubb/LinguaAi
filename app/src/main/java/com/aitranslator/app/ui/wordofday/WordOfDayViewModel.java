package com.aitranslator.app.ui.wordofday;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.data.repository.AppRepository;

public class WordOfDayViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<VocabularyWord> wordOfDay = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> saveMessage = new MutableLiveData<>();

    public WordOfDayViewModel(@NonNull Application app) {
        super(app); repository = new AppRepository(app);
    }

    public LiveData<VocabularyWord> getWordOfDay() { return wordOfDay; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getSaveMessage() { return saveMessage; }

    public void loadWordOfDay(String targetLanguage) {
        isLoading.setValue(true);
        repository.getOrFetchWordOfDay(targetLanguage, new AppRepository.WordOfDayCallback() {
            @Override public void onSuccess(VocabularyWord word) {
                wordOfDay.postValue(word); isLoading.postValue(false);
            }
            @Override public void onError(String msg) {
                error.postValue(msg); isLoading.postValue(false);
            }
        });
    }

    public void saveToVocabulary(VocabularyWord word) {
        repository.saveWord(word, new AppRepository.VocabularyCallback() {
            @Override public void onSuccess() { saveMessage.postValue("✅ Saved to vocabulary!"); }
            @Override public void onError(String msg) { saveMessage.postValue(msg); }
        });
    }
}