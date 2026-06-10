package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_results")
public class QuizResult {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long timestamp;
    public int totalQuestions;
    public int correctAnswers;
    public int xpEarned;
    public String language;   // target language name

    public QuizResult(long timestamp, int totalQuestions, int correctAnswers,
                      int xpEarned, String language) {
        this.timestamp = timestamp;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.xpEarned = xpEarned;
        this.language = language;
    }
}
