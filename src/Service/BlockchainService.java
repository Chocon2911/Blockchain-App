package Service;

import Model.Block;
import com.google.gson.Gson;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.fusesource.leveldbjni.JniDBFactory.*;

public class BlockchainService {
    //==========================================Variable==========================================
    private static final String filePath = "Db/blockchaind.db";
    private static final DB db;
    static {
        try {
            Options options = new Options();
            options.createIfMissing(true);
            db = factory.open(new File(filePath), options);
        } catch (IOException e) {
            throw new RuntimeException("Can't open database", e);
        }
    }

    //===========================================Method===========================================
    public static int getBlockCount() {
        File jsonFile = new File("Db/BlockCount.json");

        try {
            if (!jsonFile.exists()) {
                jsonFile.getParentFile().mkdirs();
                FileWriter writer = new FileWriter(jsonFile);
                writer.write("0");
                writer.close();
                return 0;
            }

            String content = new String(Files.readAllBytes(Paths.get("Db/BlockCount.json")));
            return Integer.parseInt(content.trim());

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void increaseBlockCount() {
        File jsonFile = new File("Db/BlockCount.json");

        try {
            int currentCount = getBlockCount();
            if (currentCount == -1) return;

            FileWriter writer = new FileWriter(jsonFile, false);
            writer.write(String.valueOf(currentCount + 1));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Block getBlock(int index) {
        String key = indexToKey(index);
        byte[] value = db.get(bytes(key));

        if (value == null) return null;
        return getBlockFromJson(asString(value));
    }

    public static void addBlock(Block block) {
        try {
            int index = getBlockCount();
            String key = indexToKey(index);
            String json = new Gson().toJson(block);
            db.put(bytes(key), bytes(json));
            increaseBlockCount();
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm block: " + e.getMessage());
        }
    }

    public static Block getBlockFromJson(String json) {
        return new Gson().fromJson(json, Block.class);
    }

    public static String getJsonFromBlock(Block block) {
        return new Gson().toJson(block);
    }

    private static String indexToKey(int index) {
        return String.format("%06d", index);
    }
}
