package com.aitranslator.app.utils;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link LanguageUtils}.
 *
 * Validates the language map, code lookup, name lookup, and Hindi
 * inclusion. Runs on the JVM — no Android runtime needed.
 */
public class LanguageUtilsTest {

    @Test
    public void languageMapIsNotEmpty() {
        assertFalse("LANGUAGES map must not be empty",
                LanguageUtils.LANGUAGES.isEmpty());
    }

    @Test
    public void getLanguageNamesReturnsNonEmptyArray() {
        String[] names = LanguageUtils.getLanguageNames();
        assertNotNull(names);
        assertTrue("Must have at least one language", names.length > 0);
    }

    @Test
    public void englishIsPresentWithCodeEn() {
        assertEquals("en", LanguageUtils.getCode("English"));
    }

    @Test
    public void spanishIsPresentWithCodeEs() {
        assertEquals("es", LanguageUtils.getCode("Spanish"));
    }

    @Test
    public void hindiIsPresentWithCodeHi() {
        // Critical for Speaking Quiz to dispatch the Hindi corpus
        assertEquals("hi", LanguageUtils.getCode("Hindi"));
    }

    @Test
    public void getCode_UnknownName_ReturnsEnFallback() {
        assertEquals("en", LanguageUtils.getCode("Klingon"));
    }

    @Test
    public void getCode_NullName_ReturnsEnFallback() {
        // getOrDefault with null key — must not crash
        String code = LanguageUtils.getCode(null);
        assertNotNull(code);
        assertEquals("en", code);
    }

    @Test
    public void getName_EnglishCode_ReturnsEnglish() {
        assertEquals("English", LanguageUtils.getName("en"));
    }

    @Test
    public void getName_HindiCode_ReturnsHindi() {
        assertEquals("Hindi", LanguageUtils.getName("hi"));
    }

    @Test
    public void getName_UnknownCode_ReturnsEnglishFallback() {
        assertEquals("English", LanguageUtils.getName("zz"));
    }

    @Test
    public void getCodeAndGetName_AreInverses() {
        // For every language in the map, getCode(getName(code)) == code
        for (java.util.Map.Entry<String, String> entry :
                LanguageUtils.LANGUAGES.entrySet()) {
            String name = entry.getKey();
            String code = entry.getValue();
            assertEquals("getCode(getName(code)) must round-trip",
                    code, LanguageUtils.getCode(LanguageUtils.getName(code)));
            assertEquals("getName(getCode(name)) must round-trip",
                    name, LanguageUtils.getName(LanguageUtils.getCode(name)));
        }
    }

    @Test
    public void allLanguageCodesAre2or3Characters() {
        for (java.util.Map.Entry<String, String> entry :
                LanguageUtils.LANGUAGES.entrySet()) {
            String code = entry.getValue();
            assertTrue("Language code must be 2 or 3 chars: " + code,
                    code.length() == 2 || code.length() == 3);
        }
    }

    @Test
    public void allLanguageNamesAreNonEmpty() {
        for (String name : LanguageUtils.getLanguageNames()) {
            assertNotNull(name);
            assertFalse("Language name must not be empty", name.trim().isEmpty());
        }
    }

    @Test
    public void languageCountIsAtLeast20() {
        // The app ships with 24 languages as of v25
        assertTrue("Expected at least 20 languages",
                LanguageUtils.LANGUAGES.size() >= 20);
    }
}
