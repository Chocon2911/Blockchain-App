package Main;

import Model.Block;
import Model.Blockchain;
import Model.Miner;
import Model.Wallet;
import UI.MainUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class App {
    public static void main(String[] args) {
        MainUI mainUI = new MainUI();
        mainUI.display();
    }

    public static Blockchain initBlockchain() {
        String filePath = "Src/Data/Blockchain.json";
        File file = new File(filePath);

        try {
            if (file.exists()) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                FileReader reader = new FileReader(file);
                Blockchain blockchain = gson.fromJson(reader, Blockchain.class);
                reader.close();

                if (blockchain == null) {
                    System.out.println("initBlockchain(): Not good");
                    return null;
                }
                return blockchain;
            }
            else {
                int index = 0;
                String version = "0.0.0.1";
                String merkleRoot = "0000000000000000000000000000000000000000000000000000000000000000";
                String previousHash = "";
                int difficulty = 4;

                Block firstBlock = new Block(index, version, previousHash, null, difficulty);
                Blockchain blockchain = new Blockchain(firstBlock, difficulty, version, merkleRoot);
                File parentDir = new File(file.getParent());

                if (!parentDir.exists()) parentDir.mkdirs();
                FileWriter writer = new FileWriter(file);
                String json = blockchain.toJson();
                writer.write(json);
                writer.close();
                return blockchain;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("initBlockchain(): Not good");
        return null;
    }

    public static Wallet initWallet() {
        String filePath = "Src/Data/Wallet.json";
        File file = new File(filePath);
        try {
            if (file.exists()) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                FileReader reader = new FileReader(file);
                Wallet wallet = gson.fromJson(reader, Wallet.class);
                reader.close();
                if (wallet == null) {
                    System.out.println("initWallet(): Not good");
                    return null;
                }
                return wallet;
            } else {
                String publicKey = "TestKey";
                long createdTimestamp = System.currentTimeMillis();
                Wallet wallet = new Wallet(publicKey, createdTimestamp);

                File parentDir = new File(file.getParent());
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                FileWriter writer = new FileWriter(file);
                String json = wallet.toJson();
                writer.write(json);
                writer.close();

                return wallet;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("initWallet(): Not good");
        return null;
    }
}
