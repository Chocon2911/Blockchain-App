package Src.Model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Wallet {
    private String publicKey;
    private long createdTimestamp;

    public String getPublicKey() {
        return publicKey;
    }

    public Wallet(String publicKey, long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        this.publicKey = publicKey;
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
