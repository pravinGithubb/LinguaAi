package com.aitranslator.app.ui.story;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;

public class AiStoryViewModel extends AndroidViewModel {

    public static class Story {
        public final String title;
        public final String body;
        public final String vocabulary;
        public final String comprehensionQuestions;
        public final String level;

        Story(String title, String body, String vocabulary, String questions, String level) {
            this.title = title;
            this.body = body;
            this.vocabulary = vocabulary;
            this.comprehensionQuestions = questions;
            this.level = level;
        }
    }

    private final AppRepository repository;
    private final PrefsManager prefs;

    private final MutableLiveData<Story> story = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Story> getStory() { return story; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public static final String[] LEVELS = {"Beginner (A1)", "Elementary (A2)", "Intermediate (B1)", "Upper-Intermediate (B2)"};
    public static final String[] GENRES = {"Adventure", "Mystery", "Romance", "Sci-Fi", "Folk Tale", "Humour", "Travel", "Historical"};

    public AiStoryViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    public void generateStory(String level, String genre) {
        isLoading.setValue(true);
        story.setValue(null);
        String targetLang = prefs.getTargetLanguage();
        String nativeLang = prefs.getNativeLanguage();

        // Approximate word count by level
        String wordCount;
        switch (level) {
            case "Beginner (A1)": wordCount = "80-120 words"; break;
            case "Elementary (A2)": wordCount = "120-180 words"; break;
            case "Intermediate (B1)": wordCount = "180-250 words"; break;
            default: wordCount = "250-350 words"; break;
        }

        String prompt =
            "Write an original " + genre + " short story in " + targetLang +
            " for a " + level + " learner. Length: " + wordCount + ".\n" +
            "Use vocabulary and grammar appropriate for the " + level + " level.\n\n" +
            "Respond using EXACTLY this format. Each header on its own line, " +
            "followed by the content. Do not use markdown bold or hashes around the headers.\n\n" +
            "TITLE:\n<story title in " + targetLang + ">\n\n" +
            "LEVEL:\n" + level + "\n\n" +
            "STORY:\n<the story text — multi-paragraph, fully in " + targetLang + ">\n\n" +
            "VOCABULARY:\n<5-8 key words from the story, one per line, formatted as: word — " + nativeLang + " translation>\n\n" +
            "QUESTIONS:\n<3 numbered comprehension questions in " + nativeLang + ", one per line>";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override public void onSuccess(String response) {
                try {
                    String[] all = {"TITLE", "LEVEL", "STORY", "VOCABULARY", "QUESTIONS"};
                    String title = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "TITLE", all);
                    String lvl = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "LEVEL", all);
                    String body = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "STORY", all);
                    String vocab = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "VOCABULARY", all);
                    String questions = com.aitranslator.app.utils.GeminiResponseUtils
                        .extractSection(response, "QUESTIONS", all);

                    if (body.isEmpty()) {
                        // If the model ignored our format, treat the whole response as the story
                        body = response.trim();
                    }
                    story.postValue(new Story(title, body, vocab, questions,
                        lvl.isEmpty() ? level : lvl));
                } catch (Exception e) {
                    error.postValue("Could not parse story: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }
            @Override public void onError(String err) {
                error.postValue("Story generation failed: " + err);
                isLoading.postValue(false);
            }
        });
    }

    private String extract(String text, String start, String end) {
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
