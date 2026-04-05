package com.zedox.meshtalk.ai;

import android.content.Context;
import android.util.Log;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * On-Device AI Translation Service for MeshTalk
 * Uses Google ML Kit to translate messages without internet
 * Supports 50+ languages - all processing happens on device
 * Team ZEDOX - Imagine Cup 2025
 */
public class TranslationService {

    private static final String TAG = "TranslationService";

    /** Cache translators so models aren't re-created for every message */
    private final Map<String, Translator> translatorCache = new HashMap<>();
    private final Context context;

    public TranslationService(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Translate text from one language to another.
     * The ML Kit model is downloaded on first use and cached on-device.
     *
     * @param text         Source text
     * @param sourceLang   BCP-47 language code, e.g. "en", "es", "hi"
     * @param targetLang   BCP-47 language code
     * @param callback     Receives translated text or the original on error
     */
    public void translate(String text, String sourceLang, String targetLang,
                          TranslationCallback callback) {
        if (sourceLang.equals(targetLang)) {
            callback.onSuccess(text);
            return;
        }

        String cacheKey = sourceLang + "_" + targetLang;
        Translator translator = translatorCache.get(cacheKey);

        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            translator = Translation.getClient(options);
            translatorCache.put(cacheKey, translator);
        }

        final Translator finalTranslator = translator;
        finalTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(unused ->
                        finalTranslator.translate(text)
                                .addOnSuccessListener(translatedText -> {
                                    Log.d(TAG, "Translated [" + sourceLang + "→" + targetLang + "]: " + translatedText);
                                    callback.onSuccess(translatedText);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Translation failed", e);
                                    callback.onFailure(e);
                                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Detect the language of a text snippet.
     * Falls back to "en" (English) if detection fails.
     *
     * @param text     Text to identify
     * @param callback Receives BCP-47 language code string
     */
    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        com.google.mlkit.nl.languageid.LanguageIdentification
                .getClient()
                .identifyLanguage(text)
                .addOnSuccessListener(langCode -> {
                    if ("und".equals(langCode)) {
                        callback.onDetected("en");
                    } else {
                        callback.onDetected(langCode);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Language detection failed, defaulting to 'en'", e);
                    callback.onDetected("en");
                });
    }

    /** Release all cached translator resources. Call from onDestroy(). */
    public void close() {
        for (Translator t : translatorCache.values()) {
            t.close();
        }
        translatorCache.clear();
    }

    // -------------------------------------------------------------------------
    // Callback interfaces
    // -------------------------------------------------------------------------

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(Exception e);
    }

    public interface LanguageDetectionCallback {
        void onDetected(String languageCode);
    }
}
