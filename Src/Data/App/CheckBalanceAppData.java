package Data.App;

import Data.Constrcutor.AppData;
import com.google.gson.JsonObject;

public class CheckBalanceAppData extends AppData {
    private String publicAddress;

    public CheckBalanceAppData(JsonObject data) {
        super(data);
        this.publicAddress = data.get("publicAddress").getAsString();
    }

    public String getPublicAddress() { return publicAddress; }
}
