package android.example.dex.db;

import android.content.Context;
import android.example.dex.db.dao.CollectionDao;
import android.example.dex.db.dao.SetDao;
import android.example.dex.db.dao.WishDao;
import android.example.dex.db.entity.pokemon.Pokemon;
import android.example.dex.db.entity.pokemon.Prices;
import android.example.dex.db.entity.set.PokeSet;
import android.example.dex.db.api.PokeResponse;
import android.example.dex.db.api.PokeService;
import android.example.dex.db.api.PokeSetResponse;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.example.dex.utilities.PokeUtil.getHighestPrice;

// Usually, you only need one instance of a Room database for the whole app.
@Database(entities = {Pokemon.class, PokeSet.class}, version = 1, exportSchema = false)
public abstract class PokemonRoomDatabase extends RoomDatabase {

    private static final String TAG = "PokemonRoomDatabase";
    public abstract CollectionDao collectionDao();
    public abstract SetDao setDao();
    public abstract WishDao wishDao();

    // Singleton, to prevent having multiple instances of the database opened at the same time.
    private static volatile PokemonRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    // We need to not run the insert on the main thread,
    // so we use the ExecutorService we created in the WordRoomDatabase to perform the insert on a background thread.
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Returns the singleton
    // It'll create the database the first time it's accessed, using Room's database builder to create a RoomDatabase object in
    // the application context from the WordRoomDatabase class and names it "pokemon_database"
    public static PokemonRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (PokemonRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            PokemonRoomDatabase.class, "pokemon_database")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Override the onCreate method to populate the database.
     */
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull @NotNull SupportSQLiteDatabase db) {
        super.onCreate(db);
        databaseWriteExecutor.execute(() -> {
            CollectionDao mCollectionDao = INSTANCE.collectionDao();
            SetDao mSetDao = INSTANCE.setDao();

            String BASE_URL = "https://api.pokemontcg.io/v2/";
            String API_KEY = "19118357-6a69-4cc5-8b9e-02ccf48daf44";

            // Interceptor
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(chain -> {
                Request request = chain.request().newBuilder().addHeader("X-Api-Key", API_KEY).build();
                return chain.proceed(request);
            });

            // Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(BASE_URL)
                    .client(httpClient.build())
                    .build();

            // Create pokemon service
            PokeService service = retrofit.create(PokeService.class);

            int pageNum = 1;
            int totalSeen = 0;
            int totalCount = 13685;
            while (totalSeen < totalCount) {
                // Get pokemon data
                Call<PokeResponse> call = service.getPokemonByPage(pageNum);
                call.enqueue(new retrofit2.Callback<PokeResponse>() {
                    @Override
                    public void onResponse(Call<PokeResponse> call, retrofit2.Response<PokeResponse> response) {
                        PokeResponse pokeResponse = response.body();
                        if (pokeResponse != null) {
                            Log.d(TAG, "onSuccess: Getting data");
                            List<Pokemon> pokeData = pokeResponse.getPokemons();
                            for (Pokemon pokemon : pokeData) {
                                databaseWriteExecutor.execute(() -> {
                                    // Update card_number
                                    pokemon.setCard_number(pokemon.getNumber());
                                    if (pokemon.getTcgplayer() != null) {
                                        pokemon.getTcgplayer().getPrices().setHighestPrice(getHighestPrice(pokemon));
                                    }
                                    mCollectionDao.insert(pokemon);
                                });
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PokeResponse> call, Throwable t) {
                        Log.d(TAG, "onFailure: Fail to get data", t);
                    }
                });
                pageNum += 1;
                totalSeen += 250;
                Log.d(TAG, "Total Seen: " + totalSeen);
            }

            // Create pokemon service
            service = retrofit.create(PokeService.class);
            // Get set data
            Call<PokeSetResponse> callSet = service.getSets();

            callSet.enqueue(new retrofit2.Callback<PokeSetResponse>() {
                @Override
                public void onResponse(Call<PokeSetResponse> call, retrofit2.Response<PokeSetResponse> response) {
                    PokeSetResponse pokeSetResponse = response.body();
                    if (pokeSetResponse != null) {
                        List<PokeSet> pokeSetData = pokeSetResponse.getPokeSets();
                        Log.d(TAG, "Getting Set data");
                        // TODO: Maybe insert a list instead of 1 by 1
                        for (PokeSet pokeSet : pokeSetData) {
                            PokemonRoomDatabase.databaseWriteExecutor.execute(() -> {
                                mSetDao.insert(pokeSet);
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<PokeSetResponse> call, Throwable t) {
                    Log.d(TAG, "onFailure", t);
                }
            });
        });
        }
    };

}
