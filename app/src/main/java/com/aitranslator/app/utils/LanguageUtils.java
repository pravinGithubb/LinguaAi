package com.aitranslator.app.utils;
import java.util.LinkedHashMap;
import java.util.Map;

public class LanguageUtils {
    public static final Map<String, String> LANGUAGES = new LinkedHashMap<>();

    static {
        LANGUAGES.put("English", "en"); LANGUAGES.put("Spanish", "es");
        LANGUAGES.put("French", "fr"); LANGUAGES.put("German", "de");
        LANGUAGES.put("Italian", "it"); LANGUAGES.put("Portuguese", "pt");
        LANGUAGES.put("Russian", "ru"); LANGUAGES.put("Japanese", "ja");
        LANGUAGES.put("Korean", "ko"); LANGUAGES.put("Chinese (Simplified)", "zh");
        LANGUAGES.put("Arabic", "ar"); LANGUAGES.put("Hindi", "hi");
        LANGUAGES.put("Turkish", "tr"); LANGUAGES.put("Dutch", "nl");
        LANGUAGES.put("Polish", "pl"); LANGUAGES.put("Swedish", "sv");
        LANGUAGES.put("Norwegian", "no"); LANGUAGES.put("Danish", "da");
        LANGUAGES.put("Finnish", "fi"); LANGUAGES.put("Greek", "el");
        LANGUAGES.put("Hebrew", "he"); LANGUAGES.put("Thai", "th");
        LANGUAGES.put("Vietnamese", "vi"); LANGUAGES.put("Indonesian", "id");
    }

    public static String[] getLanguageNames() { return LANGUAGES.keySet().toArray(new String[0]); }
    public static String getCode(String name) { return LANGUAGES.getOrDefault(name, "en"); }

    public static String getName(String code) {
        for (Map.Entry<String, String> e : LANGUAGES.entrySet())
            if (e.getValue().equals(code)) return e.getKey();
        return "English";
    }

    public static String getFlagEmoji(String code) {
        switch (code) {
            case "en": return "🇺🇸"; case "es": return "🇪🇸"; case "fr": return "🇫🇷";
            case "de": return "🇩🇪"; case "it": return "🇮🇹"; case "pt": return "🇧🇷";
            case "ru": return "🇷🇺"; case "ja": return "🇯🇵"; case "ko": return "🇰🇷";
            case "zh": return "🇨🇳"; case "ar": return "🇸🇦"; case "hi": return "🇮🇳";
            case "tr": return "🇹🇷"; case "nl": return "🇳🇱"; default: return "🌐";
        }
    }
}