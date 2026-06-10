package com.aitranslator.app.data.local.dao;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.aitranslator.app.data.local.entity.MediaBookmark;
import java.util.List;

@Dao
public interface MediaBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MediaBookmark b);

    @Update
    void update(MediaBookmark b);

    @Query("DELETE FROM media_bookmarks WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM media_bookmarks WHERE mediaType = :type AND languageCode = :langCode ORDER BY savedAt DESC")
    LiveData<List<MediaBookmark>> getByTypeAndLanguage(String type, String langCode);

    @Query("SELECT * FROM media_bookmarks WHERE mediaId = :mediaId LIMIT 1")
    MediaBookmark findByMediaId(String mediaId);

    @Query("UPDATE media_bookmarks SET lastPositionMs = :pos WHERE id = :id")
    void updatePosition(long id, long pos);
}
