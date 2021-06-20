package android.example.dex.api;

import android.example.dex.api.PokeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PokeService {

    // Recall: @Query are like parameters
    @GET("cards")
    Call<PokeResponse> getPokemons(@Query("q") String query);

    @GET("sets")
    Call<PokeSetResponse> getSets();
}