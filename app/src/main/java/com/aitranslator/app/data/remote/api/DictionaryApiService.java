package com.aitranslator.app.data.remote.api;
import com.aitranslator.app.data.remote.model.DictionaryResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

// 100% FREE — no API key needed
// Supports: English (en), Spanish (es), French (fr), German (de),
//           Italian (it), Portuguese (pt), Hindi (hi), Japanese (ja) and more
public interface DictionaryApiService {
    @GET("api/v2/entries/{language}/{word}")
    Call<List<DictionaryResponse>> lookup(
            @Path("language") String language,
            @Path("word") String word);
}