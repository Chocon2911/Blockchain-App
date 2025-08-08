package Model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Blockchain {
    //==========================================Variable==========================================
    private int difficulty;
    private String version;
    private String merkleRoot;
    private final List<Block> chain;

    //==========================================Get Set===========================================
    public Block getLastBlock() {
        return chain.getLast();
    }

    //========================================Constructor=========================================
    public Blockchain(Block firstBlock, int difficulty, String version, String merkleRoot) {
        this.chain = new ArrayList<>();
        this.chain.add(firstBlock);
        this.difficulty = difficulty;
        this.version = version;
        this.merkleRoot = merkleRoot;
    }

    //===========================================Method===========================================
    public void mine(Wallet wallet) {
        synchronized (this) {
            this.getLastBlock().mineBlock(wallet);
            this.createNewBlock();
        }
    }

    public void createNewBlock() {
        Block prevBlock = getLastBlock();
        Block newBlock = new Block(prevBlock.getIndex() + 1, this.version, this.merkleRoot,
                prevBlock.getHeader(), prevBlock.getNChainWork(), this.getDifficulty());
        chain.add(newBlock);
    }

    public boolean addBlock(Block block) {
        Block lastBlock = this.getLastBlock();
        if (!block.getPreviousHash().equals(lastBlock.getHash())) return false;
        this.chain.add(block);
        return true;
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHeader().equals(current.getHeader())) {
                System.out.println("Invalid hash at block " + current.getIndex());
                return false;
            }
            if (!current.getPreviousHash().equals(previous.getHeader())) {
                System.out.println("Invalid previous hash at block " + current.getIndex());
                return false;
            }
        }
        return true;
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public void printChain() {
        System.out.println(toJson());
    }

    public long getBalance(String publicKey) {
        long balance = 0;
        for (Block block : chain) {
            for (Transaction transaction : block.getTransactions()) {
                if (transaction.getReceiverKey().equals(publicKey)) {
                    balance += transaction.getAmount();
                }
            }
        }
        return balance;
    }

    private int getDifficulty() {
        return this.difficulty;
    }
}