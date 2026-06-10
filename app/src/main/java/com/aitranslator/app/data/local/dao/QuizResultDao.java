package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.QuizResult;
import java.util.List;

@Dao
public interface QuizResultDao {
    @Insert
    long insert(QuizResult result);

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC LIMIT 20")
    LiveData<List<QuizResult>> getRecent();

    @Query("SELECT SUM(xpEarned) FROM quiz_results")
    int getTotalXp();

    @Query("SELECT COUNT(*) FROM quiz_results")
    int getTotalQuizzes();

    @Query("SELECT SUM(correctAnswers) FROM quiz_results")
    int getTotalCorrect();

    @Query("SELECT SUM(totalQuestions) FROM quiz_results")
    int getTotalAttempted();
}
