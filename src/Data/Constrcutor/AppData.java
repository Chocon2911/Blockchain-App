package Data.Constrcutor;

import com.google.gson.JsonObject;

public abstract class AppData {
    //==========================================Variable==========================================
    protected String action;
    protected String type;
    protected int value;

    //========================================Constructor=========================================
    public AppData(JsonObject data) {
        this.action = data.get("action").getAsString();
        this.type = data.get("type").getAsString();
        this.value = data.get("value").getAsInt();
    }

    //==========================================Get Set===========================================
    public String getAction() { return action; }
    public String getType() { return type; }
    public int getValue() { return value; }
}
