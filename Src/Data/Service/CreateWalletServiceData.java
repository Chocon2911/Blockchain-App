package Data.Service;

import Data.Constrcutor.ServiceData;
import Model.Wallet;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class CreateWalletServiceData extends ServiceData {
    //==========================================Variable==========================================
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final String publicAddress;

    //========================================Json Adapter========================================
    // ===== Adapter cho PrivateKey =====
    private static class PrivateKeyAdapter implements JsonSerializer<PrivateKey>, JsonDeserializer<PrivateKey> {
        @Override
        public JsonElement serialize(PrivateKey src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }

        @Override
        public PrivateKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                byte[] decoded = Base64.getDecoder().decode(json.getAsString());
                java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(decoded);
                java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("EC"); // 🔥 sửa RSA -> EC
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }

    // ===== Adapter cho PublicKey =====
    private static class PublicKeyAdapter implements JsonSerializer<PublicKey>, JsonDeserializer<PublicKey> {
        @Override
        public JsonElement serialize(PublicKey src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()));
        }

        @Override
        public PublicKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            try {
                byte[] decoded = Base64.getDecoder().decode(json.getAsString());
                java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(decoded);
                java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("EC"); // 🔥 sửa RSA -> EC
                return keyFactory.generatePublic(keySpec);
            } catch (Exception e) {
                throw new JsonParseException(e);
            }
        }
    }


    // ===== Define Gson =====
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(PrivateKey.class, new PrivateKeyAdapter())
            .registerTypeAdapter(PublicKey.class, new PublicKeyAdapter())
            .create();

    //========================================Constructor=========================================
    public CreateWalletServiceData(Wallet wallet) {
        this.action = "create_wallet";
        this.privateKey = wallet.getPrivateKey();
        this.publicKey = wallet.getPublicKey();
        try {
            this.publicAddress = wallet.getAddress();
        } catch (Exception e) {
            System.out.println("ERROR getting address");
            throw new RuntimeException(e);
        }
    }

    //============================================Json============================================
    public String toJson() {
        return gson.toJson(this);
    }

    public static CreateWalletServiceData fromJson(String json) {
        return gson.fromJson(json, CreateWalletServiceData.class);
    }
}
