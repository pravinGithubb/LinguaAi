package com.aitranslator.app.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Wraps Android's SpeechRecognizer with a clean callback interface.
 * Uses the device's built-in STT engine — completely FREE, no API cost.
 */
public class SpeechRecognitionManager {

    private static final String TAG = "SpeechRecognition";

    public interface SpeechCallback {
        void onReadyForSpeech();        // mic open, listening started
        void onSpeechDetected();        // user started talking
        void onPartialResult(String partial);  // live partial text
        void onResult(String text);     // final recognized text
        void onError(String message);   // something went wrong
        void onEndOfSpeech();           // user stopped talking
    }

    private SpeechRecognizer recognizer;
    private final Context context;
    private SpeechCallback callback;
    private boolean isListening = false;

    public SpeechRecognitionManager(Context context) {
        this.context = context;
    }

    public static boolean isAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public void startListening(String languageCode, SpeechCallback callback) {
        this.callback = callback;

        if (recognizer != null) {
            recognizer.destroy();
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                if (callback != null) callback.onReadyForSpeech();
            }

            @Override
            public void onBeginningOfSpeech() {
                if (callback != null) callback.onSpeechDetected();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Could use this to animate waveform height based on volume
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                if (callback != null) callback.onEndOfSpeech();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                String msg = getErrorMessage(error);
                Log.e(TAG, "STT Error: " + msg);
                if (callback != null) callback.onError(msg);
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    if (callback != null) callback.onResult(matches.get(0));
                } else {
                    if (callback != null) callback.onError("No speech detected. Please try again.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    if (callback != null) callback.onPartialResult(partial.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // Build the recognition intent
        Locale locale = getLocaleForCode(languageCode);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);

        recognizer.startListening(intent);
    }

    public void stopListening() {
        if (recognizer != null && isListening) {
            recognizer.stopListening();
            isListening = false;
        }
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        isListening = false;
    }

    public boolean isListening() { return isListening; }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Microphone permission denied";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error — check connection";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No speech match — speak clearly and try again";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech detected — try speaking louder";
            default: return "Recognition error — please try again";
        }
    }

    private Locale getLocaleForCode(String code) {
        if (code == null) return Locale.ENGLISH;
        switch (code) {
            case "zh": return Locale.SIMPLIFIED_CHINESE;
            case "ja": return Locale.JAPANESE;
            case "ko": return Locale.KOREAN;
            case "fr": return Locale.FRENCH;
            case "de": return Locale.GERMAN;
            case "it": return Locale.ITALIAN;
            case "pt": return new Locale("pt", "BR");
            case "es": return new Locale("es", "ES");
            case "hi": return new Locale("hi", "IN");
            case "ar": return new Locale("ar");
            case "ru": return new Locale("ru");
            case "tr": return new Locale("tr");
            default:
                try { return new Locale(code); }
                catch (Exception e) { return Locale.ENGLISH; }
        }
    }
}
