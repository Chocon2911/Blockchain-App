package Src.Model;

import java.util.List;

public class Block {
    private String previousHash;
    private String hash;
    private List<Transaction> transactions;
    private long timestamp = System.currentTimeMillis();

    private int nonce;

    private static final int DIFFICULTY = 4; // Có thể điều chỉnh

    public Block(String previousHash, List<Transaction> transactions) {
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.timestamp = System.currentTimeMillis();
        this.nonce = 0;
        this.hash = calculateHash(); // Khởi tạo hash ban đầu
    }

    public String calculateHash() {
        String data = previousHash + timestamp + transactions.toString() + nonce;
        return Miner.applySha256(data);
    }

    public void mineBlock() {
        System.out.println("Đang khai thác block...");
        while (!hash.startsWith("0".repeat(DIFFICULTY))) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block đã được khai thác: " + hash);
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public static int getDifficulty() {
        return DIFFICULTY;
    }
}
