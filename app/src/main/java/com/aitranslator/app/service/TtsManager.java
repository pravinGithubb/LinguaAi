package com.aitranslator.app.service;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TtsManager {
    private static final String TAG = "TtsManager";
    private TextToSpeech tts;
    private boolean isReady = false;

    public interface TtsCallback { void onReady(); void onError(String msg); }

    public TtsManager(Context context, TtsCallback callback) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) { isReady = true; if (callback != null) callback.onReady(); }
            else { if (callback != null) callback.onError("TTS init failed"); }
        });
    }

    public void speak(String text, String langCode) {
        if (!isReady || text == null || text.isEmpty()) return;
        Locale locale = getLocale(langCode);
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS language not supported: " + langCode);
            tts.setLanguage(Locale.ENGLISH);
        }
        tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt_" + System.currentTimeMillis());
    }

    public void stop() { if (tts != null) tts.stop(); }
    public void shutdown() { if (tts != null) { tts.stop(); tts.shutdown(); isReady = false; } }
    public boolean isReady() { return isReady; }

    private Locale getLocale(String code) {
        if (code == null) return Locale.ENGLISH;
        switch (code) {
            case "zh": return Locale.CHINESE; case "ja": return Locale.JAPANESE;
            case "ko": return Locale.KOREAN; case "fr": return Locale.FRENCH;
            case "de": return Locale.GERMAN; case "it": return Locale.ITALIAN;
            default: try { return new Locale(code); } catch (Exception e) { return Locale.ENGLISH; }
        }
    }
}