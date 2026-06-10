package com.aitranslator.app.ui.lesson;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;

public class DailyLessonViewModel extends AndroidViewModel {

    public static class Lesson {
        public final String title;
        public final String intro;
        public final String vocabularySection;
        public final String grammarSection;
        public final String practiceSection;
        public final String tip;

        Lesson(String title, String intro, String vocabulary,
               String grammar, String practice, String tip) {
            this.title = title;
            this.intro = intro;
            this.vocabularySection = vocabulary;
            this.grammarSection = grammar;
            this.practiceSection = practice;
            this.tip = tip;
        }
    }

    private final AppRepository repository;
    private final PrefsManager prefs;

    private final MutableLiveData<Lesson> lesson = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public LiveData<Lesson> getLesson() { return lesson; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public DailyLessonViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    public void generateLesson(String topic) {
        isLoading.setValue(true);
        String targetLang = prefs.getTargetLanguage();
        String nativeLang = prefs.getNativeLanguage();

        String prompt = "Create a short 5-minute " + targetLang + " language lesson for a "
            + nativeLang + " speaker. Topic: \"" + topic + "\".\n\n"
            + "Structure your response in exactly this format with these exact section headers:\n"
            + "TITLE: [lesson title]\n"
            + "INTRO: [2-3 sentence engaging introduction]\n"
            + "VOCABULARY: [5-7 key words/phrases with translations, one per line as: word — translation]\n"
            + "GRAMMAR: [1-2 grammar points relevant to the topic, clearly explained with examples]\n"
            + "PRACTICE: [3 simple practice exercises or example sentences]\n"
            + "TIP: [one memorable learning tip]\n\n"
            + "Keep the lesson friendly, practical, and beginner-to-intermediate level.";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Lesson parsed = parseLesson(response);
                    lesson.postValue(parsed);
                } catch (Exception e) {
                    error.postValue("Could not parse lesson: " + e.getMessage());
                } finally {
                    isLoading.postValue(false);
                }
            }
            @Override
            public void onError(String err) {
                error.postValue("Error generating lesson: " + err);
                isLoading.postValue(false);
            }
        });
    }

    private Lesson parseLesson(String raw) {
        String title = extractSection(raw, "TITLE:", "INTRO:");
        String intro = extractSection(raw, "INTRO:", "VOCABULARY:");
        String vocab = extractSection(raw, "VOCABULARY:", "GRAMMAR:");
        String grammar = extractSection(raw, "GRAMMAR:", "PRACTICE:");
        String practice = extractSection(raw, "PRACTICE:", "TIP:");
        String tip = extractSection(raw, "TIP:", null);
        return new Lesson(title, intro, vocab, grammar, practice, tip);
    }

    private String extractSection(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start < 0) return "";
        start += startTag.length();
        if (endTag != null) {
            int end = text.indexOf(endTag, start);
            return end > start ? text.substring(start, end).trim() : text.substring(start).trim();
        }
        return text.substring(start).trim();
    }
}
