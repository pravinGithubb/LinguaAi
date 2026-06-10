package com.aitranslator.app.ui.grammar;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;

public class GrammarCheckerViewModel extends AndroidViewModel {

    public static class GrammarResult {
        public final String correctedText;
        public final String errorsExplained;
        public final String overallScore;
        public final String tips;

        GrammarResult(String corrected, String errors, String score, String tips) {
            this.correctedText = corrected;
            this.errorsExplained = errors;
            this.overallScore = score;
            this.tips = tips;
        }
    }

    private final AppRepository repository;
    private final PrefsManager prefs;

    private final MutableLiveData<GrammarResult> result = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<GrammarResult> getResult() { return result; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public GrammarCheckerViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    public void checkGrammar(String text) {
        if (text.trim().isEmpty()) {
            error.setValue("Please enter some text to check.");
            return;
        }
        isLoading.setValue(true);
        result.setValue(null);
        String targetLang = prefs.getTargetLanguage();

        String prompt =
            "You are a strict but encouraging " + targetLang + " grammar teacher.\n" +
            "Analyse the learner's text below and respond using EXACTLY this format. " +
            "Use these four headers in this order, each on its own line followed by the content. " +
            "Do not use markdown bold or hashes around the headers.\n\n" +
            "CORRECTED:\n<the corrected version of the full text>\n\n" +
            "ERRORS:\n<numbered list of errors found, with explanation. " +
            "If there are no errors, write: No errors — well done!>\n\n" +
            "SCORE:\n<a single line in the form 'N/10 — short comment'>\n\n" +
            "TIPS:\n<2-3 specific tips to improve, one per line>\n\n" +
            "LEARNER'S TEXT:\n" + text;

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    String[] all = {"CORRECTED", "ERRORS", "SCORE", "TIPS"};
                    String corrected = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "CORRECTED", all);
                    String errors = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "ERRORS", all);
                    String score = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "SCORE", all);
                    String tips = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "TIPS", all);

                    // Fall back to the full response if header parsing failed entirely
                    if (corrected.isEmpty() && errors.isEmpty() && score.isEmpty() && tips.isEmpty()) {
                        corrected = response.trim();
                    }
                    result.postValue(new GrammarResult(corrected, errors, score, tips));
                } catch (Exception e) {
                    error.postValue("Could not parse response: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }
            @Override
            public void onError(String err) {
                error.postValue("Grammar check failed: " + err);
                isLoading.postValue(false);
            }
        });
    }

    private String extract(String text, String start, String end) {
        // Deprecated — kept private to avoid breaking older callers in this file.
        // New code should use GeminiResponseUtils.extractSection.
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
