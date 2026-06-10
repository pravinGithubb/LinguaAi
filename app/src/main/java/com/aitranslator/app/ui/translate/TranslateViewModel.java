package com.aitranslator.app.ui.translate;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import com.aitranslator.app.data.repository.AppRepository;
import java.util.List;

public class TranslateViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final MutableLiveData<String> translatedText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final LiveData<List<TranslationHistory>> history;

    public TranslateViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        history = repository.getAllHistory();
    }

    public LiveData<String> getTranslatedText() { return translatedText; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<TranslationHistory>> getHistory() { return history; }

    public void translate(String text, String fromLang, String fromCode, String toLang, String toCode) {
        if (text == null || text.trim().isEmpty()) return;
        isLoading.setValue(true);
        repository.translateText(text, fromLang, fromCode, toLang, toCode,
                new AppRepository.TranslateCallback() {
                    @Override public void onSuccess(String result) {
                        translatedText.postValue(result); isLoading.postValue(false);
                        repository.insertHistory(new TranslationHistory(text, result, fromLang, toLang, System.currentTimeMillis()));
                    }
                    @Override public void onError(String error) { errorMessage.postValue(error); isLoading.postValue(false); }
                });
    }

    public void toggleFavorite(TranslationHistory item) { repository.toggleFavorite(item); }
    public void deleteHistory(long id) { repository.deleteHistory(id); }
}