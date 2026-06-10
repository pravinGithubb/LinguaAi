package com.aitranslator.app.data.remote.model;
import java.util.List;

public class TranslateResponse {
    public Data data;

    public String getTranslatedText() {
        if (data != null && data.translations != null && !data.translations.isEmpty())
            return data.translations.get(0).translatedText;
        return "";
    }

    public static class Data {
        public List<Translation> translations;
    }

    public static class Translation {
        public String translatedText;
        public String detectedSourceLanguage;
    }
}