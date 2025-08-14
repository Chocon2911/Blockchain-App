package Model;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Block {
    //==========================================Variable==========================================
    private static final BigInteger MAX_TARGET = new BigInteger("FFFF0000000000000000000000000000000000000000000000000000", 16);

    private final int index;
    private long timestamp;
    private final String version;
    private final String previousHash;
    private final BigInteger previousNChainWork;
    private int nonce;
    private final int difficulty;
    private final String merkleRoot;
    private List<Transaction> transactions = new ArrayList<>();

    //========================================Constructor=========================================
    public Block(int index, String version, String previousHash,
                 BigInteger previousNChainWork, int difficulty, List<Transaction> transactions) {
        this.index = index;
        this.version = version;
        this.previousHash = previousHash;
        this.previousNChainWork = previousNChainWork;
        this.nonce = 0;
        this.difficulty = difficulty;
        this.transactions = transactions;
        this.merkleRoot = this.calculateMerkleTree();
        this.timestamp = System.currentTimeMillis();
    }

    public Block(int index, String version, String previousHash, BigInteger previousNChainWork,
                 int difficulty, List<Transaction> transactions, String merkleRoot) {
        this.index = index;
        this.version = version;
        this.previousHash = previousHash;
        this.previousNChainWork = previousNChainWork;
        this.nonce = 0;
        this.difficulty = difficulty;
        this.transactions = transactions;
        this.merkleRoot = merkleRoot;
        this.timestamp = System.currentTimeMillis();
    }

    //==========================================Get Set===========================================
    public String getPreviousHash() {
        return this.previousHash;
    }
    public int getIndex() { return this.index; }
    public Long getTimestamp() {
        return this.timestamp;
    }
    public int getDifficulty() {
        return this.difficulty;
    }
    public List<Transaction> getTransactions() { return this.transactions; }
    public String getMerkleRoot() { return this.merkleRoot; }

    public String getHash() {
        return sha256(sha256(this.getHeader()));
    }
    public String getHeader() {
        String input = this.version + this.previousHash + this.calculateMerkleTree() + this.timestamp +
                this.timestamp + this.getBits() + this.nonce;
        return input;
    }
    public long getReward() {
        final long INITIAL_REWARD = 50_0000_0000L; // 50 BTC * 10^8 (satoshi)
        final int HALVING_INTERVAL = 210_000;
        int halvings = this.index / HALVING_INTERVAL;

        if (halvings >= 64) return 0;
        return INITIAL_REWARD >> halvings;
    }
    public BigInteger getNChainWork() {
        BigInteger target = getTarget();
        BigInteger myWork = BigInteger.ONE.shiftLeft(256).divide(target.add(BigInteger.ONE));
        if (previousHash == null) return myWork;
        return this.previousNChainWork.add(myWork);
    }

    public BigInteger getTarget() {
        return MAX_TARGET.divide(BigInteger.valueOf(this.difficulty));
    }
    private long getBits() {
        byte[] bytes = this.getTarget().toByteArray();
        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            bytes = tmp;
        }

        int exponent = bytes.length;
        int mantissa = 0;
        for (int i = 0; i < Math.min(3, bytes.length); i++) {
            mantissa <<= 8;
            mantissa |= (bytes[i] & 0xff);
        }

        if ((mantissa & 0x00800000) != 0) {
            mantissa >>= 8;
            exponent += 1;
        }

        return ((long) exponent << 24) | (mantissa & 0x007fffff);
    }

    //===========================================Method===========================================
    public boolean mineBlock(int nonce) {
        BigInteger target = getTarget();
    String hashHex = sha256(sha256(this.getHeader()));
        BigInteger hashVal = new BigInteger(hashHex, 16);

        if (hashVal.compareTo(target) > 0) return false;
        this.nonce = nonce;
        return true;
    }

    // Lightweight SHA-256 helper to avoid external dependencies
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    @Override
    public String toString() {
        return "Block #" + this.index + " {\n" +
                "  Timestamp: " + this.timestamp + "\n" +
                "  Version: " + this.version + "\n" +
                "  Previous Hash: " + this.previousHash + "\n" +
                "  Nonce: " + this.nonce + "\n" +
                "  Difficulty: " + this.difficulty + "\n" +
                "  Reward: " + this.getReward() + "\n" +
                "  Transactions: " + this.transactions + "\n" +
                "}";
    }

    public String calculateMerkleTree() {
        if (this.transactions == null || this.transactions.isEmpty()) {
            return "";
        }

        List<String> currentLayer = new ArrayList<>();
        for (Transaction tx : this.transactions) {
            currentLayer.add(tx.getHash());
        }

        while (currentLayer.size() > 1) {
            List<String> nextLayer = new ArrayList<>();

            for (int i = 0; i < currentLayer.size(); i += 2) {
                String left = currentLayer.get(i);
                String right = (i + 1 < currentLayer.size()) ? currentLayer.get(i + 1) : left;
                String combinedHash = sha256(left + right);
                nextLayer.add(combinedHash);
            }

            currentLayer = nextLayer;
        }

        return currentLayer.get(0);
    }
}