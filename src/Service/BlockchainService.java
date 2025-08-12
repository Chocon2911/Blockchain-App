package Service;

import Main.Util;
import Model.Block;
import Model.Transaction;
import com.google.gson.Gson;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.fusesource.leveldbjni.JniDBFactory.*;

public class BlockchainService {
    //==========================================Variable==========================================
    private static final String filePath = "Db/blockchaind.db";
    public static final String version = "0.0.1";
    private static DB db;
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

    public static Block getLastBlock() {
        return getBlock(getBlockCount() - 1);
    }

    public static void addBlock(Block block) {
        try {
            addBlockDB(block);
            increaseBlockCount();
        } catch (Exception e) {
            System.err.println("Error adding block: " + e.getMessage());
        }
    }

    public static Block createNewBlock(List<Transaction> transactions) {
        Block block = BlockchainService.getBlock(BlockchainService.getBlockCount() - 1);
        int index = block.getIndex() + 1;
        String version = Util.version;
        String previousHash = block.getHash();
        BigInteger previousNChainWork = block.getNChainWork();
        int difficulty = block.getDifficulty();

        Block newBlock = new Block(index, version, previousHash, previousNChainWork, difficulty, transactions);
        return newBlock;
    }

    public static boolean examineBlock(Block block) {
        Block lastBlock = getLastBlock();

        // 1. Kiểm tra index
        if (block.getIndex() != lastBlock.getIndex() + 1) {
            System.out.println("Invalid index");
            return false;
        }

        // 2. Kiểm tra previous hash
        if (!block.getPreviousHash().equals(lastBlock.getHash())) {
            System.out.println("Previous hash mismatch");
            return false;
        }

        // 3. Kiểm tra Merkle Root
        String expectedMerkle = block.calculateMerkleTree();
        List<Transaction> txs = block.getTransactions();
        if (!expectedMerkle.equals(block.calculateMerkleTree())) {
            System.out.println("Merkle root mismatch");
            return false;
        }

        // 4. Kiểm tra timestamp (phải lớn hơn block trước và không vượt quá hiện tại + 2h)
        long now = System.currentTimeMillis() / 1000L;
        if (block.getTimestamp() <= lastBlock.getTimestamp()) {
            System.out.println("Invalid timestamp (too early)");
            return false;
        }
        if (block.getTimestamp() > now + 2 * 60 * 60) {
            System.out.println("Invalid timestamp (too far in future)");
            return false;
        }

        // 5. Kiểm tra difficulty và hash
        BigInteger target = new BigInteger(block.getHash(), 16);
        if (target.compareTo(block.getTarget()) > 0) {
            System.out.println("Hash does not meet difficulty target");
            return false;
        }

        // 6. Kiểm tra NChainWork
        if (!block.getNChainWork().equals(lastBlock.getNChainWork().add(
                BigInteger.ONE.shiftLeft(256).divide(block.getTarget().add(BigInteger.ONE))))) {
            System.out.println("Invalid chain work");
            return false;
        }

        // 7. Kiểm tra giao dịch (coinbase + valid signatures)
        if (txs.isEmpty()) {
            System.out.println("No transactions");
            return false;
        }
        // Coinbase transaction check (simplified)
        if (!txs.get(0).isCoinbase(block.getReward())) {
            System.out.println("Invalid coinbase transaction");
            return false;
        }
        for (int i = 1; i < txs.size(); i++) {
            if (!TransactionService.validateTransaction(txs.get(i))) {
                System.out.println("Invalid transaction found");
                return false;
            }
        }

        return true;
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

    //=============================================Db=============================================
    public static void initDB() throws IOException {
        Options options = new Options();
        options.createIfMissing(true);
        db = factory.open(new File(filePath), options);
    }

    public static void closeDB() throws IOException {
        db.close();
    }

    public static void addBlockDB(Block block) {
        String key = String.valueOf(block.getIndex());
        String json = getJsonFromBlock(block);
        db.put(bytes(key), bytes(json));
    }

    public static Block getBlockDB(int index) {
        String key = String.valueOf(index);
        byte[] data = db.get(bytes(key));
        if (data == null) return null;
        return getBlockFromJson(asString(data));
    }
}
