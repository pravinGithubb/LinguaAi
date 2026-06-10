package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.Phrase;
import java.util.List;

@Dao
public interface PhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Phrase phrase);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<Phrase> phrases);

    @Update
    void update(Phrase phrase);

    @Query("DELETE FROM phrases WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM phrases WHERE languageCode = :langCode ORDER BY category, dateAdded DESC")
    LiveData<List<Phrase>> getByLanguage(String langCode);

    @Query("SELECT * FROM phrases WHERE languageCode = :langCode AND category = :category ORDER BY dateAdded DESC")
    LiveData<List<Phrase>> getByCategory(String langCode, String category);

    @Query("SELECT * FROM phrases WHERE languageCode = :langCode AND isFavorite = 1 ORDER BY dateAdded DESC")
    LiveData<List<Phrase>> getFavorites(String langCode);

    @Query("SELECT COUNT(*) FROM phrases WHERE languageCode = :langCode")
    int getCountForLanguage(String langCode);

    @Query("SELECT DISTINCT category FROM phrases WHERE languageCode = :langCode ORDER BY category")
    List<String> getCategoriesForLanguage(String langCode);
}
