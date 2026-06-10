package com.aitranslator.app.ui.phrasebook;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.Phrase;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;

public class PhrasebookViewModel extends AndroidViewModel {

    public static final String[] CATEGORIES = {
        "Greetings", "Food & Drink", "Directions", "Shopping",
        "Emergency", "Hotel", "Transport", "Numbers & Time",
        "Polite Phrases", "Small Talk"
    };
    public static final String[] CATEGORY_EMOJIS = {
        "👋", "🍽️", "🗺️", "🛒", "🚨", "🏨", "🚆", "🔢", "🙏", "💬"
    };

    private final AppRepository repository;
    private final PrefsManager prefs;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final MutableLiveData<String> currentCategory = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> showFavoritesOnly = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<String> getCurrentCategory() { return currentCategory; }
    public LiveData<Boolean> getShowFavoritesOnly() { return showFavoritesOnly; }

    public PhrasebookViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    public LiveData<List<Phrase>> getAllPhrases() {
        return repository.getPhrasesByLanguage(prefs.getTargetLanguageCode());
    }

    public LiveData<List<Phrase>> getPhrasesForCategory(String category) {
        return repository.getPhrasesByCategory(prefs.getTargetLanguageCode(), category);
    }

    public LiveData<List<Phrase>> getFavorites() {
        return repository.getFavoritePhrases(prefs.getTargetLanguageCode());
    }

    public void selectCategory(String category) {
        currentCategory.setValue(category);
        showFavoritesOnly.setValue(false);
    }

    public void showFavorites() {
        showFavoritesOnly.setValue(true);
        currentCategory.setValue("");
    }

    public void toggleFavorite(Phrase p) {
        repository.togglePhraseFavorite(p);
    }

    public void deletePhrase(Phrase p) {
        repository.deletePhrase(p.id);
    }

    /** Generates 8-12 phrases for a category via Gemini and saves to Room. */
    public void generatePhrasesForCategory(String category) {
        isLoading.setValue(true);
        String lang = prefs.getTargetLanguage();
        String langCode = prefs.getTargetLanguageCode();
        String native_ = prefs.getNativeLanguage();

        String prompt = "Generate 10 essential travel phrases for the category \"" + category
            + "\" for someone learning " + lang + " whose native language is " + native_ + ".\n"
            + "Respond with ONLY a valid JSON array, no markdown, no extra text:\n"
            + "[\n"
            + "  {\n"
            + "    \"target\": \"phrase in " + lang + "\",\n"
            + "    \"native\": \"translation in " + native_ + "\",\n"
            + "    \"phonetic\": \"romanization or phonetic guide (or empty string if not needed)\"\n"
            + "  }\n"
            + "]\n"
            + "Use practical, commonly-used everyday phrases.";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                try {
                    String json = response.trim()
                        .replaceAll("```json", "").replaceAll("```", "").trim();
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    List<Phrase> phrases = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject obj = arr.get(i).getAsJsonObject();
                        String target = obj.get("target").getAsString();
                        String nat = obj.get("native").getAsString();
                        String phonetic = obj.has("phonetic") ? obj.get("phonetic").getAsString() : "";
                        phrases.add(new Phrase(category, target, nat, phonetic, langCode));
                    }
                    repository.savePhrases(phrases);
                    status.postValue("Added " + phrases.size() + " phrases to " + category);
                    isLoading.postValue(false);
                } catch (Exception e) {
                    status.postValue("Could not parse phrases: " + e.getMessage());
                    isLoading.postValue(false);
                }
            }
            @Override public void onError(String err) {
                status.postValue("Error: " + err);
                isLoading.postValue(false);
            }
        });
    }
}
