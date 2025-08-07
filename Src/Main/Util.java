package Src.Main;

import Src.Model.Blockchain;
import Src.Model.Wallet;

import java.io.File;
import java.io.FileWriter;
import java.security.MessageDigest;

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

    public String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
