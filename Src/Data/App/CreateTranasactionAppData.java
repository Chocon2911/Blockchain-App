package Data.App;

import Data.Constrcutor.AppData;
import Service.TransactionService;
import com.google.gson.JsonObject;

import java.security.PrivateKey;
import java.security.PublicKey;

public class CreateTranasactionAppData extends AppData {
    //==========================================Variable==========================================
    private String privateKeySender;
    private String publicKeySender;
    private String publicAddressReceiver;
    private int amount;
    private int fee;

    //========================================Constructor=========================================
    public CreateTranasactionAppData(JsonObject data) {
        super(data);
        this.privateKeySender = data.get("privateKeySender").getAsString();
        this.publicKeySender = data.get("publicKeySender").getAsString();
        this.publicAddressReceiver = data.get("publicAddressReceiver").getAsString();
        this.amount = data.get("amount").getAsInt();
        this.fee = data.get("fee").getAsInt();
    }

    //==========================================Get Set===========================================
    public String getPrivateKeySender() { return privateKeySender; }
    public String getPublicKeySender() { return publicKeySender; }
    public String getPublicAddressReceiver() { return publicAddressReceiver; }
    public int getAmount() { return amount; }
    public int getFee() { return fee; }

    public PublicKey getPublicKeySenderAdapter() { return TransactionService.getPublicKey(publicKeySender); }
    public PrivateKey getPrivateKeySenderAdapter() { return TransactionService.getPrivateKey(privateKeySender); }
}
