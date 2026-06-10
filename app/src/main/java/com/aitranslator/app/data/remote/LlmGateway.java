package com.aitranslator.app.data.remote;

import android.util.Log;

import com.aitranslator.app.BuildConfig;
import com.aitranslator.app.data.remote.api.RetrofitClient;
import com.aitranslator.app.data.remote.model.GeminiRequest;
import com.aitranslator.app.data.remote.model.GeminiResponse;
import com.aitranslator.app.data.remote.model.OpenAiRequest;
import com.aitranslator.app.data.remote.model.OpenAiResponse;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Single facade for "generate text via an LLM" with automatic fallback.
 *
 * Tries Gemini 2.5 flash-lite first. If it fails for *any* reason —
 * network error, 4xx (including 429 quota), 5xx, timeout, malformed
 * response — transparently retries via OpenAI gpt-4.1-nano.
 *
 * Consumers see the same {@link GeminiResponse} shape regardless of
 * which provider answered, so existing call sites in AppRepository
 * don't need to know about the fallback at all.
 *
 * Usage:
 * <pre>
 *   LlmGateway.generate(geminiRequest, new Callback<GeminiResponse>() {
 *       &#064;Override public void onResponse(Call<GeminiResponse> call,
 *                                          Response<GeminiResponse> response) { ... }
 *       &#064;Override public void onFailure(Call<GeminiResponse> call, Throwable t) { ... }
 *   });
 * </pre>
 *
 * If both providers fail, the caller's {@code onFailure} fires with the
 * OpenAI throwable (the most recent failure). The Gemini failure is
 * logged but not surfaced to the caller — that's a deliberate choice to
 * keep error handling on the call sites simple.
 */
public final class LlmGateway {

    private static final String TAG = "LlmGateway";

    private LlmGateway() { /* utility class — no instances */ }

    /**
     * Issue an LLM request. Returns the Gemini call so the caller can
     * cancel it if the fragment / activity goes away. If the Gemini
     * call has already completed and we're now in the OpenAI fallback,
     * cancelling the returned Call is a no-op (the OpenAI call runs on
     * its own and there's no way to cancel it from this signature). For
     * normal user flows that's fine — the OpenAI request will complete
     * and the callback will be invoked, but the caller has already moved
     * on so the result is dropped.
     */
    public static Call<GeminiResponse> generate(
            GeminiRequest request,
            final Callback<GeminiResponse> callback) {

        Call<GeminiResponse> geminiCall = RetrofitClient.getInstance()
                .getGeminiApiService()
                .generateContent(BuildConfig.GEMINI_API_KEY, request);

        geminiCall.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                // Treat HTTP errors and empty bodies as "Gemini failed →
                // try OpenAI". The fallback's job is to make the call
                // succeed at all costs, even when Gemini is rate-limited
                // (HTTP 429) or having a bad day (HTTP 5xx).
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "Gemini HTTP " + response.code()
                            + " — falling back to OpenAI");
                    fallbackToOpenAi(request, callback,
                            new IOException("Gemini HTTP " + response.code()));
                    return;
                }
                // Gemini-side responses can be successful HTTP 200 but
                // contain no candidates (e.g. blocked by safety filter).
                // We treat empty content as a failure so the caller still
                // gets a useful answer from OpenAI.
                if (response.body().getTextContent() == null
                        || response.body().getTextContent().isEmpty()) {
                    Log.w(TAG, "Gemini returned empty body — falling back to OpenAI");
                    fallbackToOpenAi(request, callback,
                            new IOException("Gemini returned empty body"));
                    return;
                }
                // Happy path: Gemini answered, pass it straight through.
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                // Network-level failure (timeout, no connectivity, DNS).
                Log.w(TAG, "Gemini onFailure — falling back to OpenAI: " + t.getMessage());
                fallbackToOpenAi(request, callback, t);
            }
        });

        return geminiCall;
    }

    /**
     * Issues the OpenAI fallback request and adapts the response back to
     * the {@link GeminiResponse} shape the caller expects. If OpenAI
     * also fails, the caller sees the OpenAI failure (Gemini's was
     * already logged above).
     */
    private static void fallbackToOpenAi(GeminiRequest geminiRequest,
                                          final Callback<GeminiResponse> callback,
                                          final Throwable geminiError) {

        OpenAiRequest openAiRequest = OpenAiRequest.fromGemini(geminiRequest);

        final Call<OpenAiResponse> openAiCall = RetrofitClient.getInstance()
                .getOpenAiApiService()
                .chatCompletion(
                        "Bearer " + BuildConfig.OPENAI_API_KEY,
                        openAiRequest);

        openAiCall.enqueue(new Callback<OpenAiResponse>() {
            @Override
            public void onResponse(Call<OpenAiResponse> openCall,
                                   Response<OpenAiResponse> openResponse) {
                if (!openResponse.isSuccessful() || openResponse.body() == null) {
                    Log.e(TAG, "OpenAI fallback also failed with HTTP "
                            + openResponse.code());
                    // Synthesise a Retrofit-style failure so the caller's
                    // onFailure path triggers consistently.
                    callback.onFailure(null,
                            new IOException("Both providers failed. "
                                    + "Gemini: " + geminiError.getMessage()
                                    + " · OpenAI HTTP " + openResponse.code()));
                    return;
                }
                // Wrap OpenAI's response in the GeminiResponse shape the
                // rest of the app expects, then deliver via Retrofit's
                // Response.success() so the existing call-site logic
                // (response.body().getTextContent()) works unchanged.
                GeminiResponse adapted = openResponse.body().toGemini();
                Log.i(TAG, "OpenAI fallback succeeded ("
                        + adapted.getTextContent().length() + " chars)");
                callback.onResponse(null, Response.success(adapted));
            }

            @Override
            public void onFailure(Call<OpenAiResponse> openCall, Throwable t) {
                Log.e(TAG, "OpenAI fallback failed: " + t.getMessage());
                callback.onFailure(null, new IOException(
                        "Both providers failed. Gemini: " + geminiError.getMessage()
                                + " · OpenAI: " + t.getMessage(), t));
            }
        });
    }
}
