package com.aitranslator.app.ui.pronunciation;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;

public class PronunciationScorerViewModel extends AndroidViewModel {

    public static class ScorerResult {
        public final int score;          // 0–100
        public final String feedback;
        public final String targetText;
        public final String spokenText;
        public final String tips;

        ScorerResult(int score, String feedback, String target, String spoken, String tips) {
            this.score = score;
            this.feedback = feedback;
            this.targetText = target;
            this.spokenText = spoken;
            this.tips = tips;
        }
    }

    private final AppRepository repository;
    private final PrefsManager prefs;

    private final MutableLiveData<ScorerResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> practicePhrase = new MutableLiveData<>();

    public LiveData<ScorerResult> getResult() { return result; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getPracticePhrase() { return practicePhrase; }

    public PronunciationScorerViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    /** Fetch an AI-generated practice phrase */
    public void generatePracticePhrase() {
        isLoading.setValue(true);
        String lang = prefs.getTargetLanguage();
        String prompt = "Give me ONE short " + lang + " phrase (5-12 words) good for pronunciation practice. "
            + "Pick something with interesting sounds common in " + lang + ". "
            + "Reply with ONLY the phrase in " + lang + ", nothing else.";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                practicePhrase.postValue(response.trim().replaceAll("\"", ""));
                isLoading.postValue(false);
            }
            @Override public void onError(String err) {
                error.postValue("Could not generate phrase: " + err);
                isLoading.postValue(false);
            }
        });
    }

    /**
     * Scores pronunciation by comparing spokenText (from STT) against targetText.
     * Uses Gemini to give a nuanced score and feedback.
     */
    public void scorePronunciation(String targetText, String spokenText) {
        if (targetText.trim().isEmpty()) {
            error.setValue("No target phrase to compare against.");
            return;
        }
        if (spokenText.trim().isEmpty()) {
            error.setValue("No speech detected. Please try speaking again.");
            return;
        }
        isLoading.setValue(true);
        result.setValue(null);
        String lang = prefs.getTargetLanguage();

        String prompt =
            "You are a strict but fair " + lang + " pronunciation coach.\n" +
            "Compare what the learner said (transcribed by speech-to-text) against the target phrase.\n" +
            "Speech-to-text isn't perfect — focus on how close the spoken transcript is to the target, " +
            "treating common STT slips charitably.\n\n" +
            "Respond using EXACTLY this format. Each header on its own line followed by the content. " +
            "Do not use markdown bold or hashes around the headers.\n\n" +
            "SCORE:\n<a single integer 0-100 only, nothing else>\n\n" +
            "FEEDBACK:\n<2-3 sentences of pronunciation feedback>\n\n" +
            "TIPS:\n<2 specific tips for the words that were difficult>\n\n" +
            "TARGET PHRASE:\n" + targetText + "\n\n" +
            "WHAT THE LEARNER SAID:\n" + spokenText;

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                try {
                    String[] all = {"SCORE", "FEEDBACK", "TIPS"};
                    String scoreLine = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "SCORE", all);
                    String feedback = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "FEEDBACK", all);
                    String tips = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "TIPS", all);

                    int score = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractScore(scoreLine, 70);

                    result.postValue(new ScorerResult(score, feedback, targetText, spokenText, tips));
                } catch (Exception e) {
                    error.postValue("Could not parse score: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }
            @Override public void onError(String err) {
                error.postValue("Scoring failed: " + err);
                isLoading.postValue(false);
            }
        });
    }

    private String extractSection(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        if (end != null) {
            int e = text.indexOf(end, s);
            return e > s ? text.substring(s, e).trim() : text.substring(s).trim();
        }
        return text.substring(s).trim();
    }
}
