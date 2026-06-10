package com.aitranslator.app.ui.quiz;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.aitranslator.app.data.local.entity.QuizResult;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import com.aitranslator.app.data.repository.AppRepository;
import com.aitranslator.app.utils.PrefsManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;

public class QuizViewModel extends AndroidViewModel {

    // ── Data class for a single question ────────────────────────────────────
    public static class QuizQuestion {
        public final String word;
        public final String correctAnswer;
        public final List<String> options;   // 4 options, shuffled
        public final String exampleSentence;

        QuizQuestion(String word, String correctAnswer, List<String> options, String exampleSentence) {
            this.word = word;
            this.correctAnswer = correctAnswer;
            this.options = options;
            this.exampleSentence = exampleSentence;
        }
    }

    private final AppRepository repository;
    private final PrefsManager prefs;

    // Observed by the fragment
    private final MutableLiveData<QuizQuestion> currentQuestion = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> quizFinished = new MutableLiveData<>(false);

    // Quiz state
    private final List<QuizQuestion> questionBank = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int totalQuestions = 0;
    private long quizStartTime;

    public LiveData<QuizQuestion> getCurrentQuestion() { return currentQuestion; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getQuizFinished() { return quizFinished; }
    public int getCorrectCount() { return correctCount; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCurrentIndex() { return currentIndex; }

    public QuizViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        prefs = PrefsManager.getInstance(application);
    }

    /** Starts a quiz by generating questions via Gemini from vocabulary words */
    public void startQuiz(int questionCount) {
        isLoading.setValue(true);
        quizStartTime = System.currentTimeMillis();
        questionBank.clear();
        currentIndex = 0;
        correctCount = 0;
        quizFinished.setValue(false);

        String targetLang = prefs.getTargetLanguage();
        String nativeLang = prefs.getNativeLanguage();

        // Ask Gemini to generate multiple-choice vocabulary quiz questions
        String prompt = "Generate " + questionCount + " multiple-choice vocabulary quiz questions for "
            + targetLang + " learners whose native language is " + nativeLang + ".\n"
            + "Each question should test the meaning of a " + targetLang + " word.\n"
            + "Respond ONLY with a valid JSON array, no markdown, no extra text:\n"
            + "[\n"
            + "  {\n"
            + "    \"word\": \"" + targetLang + " word here\",\n"
            + "    \"correct\": \"correct " + nativeLang + " translation\",\n"
            + "    \"wrong1\": \"wrong option 1\",\n"
            + "    \"wrong2\": \"wrong option 2\",\n"
            + "    \"wrong3\": \"wrong option 3\",\n"
            + "    \"example\": \"example sentence using the word\"\n"
            + "  }\n"
            + "]\n"
            + "Use common, useful vocabulary at beginner-to-intermediate level.";

        repository.askAi(prompt, new AppRepository.AiResponseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JsonElement root = com.aitranslator.app.utils.GeminiResponseUtils.parseJson(response);

                    // Tolerate either a top-level array or { "questions": [...] }
                    JsonArray arr;
                    if (root.isJsonArray()) {
                        arr = root.getAsJsonArray();
                    } else if (root.isJsonObject() && root.getAsJsonObject().has("questions")
                            && root.getAsJsonObject().get("questions").isJsonArray()) {
                        arr = root.getAsJsonObject().getAsJsonArray("questions");
                    } else {
                        statusMessage.postValue("Quiz response was not a JSON array");
                        isLoading.postValue(false);
                        return;
                    }

                    List<QuizQuestion> questions = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        if (!arr.get(i).isJsonObject()) continue;
                        JsonObject obj = arr.get(i).getAsJsonObject();

                        String word     = optStr(obj, "word");
                        String correct  = optStr(obj, "correct");
                        String w1       = optStr(obj, "wrong1");
                        String w2       = optStr(obj, "wrong2");
                        String w3       = optStr(obj, "wrong3");
                        String example  = optStr(obj, "example");

                        // Skip malformed entries instead of crashing the whole quiz
                        if (word.isEmpty() || correct.isEmpty()
                                || w1.isEmpty() || w2.isEmpty() || w3.isEmpty()) {
                            continue;
                        }

                        List<String> opts = new ArrayList<>(Arrays.asList(correct, w1, w2, w3));
                        Collections.shuffle(opts);
                        questions.add(new QuizQuestion(word, correct, opts, example));
                    }

                    if (questions.isEmpty()) {
                        statusMessage.postValue("Couldn't generate valid questions. Please try again.");
                        isLoading.postValue(false);
                        return;
                    }

                    questionBank.addAll(questions);
                    totalQuestions = questionBank.size();
                    isLoading.postValue(false);
                    currentQuestion.postValue(questionBank.get(0));
                } catch (Exception e) {
                    isLoading.postValue(false);
                    statusMessage.postValue("Could not parse quiz: " + e.getMessage());
                }
            }
            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                statusMessage.postValue("Error generating quiz: " + error);
            }
        });
    }

    /** Safe string field accessor for quiz JSON objects. */
    private static String optStr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString().trim();
    }

    /** Call when user taps an answer. Returns true if correct. */
    public boolean submitAnswer(String chosen) {
        QuizQuestion q = currentQuestion.getValue();
        if (q == null) return false;
        boolean correct = chosen.equals(q.correctAnswer);
        if (correct) correctCount++;
        return correct;
    }

    /** Advances to next question or finishes quiz */
    public void nextQuestion() {
        currentIndex++;
        if (currentIndex < questionBank.size()) {
            currentQuestion.setValue(questionBank.get(currentIndex));
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {
        int xp = correctCount * 10;
        prefs.addXp(xp);

        QuizResult result = new QuizResult(
            System.currentTimeMillis(), totalQuestions, correctCount, xp, prefs.getTargetLanguage());
        repository.saveQuizResult(result);
        quizFinished.setValue(true);
    }

    public int calculateXp() { return correctCount * 10; }
}
