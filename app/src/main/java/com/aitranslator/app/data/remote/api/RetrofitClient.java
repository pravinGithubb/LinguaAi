package com.aitranslator.app.data.remote.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Singleton holder for every Retrofit-built API service used by the app.
 *
 * Adds OpenAI alongside Gemini / Google Translate / dictionary so the
 * {@link com.aitranslator.app.data.remote.LlmGateway} can transparently
 * fall back from Gemini to OpenAI when Gemini fails.
 */
public class RetrofitClient {

    private static final String GEMINI_BASE_URL     = "https://generativelanguage.googleapis.com/";
    private static final String TRANSLATE_BASE_URL  = "https://translation.googleapis.com/";
    private static final String DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/";
    private static final String OPENAI_BASE_URL     = "https://api.openai.com/";

    private static RetrofitClient instance;

    private final GeminiApiService     geminiApiService;
    private final TranslateApiService  translateApiService;
    private final DictionaryApiService dictionaryApiService;
    private final OpenAiApiService     openAiApiService;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        geminiApiService = new Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(GeminiApiService.class);

        translateApiService = new Retrofit.Builder()
                .baseUrl(TRANSLATE_BASE_URL).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(TranslateApiService.class);

        dictionaryApiService = new Retrofit.Builder()
                .baseUrl(DICTIONARY_BASE_URL).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(DictionaryApiService.class);

        openAiApiService = new Retrofit.Builder()
                .baseUrl(OPENAI_BASE_URL).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(OpenAiApiService.class);
    }

    public static RetrofitClient getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) instance = new RetrofitClient();
            }
        }
        return instance;
    }

    public GeminiApiService     getGeminiApiService()     { return geminiApiService; }
    public TranslateApiService  getTranslateApiService()  { return translateApiService; }
    public DictionaryApiService getDictionaryApiService() { return dictionaryApiService; }
    public OpenAiApiService     getOpenAiApiService()     { return openAiApiService; }
}
