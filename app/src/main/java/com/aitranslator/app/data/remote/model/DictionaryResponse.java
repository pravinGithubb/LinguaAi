package com.aitranslator.app.data.remote.model;
import java.util.List;

// Free Dictionary API: https://api.dictionaryapi.dev/api/v2/entries/en/<word>
public class DictionaryResponse {
    public String word;
    public String phonetic;
    public List<Phonetics> phonetics;
    public List<Meaning> meanings;

    public String getPhonetic() {
        if (phonetic != null && !phonetic.isEmpty()) return phonetic;
        if (phonetics != null) {
            for (Phonetics p : phonetics) {
                if (p.text != null && !p.text.isEmpty()) return p.text;
            }
        }
        return "";
    }

    public String getFirstDefinition() {
        if (meanings != null && !meanings.isEmpty()) {
            Meaning m = meanings.get(0);
            if (m.definitions != null && !m.definitions.isEmpty()) {
                return m.definitions.get(0).definition;
            }
        }
        return "";
    }

    public String getFirstExample() {
        if (meanings != null) {
            for (Meaning m : meanings) {
                if (m.definitions != null) {
                    for (Definition d : m.definitions) {
                        if (d.example != null && !d.example.isEmpty()) return d.example;
                    }
                }
            }
        }
        return "";
    }

    public String getPartOfSpeech() {
        if (meanings != null && !meanings.isEmpty()) return meanings.get(0).partOfSpeech;
        return "";
    }

    public static class Phonetics {
        public String text;
        public String audio;
    }

    public static class Meaning {
        public String partOfSpeech;
        public List<Definition> definitions;
        public List<String> synonyms;
        public List<String> antonyms;
    }

    public static class Definition {
        public String definition;
        public String example;
        public List<String> synonyms;
        public List<String> antonyms;
    }
}