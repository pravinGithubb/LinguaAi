package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.VocabularyWord;
import java.util.List;

@Dao
public interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VocabularyWord word);

    @Update
    void update(VocabularyWord word);

    @Query("DELETE FROM vocabulary WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM vocabulary ORDER BY dateAdded DESC")
    LiveData<List<VocabularyWord>> getAll();

    @Query("SELECT * FROM vocabulary WHERE masteryLevel < 3 ORDER BY nextReviewDate ASC")
    LiveData<List<VocabularyWord>> getDueForReview();

    @Query("SELECT * FROM vocabulary WHERE masteryLevel < 3 ORDER BY nextReviewDate ASC LIMIT :limit")
    List<VocabularyWord> getDueForReviewSync(int limit);

    @Query("SELECT * FROM vocabulary WHERE isWordOfDay = 1 ORDER BY dateAdded DESC LIMIT 1")
    VocabularyWord getWordOfDaySync();

    @Query("UPDATE vocabulary SET isWordOfDay = 0")
    void clearWordOfDay();

    @Query("SELECT COUNT(*) FROM vocabulary")
    int getTotalCount();

    @Query("SELECT COUNT(*) FROM vocabulary WHERE masteryLevel = 3")
    int getMasteredCount();

    @Query("SELECT * FROM vocabulary WHERE word LIKE '%' || :query || '%' OR definition LIKE '%' || :query || '%'")
    LiveData<List<VocabularyWord>> search(String query);

    @Query("SELECT * FROM vocabulary WHERE word = :word AND language = :language LIMIT 1")
    VocabularyWord findByWordAndLanguage(String word, String language);
}