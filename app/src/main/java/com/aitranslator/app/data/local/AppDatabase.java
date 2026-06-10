package com.aitranslator.app.data.local;
import android.content.Context;
import androidx.room.*;
import com.aitranslator.app.data.local.dao.*;
import com.aitranslator.app.data.local.entity.*;

@Database(
    entities = {
        ConversationMessage.class,
        TranslationHistory.class,
        VocabularyWord.class,
        FlashcardSession.class,
        QuizResult.class,
        Phrase.class,
        MediaBookmark.class
    },
    version = 5,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract MessageDao messageDao();
    public abstract TranslationHistoryDao translationHistoryDao();
    public abstract VocabularyDao vocabularyDao();
    public abstract FlashcardSessionDao flashcardSessionDao();
    public abstract QuizResultDao quizResultDao();
    public abstract PhraseDao phraseDao();
    public abstract MediaBookmarkDao mediaBookmarkDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ai_translator_db"
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
