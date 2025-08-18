package Data.Constrcutor;

import com.google.gson.JsonObject;

public abstract class AppData {
    //==========================================Variable==========================================
    protected String action;

    //========================================Constructor=========================================
    public AppData(JsonObject data) {
        this.action = data.get("action").getAsString();
    }

    //==========================================Get Set===========================================
    public String getAction() { return action; }
}
