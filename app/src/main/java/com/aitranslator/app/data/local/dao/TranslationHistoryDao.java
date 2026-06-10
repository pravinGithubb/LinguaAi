package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.TranslationHistory;
import java.util.List;

@Dao
public interface TranslationHistoryDao {
    @Insert long insert(TranslationHistory history);
    @Update void update(TranslationHistory history);
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    LiveData<List<TranslationHistory>> getAll();
    @Query("SELECT COUNT(*) FROM translation_history")
    int getTotalTranslations();
    @Query("DELETE FROM translation_history WHERE id = :id")
    void deleteById(long id);
}