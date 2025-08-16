package Data.App;

import Data.Constrcutor.AppData;
import Model.Block;
import Service.BlockchainService;
import com.google.gson.JsonObject;

public class CreateBlockAppData extends AppData {
    private int threadAmount;
    private Block block;

    public CreateBlockAppData(JsonObject data) {
        super(data);
        this.threadAmount = data.get("threadAmount").getAsInt();
        // Get list txId
    }

    public int getThreadAmount() { return threadAmount; }
    public Block getBlock() { return block; }
}
