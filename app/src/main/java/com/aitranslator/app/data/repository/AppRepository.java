package com.aitranslator.app.data.repository;
import android.app.Application;
import androidx.lifecycle.LiveData;
import com.aitranslator.app.BuildConfig;
import com.aitranslator.app.data.local.AppDatabase;
import com.aitranslator.app.data.local.dao.*;
import com.aitranslator.app.data.local.entity.*;
import com.aitranslator.app.data.remote.api.RetrofitClient;
import com.aitranslator.app.data.remote.model.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppRepository {
    private final MessageDao messageDao;
    private final TranslationHistoryDao historyDao;
    private final VocabularyDao vocabularyDao;
    private final FlashcardSessionDao flashcardSessionDao;
    private final com.aitranslator.app.data.local.dao.QuizResultDao quizResultDao;
    private final com.aitranslator.app.data.local.dao.PhraseDao phraseDao;
    private final com.aitranslator.app.data.local.dao.MediaBookmarkDao mediaBookmarkDao;
    private final ExecutorService executor;

    public interface AiResponseCallback { void onSuccess(String response); void onError(String error); }
    public interface TranslateCallback { void onSuccess(String translatedText); void onError(String error); }
    public interface DictionaryCallback { void onSuccess(DictionaryResponse response); void onError(String error); }
    public interface VocabularyCallback { void onSuccess(); void onError(String error); }
    public interface IntCallback { void onSuccess(int value); }

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        messageDao = db.messageDao();
        historyDao = db.translationHistoryDao();
        vocabularyDao = db.vocabularyDao();
        flashcardSessionDao = db.flashcardSessionDao();
        quizResultDao = db.quizResultDao();
        phraseDao = db.phraseDao();
        mediaBookmarkDao = db.mediaBookmarkDao();
        executor = Executors.newFixedThreadPool(4);
    }

    // ── Messages ────────────────────────────────────────────────
    public LiveData<List<ConversationMessage>> getMessagesBySession(String sessionId) {
        return messageDao.getMessagesBySession(sessionId);
    }
    public void insertMessage(ConversationMessage message) {
        executor.execute(() -> messageDao.insert(message));
    }

    // ── Translation History ──────────────────────────────────────
    public LiveData<List<TranslationHistory>> getAllHistory() { return historyDao.getAll(); }
    public void insertHistory(TranslationHistory history) {
        executor.execute(() -> historyDao.insert(history));
    }
    public void toggleFavorite(TranslationHistory history) {
        executor.execute(() -> { history.isFavorite = !history.isFavorite; historyDao.update(history); });
    }
    public void deleteHistory(long id) { executor.execute(() -> historyDao.deleteById(id)); }

    // ── Vocabulary ───────────────────────────────────────────────
    public LiveData<List<VocabularyWord>> getAllVocabulary() { return vocabularyDao.getAll(); }
    public LiveData<List<VocabularyWord>> getDueForReview() { return vocabularyDao.getDueForReview(); }
    public LiveData<List<VocabularyWord>> searchVocabulary(String query) { return vocabularyDao.search(query); }

    public void saveWord(VocabularyWord word, VocabularyCallback callback) {
        executor.execute(() -> {
            try {
                VocabularyWord existing = vocabularyDao.findByWordAndLanguage(word.word, word.language);
                if (existing != null) {
                    callback.onError("Word already in your vocabulary!");
                } else {
                    vocabularyDao.insert(word);
                    callback.onSuccess();
                }
            } catch (Exception e) { callback.onError(e.getMessage()); }
        });
    }

    public void updateMastery(VocabularyWord word, boolean known) {
        executor.execute(() -> {
            if (known) {
                word.masteryLevel = Math.min(3, word.masteryLevel + 1);
                // Spaced repetition: 1=1day, 2=3days, 3=7days
                long[] intervals = {0L, 86400000L, 259200000L, 604800000L};
                word.nextReviewDate = System.currentTimeMillis() + intervals[word.masteryLevel];
            } else {
                word.masteryLevel = Math.max(0, word.masteryLevel - 1);
                word.nextReviewDate = System.currentTimeMillis() + 3600000L; // retry in 1hr
            }
            vocabularyDao.update(word);
        });
    }

    public void deleteWord(long id) { executor.execute(() -> vocabularyDao.deleteById(id)); }

    public void saveFlashcardSession(FlashcardSession session) {
        executor.execute(() -> flashcardSessionDao.insert(session));
    }

    // ── Word of the Day ──────────────────────────────────────────
    public void getOrFetchWordOfDay(String language, WordOfDayCallback callback) {
        executor.execute(() -> {
            VocabularyWord existing = vocabularyDao.getWordOfDaySync();
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd",
                    java.util.Locale.getDefault()).format(new java.util.Date());

            // Check if today's word exists
            if (existing != null) {
                String wordDate = new java.text.SimpleDateFormat("yyyy-MM-dd",
                        java.util.Locale.getDefault()).format(new java.util.Date(existing.dateAdded));
                if (wordDate.equals(today)) {
                    callback.onSuccess(existing);
                    return;
                }
            }
            // Fetch a new word of the day via Gemini
            fetchNewWordOfDay(language, callback);
        });
    }

    private void fetchNewWordOfDay(String language, WordOfDayCallback callback) {
        String prompt =
            "Give me one interesting, useful " + language + " vocabulary word for a learner.\n" +
            "Respond with ONLY a single JSON object — no markdown fences, no commentary.\n" +
            "Use this exact schema (all fields are strings):\n" +
            "{\n" +
            "  \"word\": \"the " + language + " word\",\n" +
            "  \"phonetic\": \"IPA or pronunciation guide, or empty string\",\n" +
            "  \"partOfSpeech\": \"noun, verb, adjective, etc.\",\n" +
            "  \"definition\": \"a one-sentence definition in English\",\n" +
            "  \"example\": \"a short example sentence in " + language + "\",\n" +
            "  \"translation\": \"the English translation of the word\"\n" +
            "}";

        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiRequest.Content("user", prompt));

        com.aitranslator.app.data.remote.LlmGateway.generate(new GeminiRequest(contents), new Callback<GeminiResponse>() {
                    @Override public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            callback.onError("API error: " + response.code());
                            return;
                        }
                        try {
                            String raw = response.body().getTextContent();
                            com.google.gson.JsonObject obj =
                                com.aitranslator.app.utils.GeminiResponseUtils.parseJson(raw).getAsJsonObject();

                            VocabularyWord wod = new VocabularyWord(
                                    optString(obj, "word"),
                                    language,
                                    optString(obj, "definition"),
                                    optString(obj, "phonetic"),
                                    optString(obj, "partOfSpeech"),
                                    optString(obj, "example"),
                                    optString(obj, "translation")
                            );
                            if (wod.word == null || wod.word.isEmpty()) {
                                callback.onError("Word of the day was empty");
                                return;
                            }
                            wod.isWordOfDay = true;
                            executor.execute(() -> {
                                vocabularyDao.clearWordOfDay();
                                vocabularyDao.insert(wod);
                                callback.onSuccess(wod);
                            });
                        } catch (Exception e) {
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    }
                    @Override public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    /** Safe getter — returns "" instead of throwing on missing/null fields. */
    private static String optString(com.google.gson.JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    public interface WordOfDayCallback { void onSuccess(VocabularyWord word); void onError(String error); }

    // ── Dictionary Lookup ────────────────────────────────────────
    public void lookupWord(String word, String languageCode, DictionaryCallback callback) {
        // Free Dictionary API only supports English well; for others use Gemini
        if (languageCode.equals("en")) {
            RetrofitClient.getInstance().getDictionaryApiService()
                    .lookup(languageCode, word.toLowerCase().trim())
                    .enqueue(new Callback<List<DictionaryResponse>>() {
                        @Override public void onResponse(Call<List<DictionaryResponse>> call,
                                Response<List<DictionaryResponse>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                callback.onSuccess(response.body().get(0));
                            } else {
                                lookupWithGemini(word, languageCode, callback);
                            }
                        }
                        @Override public void onFailure(Call<List<DictionaryResponse>> call, Throwable t) {
                            lookupWithGemini(word, languageCode, callback);
                        }
                    });
        } else {
            lookupWithGemini(word, languageCode, callback);
        }
    }

    private void lookupWithGemini(String word, String languageCode, DictionaryCallback callback) {
        String prompt = "Define the word \"" + word + "\" in language code \"" + languageCode + "\". " +
                "Respond in this exact JSON format only, no markdown:\n" +
                "{\"word\":\"" + word + "\",\"phonetic\":\"/phonetic/\",\"partOfSpeech\":\"noun\"," +
                "\"definition\":\"clear definition here\",\"example\":\"example sentence here\"}";

        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiRequest.Content("user", prompt));

        com.aitranslator.app.data.remote.LlmGateway.generate(new GeminiRequest(contents), new Callback<GeminiResponse>() {
                    @Override public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String json = response.body().getTextContent().trim()
                                        .replace("```json","").replace("```","").trim();
                                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                                DictionaryResponse dr = new DictionaryResponse();
                                dr.word = obj.has("word") ? obj.get("word").getAsString() : word;
                                dr.phonetic = obj.has("phonetic") ? obj.get("phonetic").getAsString() : "";
                                DictionaryResponse.Meaning meaning = new DictionaryResponse.Meaning();
                                meaning.partOfSpeech = obj.has("partOfSpeech") ? obj.get("partOfSpeech").getAsString() : "";
                                DictionaryResponse.Definition def = new DictionaryResponse.Definition();
                                def.definition = obj.has("definition") ? obj.get("definition").getAsString() : "";
                                def.example = obj.has("example") ? obj.get("example").getAsString() : "";
                                meaning.definitions = new ArrayList<>();
                                meaning.definitions.add(def);
                                dr.meanings = new ArrayList<>();
                                dr.meanings.add(meaning);
                                callback.onSuccess(dr);
                            } catch (Exception e) { callback.onError("Parse error: " + e.getMessage()); }
                        } else { callback.onError("Lookup failed: " + response.code()); }
                    }
                    @Override public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    // ── Gemini Chat ──────────────────────────────────────────────
    public void sendMessageToGemini(String userMessage, String nativeLang, String targetLang,
            List<ConversationMessage> history, AiResponseCallback callback) {
        List<GeminiRequest.Content> contents = new ArrayList<>();
        String systemPrompt = "You are a warm, encouraging language tutor. The user speaks " + nativeLang +
                " and is learning " + targetLang + ". Help them practice through natural conversation. " +
                "Gently correct grammar, explain vocabulary naturally, keep replies to 2-4 sentences. " +
                "If they write in " + nativeLang + ", respond in BOTH " + targetLang + " and " + nativeLang + ".";
        contents.add(new GeminiRequest.Content("user", systemPrompt));
        contents.add(new GeminiRequest.Content("model", "Understood! I'm excited to help you learn " + targetLang + ". Let's begin!"));
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            ConversationMessage msg = history.get(i);
            contents.add(new GeminiRequest.Content(msg.sender.equals("user") ? "user" : "model", msg.content));
        }
        contents.add(new GeminiRequest.Content("user", userMessage));
        com.aitranslator.app.data.remote.LlmGateway.generate(new GeminiRequest(contents), new Callback<GeminiResponse>() {
                    @Override public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String text = response.body().getTextContent();
                            if (!text.isEmpty()) callback.onSuccess(text);
                            else callback.onError("Empty response from AI");
                        } else callback.onError("API Error " + response.code());
                    }
                    @Override public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    // ── Translate ────────────────────────────────────────────────
    public void translateText(String text, String fromLang, String fromCode,
            String toLang, String toCode, TranslateCallback callback) {
        if (BuildConfig.TRANSLATE_API_KEY.equals("YOUR_GOOGLE_TRANSLATE_API_KEY_HERE")) {
            translateWithGemini(text, fromLang, toLang, callback); return;
        }
        RetrofitClient.getInstance().getTranslateApiService()
                .translate(text, fromCode, toCode, BuildConfig.TRANSLATE_API_KEY, "text")
                .enqueue(new Callback<TranslateResponse>() {
                    @Override public void onResponse(Call<TranslateResponse> call, Response<TranslateResponse> response) {
                        if (response.isSuccessful() && response.body() != null)
                            callback.onSuccess(response.body().getTranslatedText());
                        else translateWithGemini(text, fromLang, toLang, callback);
                    }
                    @Override public void onFailure(Call<TranslateResponse> call, Throwable t) {
                        translateWithGemini(text, fromLang, toLang, callback);
                    }
                });
    }

    private void translateWithGemini(String text, String fromLang, String toLang, TranslateCallback callback) {
        String prompt = "Translate from " + fromLang + " to " + toLang + ". Return ONLY translated text:\n\n" + text;
        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiRequest.Content("user", prompt));
        com.aitranslator.app.data.remote.LlmGateway.generate(new GeminiRequest(contents), new Callback<GeminiResponse>() {
                    @Override public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null)
                            callback.onSuccess(response.body().getTextContent());
                        else callback.onError("Translation failed: " + response.code());
                    }
                    @Override public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    public List<VocabularyWord> getDueWordsSync(int limit) {
        return vocabularyDao.getDueForReviewSync(limit);
    }

    // ── Quiz (Batch 3) ────────────────────────────────────────────────────────
    public void saveQuizResult(com.aitranslator.app.data.local.entity.QuizResult result) {
        executor.execute(() -> quizResultDao.insert(result));
    }

    public androidx.lifecycle.LiveData<java.util.List<com.aitranslator.app.data.local.entity.QuizResult>>
            getRecentQuizResults() {
        return quizResultDao.getRecent();
    }

    public void getTotalXpFromDb(IntCallback callback) {
        executor.execute(() -> {
            int xp = quizResultDao.getTotalXp();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(xp));
        });
    }

    // ── Batch 2 convenience methods ───────────────────────────────────────────

    /**
     * Simple Gemini ask — for Camera OCR, PDF, etc.
     */
    public void askAi(String prompt, AiResponseCallback callback) {
        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiRequest.Content("user", prompt));
        com.aitranslator.app.data.remote.LlmGateway.generate(new GeminiRequest(contents), new Callback<GeminiResponse>() {
                    @Override public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null)
                            callback.onSuccess(response.body().getTextContent());
                        else callback.onError("AI error: " + response.code());
                    }
                    @Override public void onFailure(Call<GeminiResponse> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    /**
     * Simplified translate — auto-detects source, uses Gemini.
     * "auto" source lang triggers Gemini fallback directly.
     */
    public void translateText(String text, String fromLang, String toLang, TranslateCallback callback) {
        translateWithGemini(text, fromLang.equals("auto") ? "auto-detected language" : fromLang, toLang, callback);
    }

    // ── Batch 5: Phrasebook ──────────────────────────────────────────────────
    public androidx.lifecycle.LiveData<java.util.List<com.aitranslator.app.data.local.entity.Phrase>>
            getPhrasesByLanguage(String langCode) {
        return phraseDao.getByLanguage(langCode);
    }

    public androidx.lifecycle.LiveData<java.util.List<com.aitranslator.app.data.local.entity.Phrase>>
            getPhrasesByCategory(String langCode, String category) {
        return phraseDao.getByCategory(langCode, category);
    }

    public androidx.lifecycle.LiveData<java.util.List<com.aitranslator.app.data.local.entity.Phrase>>
            getFavoritePhrases(String langCode) {
        return phraseDao.getFavorites(langCode);
    }

    public void savePhrase(com.aitranslator.app.data.local.entity.Phrase phrase) {
        executor.execute(() -> phraseDao.insert(phrase));
    }

    public void savePhrases(java.util.List<com.aitranslator.app.data.local.entity.Phrase> phrases) {
        executor.execute(() -> phraseDao.insertAll(phrases));
    }

    public void togglePhraseFavorite(com.aitranslator.app.data.local.entity.Phrase phrase) {
        executor.execute(() -> {
            phrase.isFavorite = !phrase.isFavorite;
            phraseDao.update(phrase);
        });
    }

    public void deletePhrase(long id) {
        executor.execute(() -> phraseDao.deleteById(id));
    }

    public void getPhraseCount(String langCode, IntCallback callback) {
        executor.execute(() -> {
            int count = phraseDao.getCountForLanguage(langCode);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(count));
        });
    }

    // ── Batch 6: Media Bookmarks (YouTube + Podcasts) ───────────────────────
    public androidx.lifecycle.LiveData<java.util.List<com.aitranslator.app.data.local.entity.MediaBookmark>>
            getMediaBookmarks(String type, String langCode) {
        return mediaBookmarkDao.getByTypeAndLanguage(type, langCode);
    }

    public void saveMediaBookmark(com.aitranslator.app.data.local.entity.MediaBookmark b) {
        executor.execute(() -> mediaBookmarkDao.insert(b));
    }

    public void deleteMediaBookmark(long id) {
        executor.execute(() -> mediaBookmarkDao.deleteById(id));
    }

    public void updateBookmarkPosition(long id, long positionMs) {
        executor.execute(() -> mediaBookmarkDao.updatePosition(id, positionMs));
    }

    public void findMediaBookmark(String mediaId,
            java.util.function.Consumer<com.aitranslator.app.data.local.entity.MediaBookmark> callback) {
        executor.execute(() -> {
            com.aitranslator.app.data.local.entity.MediaBookmark b = mediaBookmarkDao.findByMediaId(mediaId);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.accept(b));
        });
    }
}
