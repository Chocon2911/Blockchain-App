package Service;

import Model.*;
import com.google.gson.*;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.fusesource.leveldbjni.JniDBFactory.*;

public class BlockchainService {
    //==========================================Variable==========================================
    private static final String filePath = "Db/blockchaind.db";
    public static final String version = "0.0.1";
    private static DB db;
    public static final Gson gson = new GsonBuilder()
            // byte[] <-> Base64
            .registerTypeHierarchyAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(Base64.getEncoder().encodeToString(src))
            )
            .registerTypeHierarchyAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) ->
                    Base64.getDecoder().decode(json.getAsString())
            )
            // PublicKey <-> Base64
            .registerTypeHierarchyAdapter(PublicKey.class, (JsonSerializer<PublicKey>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()))
            )
            .registerTypeHierarchyAdapter(PublicKey.class, (JsonDeserializer<PublicKey>) (json, typeOfT, context) -> {
                try {
                    byte[] bytes = Base64.getDecoder().decode(json.getAsString());
                    KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Hoặc "RSA" nếu bạn dùng RSA
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                    return keyFactory.generatePublic(spec);
                } catch (Exception e) {
                    throw new JsonParseException(e);
                }
            })
            .create();
    static {
        try {
            Options options = new Options();
            options.createIfMissing(true);
            db = factory.open(new File(filePath), options);

            // In ra tất cả key-value trong DB
            try (DBIterator iterator = db.iterator()) {
                iterator.seekToFirst();
                System.out.println("===== Database Content =====");
                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    String key = new String(entry.getKey(), StandardCharsets.UTF_8);
                    String value = new String(entry.getValue(), StandardCharsets.UTF_8);
                    System.out.println(key + " = " + value);
                }
                System.out.println("============================");
            }

        } catch (IOException e) {
            throw new RuntimeException("Can't open database", e);
        }
    }

    //=========================================Db Handler=========================================
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
            System.err.println(e.getMessage());
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

    public static void decreaseBlockCount() {
        File jsonFile = new File("Db/BlockCount.json");

        try {
            int currentCount = getBlockCount();
            if (currentCount == -1) return;

            FileWriter writer = new FileWriter(jsonFile, false);
            writer.write(String.valueOf(currentCount - 1));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Block getBlock(int index) {
        System.out.println("Getting block " + index);
        Block block = getBlockDB(index);
        if (block == null) {
            System.err.println("Error parsing block JSON at index " + index);
        }
        return block;
    }

    public static Block getLastBlock() {
        return getBlock(getBlockCount());
    }

    public static void addBlock(Block block) {
        try {
            for (Transaction tx : block.getTransactions()) {
                TransactionService.removeFromMempool(tx);
                for (UTXO utxo : tx.getUtxos()) {
                    utxo.setIsLocked(false);
                    UTXOSet.addUTXO(utxo);
                }
                for (TxIn txIn : tx.getInputs()) {
                    UTXOSet.removeUTXO(txIn.getPrevTxId(), txIn.getOutputIndex());
                }
            }
            addBlockDB(block);
            increaseBlockCount();
        } catch (Exception e) {
            System.err.println("Error adding block: " + e.getMessage());
        }
    }

    public static Block createNewBlock(List<Transaction> transactions, Wallet wallet) {
        if (getBlockCount() == 0) {
                Block genesisBlock = new Block(wallet, 1, version,
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        new BigInteger("0000000000000000000000000000000000000000000000000000000100010001", 16),
                        1, new ArrayList<>());
                return genesisBlock;
        }

        Block block = BlockchainService.getBlock(getBlockCount());
        if (getBlockCount() == 0) return block;
        if (block == null) System.err.println("Error creating new block");
        int index = block.getIndex() + 1;
        String previousHash = block.getHash();
        BigInteger previousNChainWork = block.getNChainWork();
        int difficulty = block.getDifficulty();

        Block newBlock = new Block(wallet, index, version, previousHash, previousNChainWork, difficulty, transactions);
        return newBlock;
    }

    public static void removeBlock() {
        int index = getBlockCount() - 1;
        String key = indexToKey(index);
        db.delete(bytes(key));
        decreaseBlockCount();
    }

    //=======================================Block Verifier=======================================
    public static boolean examineBlock(Block block, String senderAddress) {

        if (getBlockCount() <= 0) return true;
        Block lastBlock = getLastBlock();

        // 1. Examine index
        if (block.getIndex() != lastBlock.getIndex() + 1 && senderAddress != null) {
            if (block.getIndex() > lastBlock.getIndex() + 1) {
                PeerService.broadcastBlockLocator(senderAddress);
            }

            System.out.println("Invalid index");
            return false;
        }

        // 2. Verify previous hash with curr block
        if (!block.getPreviousHash().equals(lastBlock.getHash())) {
            System.out.println("Previous hash mismatch");
            return false;
        }

        // 3. Check merkle root
        String expectedMerkle = block.calculateMerkleTree();
        List<Transaction> txs = block.getTransactions();
        if (!expectedMerkle.equals(block.calculateMerkleTree())) {
            System.out.println("Merkle root mismatch");
            return false;
        }

        // 4. Verify timestamp (not earlier than last block and not too far in the future)
        long now = System.currentTimeMillis() / 1000L;
        if (block.getTimestamp() <= lastBlock.getTimestamp()) {
            System.out.println("Invalid timestamp (too early)");
            return false;
        }
//        if (block.getTimestamp() > now + 2 * 60 * 60) {
//            System.out.println("Invalid timestamp (too far in future)");
//            return false;
//        }

        // 5. Check difficulty and hash
        BigInteger target = new BigInteger(block.getHash(), 16);
        if (target.compareTo(block.getTarget()) > 0) {
            System.out.println("Hash does not meet difficulty target");
            return false;
        }

        // 6. Verify NChainWork
        if (!block.getNChainWork().equals(lastBlock.getNChainWork().add(
                BigInteger.ONE.shiftLeft(256).divide(block.getTarget().add(BigInteger.ONE))))) {
            System.out.println("Invalid chain work");
            return false;
        }

        // 7. Check transactions
        if (txs.isEmpty()) {
            System.out.println("No transactions");
            return false;
        }

//        if (!txs.get(txs.size() - 1).isCoinbase()) {
//            System.out.println("Invalid coinbase transaction");
//            return false;
//        }
        for (int i = 1; i < txs.size(); i++) {
            try {
                if (!TransactionService.validateTransaction(txs.get(i))) {
                    System.out.println("Invalid transaction found");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error validating transaction: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    //============================================Json============================================
    public static Block getBlockFromJson(String json) {
        return gson.fromJson(json, Block.class);
    }

    public static String getJsonFromBlock(Block block) {
        return gson.toJson(block);
    }

    private static String indexToKey(int index) {
        return String.valueOf(index);
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
        System.out.println("Block: " + json);
        db.put(bytes(key), bytes(json));
    }

    public static Block getBlockDB(int index) {
        String key = String.valueOf(index);
        byte[] data = db.get(bytes(key));
        if (data == null) return null;
        return getBlockFromJson(asString(data));
    }
}
