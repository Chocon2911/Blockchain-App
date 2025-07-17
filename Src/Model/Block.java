package Src.Model;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Block {
    //==========================================Variable==========================================
    private final int index;
    private final long timestamp;
    private final String version;
    private final String merkleRoot;
    private final String previousHash;
    private int nonce;
    private final int difficulty;
    private final long reward;
    private List<Transaction> transactions = new ArrayList<>();

    //========================================Constructor=========================================
    public Block(int index, String version, String merkleRoot, String previousHash, int difficulty) {
        this.index = index;
        this.timestamp = System.currentTimeMillis();
        this.version = version;
        this.merkleRoot = merkleRoot;
        this.previousHash = previousHash;
        this.nonce = 0;
        this.difficulty = difficulty;
        this.reward = this.getInitReward();
        this.transactions = new ArrayList<>();
    }

    //==========================================Get Set===========================================
    public String getPreviousHash() {
        return this.previousHash;
    }
    public int getIndex() {
        return this.index;
    }
    public String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(this.timestamp));
    }
    public int getDifficulty() {
        return this.difficulty;
    }
    public float getReward()
    {
        return this.reward;
    }
    public List<Transaction> getTransactions() { return this.transactions; }

    //===========================================Method===========================================
    public static String applySha256(String input) {
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

    public void mineBlock(Wallet wallet) {
        String target = "0".repeat(this.difficulty);
        while (!this.getHash().substring(0, this.difficulty).equals(target)) {
            this.nonce++;
        }

        Transaction rewardTransaction = new Transaction(wallet.getPublicKey(), this.reward);
        this.transactions.add(rewardTransaction);
        System.out.println("Block mined: " + this.getHash());
    }

    public String getHash() {
        String input = this.index + this.previousHash + this.timestamp + this.version
                + this.merkleRoot + this.nonce + this.difficulty;
        return applySha256(input);
    }

    @Override
    public String toString() {
        return "Block #" + this.index + " {\n" +
                "  Timestamp: " + this.getTimestamp() + "\n" +
                "  Version: " + this.version + "\n" +
                "  Previous Hash: " + this.previousHash + "\n" +
                "  Nonce: " + this.nonce + "\n" +
                "  Difficulty: " + this.difficulty + "\n" +
                "  Reward: " + this.reward + "\n" +
                "  Transactions: " + this.transactions + "\n" +
                "}";
    }

    private long getInitReward() {
        final double INITIAL_REWARD = 50;
        int halvingInterval = 210000;
        double halvingCount = (double) this.index / halvingInterval;
        return (long) (INITIAL_REWARD / Math.pow(2, halvingCount)) * 100000000L;
    }
}
