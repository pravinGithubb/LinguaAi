package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "media_bookmarks")
public class MediaBookmark {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String mediaType;     // "youtube" or "podcast"
    public String title;
    public String subtitle;      // channel name or podcast name
    public String mediaId;       // YouTube video ID, or episode URL for podcasts
    public String thumbnailUrl;  // poster / cover art
    public String languageCode;  // target language code
    public long durationSec;     // 0 if unknown
    public long savedAt;
    public long lastPositionMs;  // for podcast resume

    public MediaBookmark(String mediaType, String title, String subtitle,
                         String mediaId, String thumbnailUrl, String languageCode,
                         long durationSec) {
        this.mediaType = mediaType;
        this.title = title;
        this.subtitle = subtitle;
        this.mediaId = mediaId;
        this.thumbnailUrl = thumbnailUrl;
        this.languageCode = languageCode;
        this.durationSec = durationSec;
        this.savedAt = System.currentTimeMillis();
        this.lastPositionMs = 0;
    }
}
