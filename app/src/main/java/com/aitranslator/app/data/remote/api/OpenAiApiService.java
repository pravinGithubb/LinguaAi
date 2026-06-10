package com.aitranslator.app.data.remote.api;

import com.aitranslator.app.data.remote.model.OpenAiRequest;
import com.aitranslator.app.data.remote.model.OpenAiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * OpenAI Chat Completions API.
 *
 * Used as a fallback by {@link com.aitranslator.app.data.remote.LlmGateway}
 * when Gemini fails (network, 429 rate-limit, 5xx, timeout). The endpoint
 * is hard-coded to GPT-4.1-nano in the request body — see
 * {@link OpenAiRequest#defaultModel()}.
 *
 * Authentication uses a Bearer token in the Authorization header (NOT a
 * query-param like Gemini), so the key is passed via @Header.
 */
public interface OpenAiApiService {

    @POST("v1/chat/completions")
    Call<OpenAiResponse> chatCompletion(
            @Header("Authorization") String bearerKey,
            @Body OpenAiRequest request);
}
