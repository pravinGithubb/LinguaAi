package com.aitranslator.app.data.local;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.aitranslator.app.data.local.dao.QuizResultDao;
import com.aitranslator.app.data.local.dao.TranslationHistoryDao;
import com.aitranslator.app.data.local.dao.VocabularyDao;
import com.aitranslator.app.data.local.entity.QuizResult;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import com.aitranslator.app.data.local.entity.VocabularyWord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Instrumented Room DAO tests.
 *
 * These run on a device or emulator (androidTest), using an in-memory
 * database so they leave no state behind and run quickly (~1 sec each).
 *
 * How to run in Android Studio:
 *   Right-click this file → Run 'AppDaoTest'
 *   (Requires a connected device or running emulator.)
 *
 * Three DAOs are tested here for compactness:
 *   - {@link QuizResultDao}
 *   - {@link TranslationHistoryDao}
 *   - {@link VocabularyDao}
 *
 * Each test is independent — setUp() creates a fresh in-memory DB and
 * tearDown() closes it.
 */
@RunWith(AndroidJUnit4.class)
public class AppDaoTest {

    /**
     * Swaps the background executor used by Architecture Components so
     * LiveData runs synchronously in tests.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private AppDatabase db;
    private QuizResultDao quizDao;
    private TranslationHistoryDao historyDao;
    private VocabularyDao vocabDao;

    @Before
    public void createDb() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class)
                .allowMainThreadQueries()   // fine in tests; never do this in production
                .build();
        quizDao   = db.quizResultDao();
        historyDao = db.translationHistoryDao();
        vocabDao  = db.vocabularyDao();
    }

    @After
    public void closeDb() {
        if (db != null) db.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  QuizResultDao
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void quiz_InsertAndRetrieve() throws InterruptedException {
        QuizResult row = new QuizResult(
                System.currentTimeMillis(), 10, 8, 80, "Spanish");
        quizDao.insert(row);

        List<QuizResult> results = getValueNow(quizDao.getRecent());
        assertEquals(1, results.size());
        assertEquals(10, results.get(0).totalQuestions);
        assertEquals(8,  results.get(0).correctAnswers);
        assertEquals(80, results.get(0).xpEarned);
        assertEquals("Spanish", results.get(0).language);
    }

    @Test
    public void quiz_InsertMultiple_OrderedByTimestampDesc() throws InterruptedException {
        long now = System.currentTimeMillis();
        quizDao.insert(new QuizResult(now - 2000, 10, 5, 50, "Spanish")); // oldest
        quizDao.insert(new QuizResult(now - 1000, 10, 7, 70, "Hindi"));
        quizDao.insert(new QuizResult(now,         10, 9, 90, "French")); // newest

        List<QuizResult> results = getValueNow(quizDao.getRecent());
        assertEquals(3, results.size());
        assertEquals("French",  results.get(0).language); // newest first
        assertEquals("Hindi",   results.get(1).language);
        assertEquals("Spanish", results.get(2).language);
    }

    @Test
    public void quiz_getTotalXp_SumsAllRows() {
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 10, 8, 80, "Spanish"));
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 10, 6, 60, "Hindi"));
        assertEquals(140, quizDao.getTotalXp());
    }

    @Test
    public void quiz_getTotalXp_EmptyDb_ReturnsZero() {
        assertEquals(0, quizDao.getTotalXp());
    }

    @Test
    public void quiz_getTotalQuizzes_CountsRows() {
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 5, 3, 30, "French"));
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 5, 4, 40, "French"));
        assertEquals(2, quizDao.getTotalQuizzes());
    }

    @Test
    public void quiz_getTotalCorrect_SumsCorrectAnswers() {
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 10, 7, 70, "Spanish"));
        quizDao.insert(new QuizResult(System.currentTimeMillis(), 10, 9, 90, "Spanish"));
        assertEquals(16, quizDao.getTotalCorrect());
    }

    @Test
    public void quiz_getRecent_LimitedTo20Rows() throws InterruptedException {
        // Insert 25 rows — getRecent() should cap at 20
        long base = System.currentTimeMillis();
        for (int i = 0; i < 25; i++) {
            quizDao.insert(new QuizResult(base + i * 1000, 10, i % 10, i * 10, "Spanish"));
        }
        List<QuizResult> results = getValueNow(quizDao.getRecent());
        assertTrue("getRecent must return at most 20 rows",
                results.size() <= 20);
    }

    @Test
    public void quiz_Insert_ReturnsGeneratedId() {
        QuizResult row = new QuizResult(System.currentTimeMillis(), 10, 8, 80, "Spanish");
        long id = quizDao.insert(row);
        assertTrue("Auto-generated ID must be > 0", id > 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TranslationHistoryDao
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void history_InsertAndRetrieve() throws InterruptedException {
        TranslationHistory row = new TranslationHistory(
                "Hello", "Hola", "English", "Spanish",
                System.currentTimeMillis());
        historyDao.insert(row);

        List<TranslationHistory> all = getValueNow(historyDao.getAll());
        assertEquals(1, all.size());
        assertEquals("Hello", all.get(0).originalText);
        assertEquals("Hola",  all.get(0).translatedText);
        assertEquals("English", all.get(0).fromLanguage);
        assertEquals("Spanish", all.get(0).toLanguage);
    }

    @Test
    public void history_DefaultFavourite_IsFalse() throws InterruptedException {
        historyDao.insert(new TranslationHistory(
                "Good morning", "Buenos días", "English", "Spanish",
                System.currentTimeMillis()));
        List<TranslationHistory> all = getValueNow(historyDao.getAll());
        assertFalse("Newly inserted history must not be favourite by default",
                all.get(0).isFavorite);
    }

    @Test
    public void history_InsertMultiple_AllRetrieved() throws InterruptedException {
        historyDao.insert(new TranslationHistory("cat", "gato", "en", "es",
                System.currentTimeMillis()));
        historyDao.insert(new TranslationHistory("dog", "perro", "en", "es",
                System.currentTimeMillis()));
        historyDao.insert(new TranslationHistory("house", "casa", "en", "es",
                System.currentTimeMillis()));

        List<TranslationHistory> all = getValueNow(historyDao.getAll());
        assertEquals(3, all.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VocabularyDao
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    public void vocab_InsertAndRetrieve() throws InterruptedException {
        VocabularyWord word = makeWord("hola", "es");
        vocabDao.insert(word);

        List<VocabularyWord> all = getValueNow(vocabDao.getAll());
        assertEquals(1, all.size());
        assertEquals("hola", all.get(0).word);
        assertEquals("es",   all.get(0).language);
    }

    @Test
    public void vocab_Search_FindsByWordSubstring() throws InterruptedException {
        vocabDao.insert(makeWord("elephant", "en"));
        vocabDao.insert(makeWord("elevate", "en"));
        vocabDao.insert(makeWord("cat", "en"));

        List<VocabularyWord> results = getValueNow(vocabDao.search("ele"));
        assertEquals("Search 'ele' should find elephant + elevate", 2, results.size());
    }

    @Test
    public void vocab_InsertedWordHasMasteryLevel0() throws InterruptedException {
        vocabDao.insert(makeWord("bonjour", "fr"));
        List<VocabularyWord> all = getValueNow(vocabDao.getAll());
        assertEquals("New word should have mastery level 0",
                0, all.get(0).masteryLevel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper: synchronously get a LiveData value (max 2s wait)
    // ─────────────────────────────────────────────────────────────────────────

    private <T> T getValueNow(androidx.lifecycle.LiveData<T> liveData)
            throws InterruptedException {
        final Object[] result = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        androidx.lifecycle.Observer<T> observer = value -> {
            result[0] = value;
            latch.countDown();
        };
        // observeForever must run on main thread — InstantTaskExecutorRule handles this
        liveData.observeForever(observer);
        if (!latch.await(2, TimeUnit.SECONDS)) {
            fail("LiveData did not emit a value within 2 seconds");
        }
        liveData.removeObserver(observer);
        //noinspection unchecked
        return (T) result[0];
    }

    /** Builds a minimal VocabularyWord for insert tests. */
    private VocabularyWord makeWord(String word, String lang) {
        return new VocabularyWord(
                word, lang, "test definition",
                "/phonetic/", "noun", "example sentence", "translation");
    }
}
