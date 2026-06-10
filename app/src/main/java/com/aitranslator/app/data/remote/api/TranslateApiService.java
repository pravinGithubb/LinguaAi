package com.aitranslator.app.data.remote.api;
import com.aitranslator.app.data.remote.model.TranslateResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface TranslateApiService {
    @GET("language/translate/v2")
    Call<TranslateResponse> translate(
            @Query("q") String text,
            @Query("source") String sourceLang,
            @Query("target") String targetLang,
            @Query("key") String apiKey,
            @Query("format") String format);
}