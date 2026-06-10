package com.aitranslator.app.ui.langpacks;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.*;

/**
 * Manages ML Kit on-device language translation models.
 * Each "language pack" is a downloadable model (~30 MB) that enables
 * offline source ↔ English translation.
 */
public class LanguagePackManager {

    public interface SimpleCallback {
        void onComplete(boolean success, String message);
    }

    public interface CheckCallback {
        void onResult(Set<String> downloadedLanguageCodes);
    }

    public interface TranslateCallback {
        void onSuccess(String translation);
        void onError(String error);
    }

    private final RemoteModelManager modelManager = RemoteModelManager.getInstance();

    /** Returns true if ML Kit supports this language code. */
    public static boolean isSupported(String code) {
        return TranslateLanguage.fromLanguageTag(code) != null;
    }

    /** Lists which language packs are currently downloaded on the device. */
    public void listDownloadedModels(CheckCallback callback) {
        modelManager.getDownloadedModels(TranslateRemoteModel.class)
            .addOnSuccessListener(models -> {
                Set<String> codes = new HashSet<>();
                for (TranslateRemoteModel m : models) codes.add(m.getLanguage());
                callback.onResult(codes);
            })
            .addOnFailureListener(e -> callback.onResult(new HashSet<>()));
    }

    /** Downloads the model for a given language code (e.g. "es", "fr"). */
    public void downloadPack(String langCode, boolean wifiOnly, SimpleCallback callback) {
        String tag = TranslateLanguage.fromLanguageTag(langCode);
        if (tag == null) {
            callback.onComplete(false, "Language not supported by offline pack");
            return;
        }
        TranslateRemoteModel model = new TranslateRemoteModel.Builder(tag).build();
        DownloadConditions.Builder cb = new DownloadConditions.Builder();
        if (wifiOnly) cb.requireWifi();
        modelManager.download(model, cb.build())
            .addOnSuccessListener(v -> callback.onComplete(true, "Downloaded " + langCode))
            .addOnFailureListener(e -> callback.onComplete(false, "Download failed: " + e.getMessage()));
    }

    /** Removes a downloaded model. */
    public void deletePack(String langCode, SimpleCallback callback) {
        String tag = TranslateLanguage.fromLanguageTag(langCode);
        if (tag == null) {
            callback.onComplete(false, "Language not supported");
            return;
        }
        TranslateRemoteModel model = new TranslateRemoteModel.Builder(tag).build();
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener(v -> callback.onComplete(true, "Deleted " + langCode))
            .addOnFailureListener(e -> callback.onComplete(false, "Delete failed: " + e.getMessage()));
    }

    /** Translates fully offline using already-downloaded models. */
    public void translateOffline(String text, String fromCode, String toCode, TranslateCallback callback) {
        String fromTag = TranslateLanguage.fromLanguageTag(fromCode);
        String toTag = TranslateLanguage.fromLanguageTag(toCode);
        if (fromTag == null || toTag == null) {
            callback.onError("Language not supported");
            return;
        }
        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(fromTag)
            .setTargetLanguage(toTag)
            .build();
        Translator translator = Translation.getClient(options);

        // Don't trigger download here — assume models already present
        DownloadConditions noDownload = new DownloadConditions.Builder().requireWifi().build();
        translator.downloadModelIfNeeded(noDownload)
            .addOnSuccessListener(v ->
                translator.translate(text)
                    .addOnSuccessListener(result -> {
                        callback.onSuccess(result);
                        translator.close();
                    })
                    .addOnFailureListener(e -> {
                        callback.onError("Translate failed: " + e.getMessage());
                        translator.close();
                    }))
            .addOnFailureListener(e -> {
                callback.onError("Model not available offline. Download the pack first.");
                translator.close();
            });
    }
}
