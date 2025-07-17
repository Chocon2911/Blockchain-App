package Src.Main;

import Src.Model.Blockchain;
import Src.Model.Wallet;

import java.io.File;
import java.io.FileWriter;

public class Util {
    //==========================================Variable==========================================
    private static Util instance = new Util();
    public static Util getInstance() { return instance; }

    //========================================Constructor=========================================
    public Util() {}

    //===========================================Method===========================================
    public void SaveJsonFile(String filePath, String json) {
        File file = new File(filePath);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SaveWalletJson(Wallet wallet) {
        String filePath = "Src/Data/Wallet.json";
        String json = wallet.toJson();
        SaveJsonFile(filePath, json);
    }

    public void SaveBlockchainJson(Blockchain blockchain) {
        String filePath = "Src/Data/Blockchain.json";
        String json = blockchain.toJson();
        SaveJsonFile(filePath, json);
    }
}
