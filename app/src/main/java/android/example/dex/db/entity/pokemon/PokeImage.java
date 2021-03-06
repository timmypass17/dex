package android.example.dex.db.entity.pokemon;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

@Parcel
public class PokeImage {

    public PokeImage(){}

    @SerializedName("small")
    public String smallImage;

    @SerializedName("large")
    public String largeImage;

    public String getSmallImage() {
        return smallImage;
    }

    public String getLargeImage() {
        return largeImage;
    }
}
