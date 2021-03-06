package android.example.dex.db.entity.pokemon;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class TCGPlayer {

    @SerializedName("url")
    public String url;

    @Embedded
    @SerializedName("prices")
    public Prices prices;

    public String updatedAt;

    public TCGPlayer() {}

    public String getUrl() {
        return url;
    }

    public Prices getPrices() {
        return prices;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }


}
