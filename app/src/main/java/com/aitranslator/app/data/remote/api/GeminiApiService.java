package com.aitranslator.app.data.remote.api;
import com.aitranslator.app.data.remote.model.GeminiRequest;
import com.aitranslator.app.data.remote.model.GeminiResponse;
import retrofit2.Call;
import retrofit2.http.*;

public interface GeminiApiService {
    // Model: gemini-2.5-flash-lite (cheaper tier — ~$0.10/$0.40 per 1M
    // tokens vs Flash's $0.15/$0.60). LlmGateway falls back to OpenAI
    // gpt-4.1-nano on any failure.
    @POST("v1beta/models/gemini-2.5-flash-lite:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request);
}