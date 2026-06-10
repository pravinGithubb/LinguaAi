package com.aitranslator.app.data.local.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vocabulary")
public class VocabularyWord {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String word;
    public String language;        // ISO code e.g. "es"
    public String definition;      // short definition
    public String phonetic;        // e.g. "/hola/"
    public String partOfSpeech;    // noun, verb, etc.
    public String exampleSentence;
    public String translation;     // in user's native language
    public long dateAdded;
    public int masteryLevel;       // 0=new, 1=learning, 2=familiar, 3=mastered
    public long nextReviewDate;    // for spaced repetition
    public boolean isWordOfDay;

    public VocabularyWord(String word, String language, String definition,
            String phonetic, String partOfSpeech, String exampleSentence, String translation) {
        this.word = word; this.language = language; this.definition = definition;
        this.phonetic = phonetic; this.partOfSpeech = partOfSpeech;
        this.exampleSentence = exampleSentence; this.translation = translation;
        this.dateAdded = System.currentTimeMillis();
        this.masteryLevel = 0;
        this.nextReviewDate = System.currentTimeMillis();
        this.isWordOfDay = false;
    }
}