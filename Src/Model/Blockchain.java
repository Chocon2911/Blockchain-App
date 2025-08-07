package Src.Model;

import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private List<Block> chain;

    public Blockchain() {
        this.chain = new ArrayList<>();
        // Khởi tạo block đầu tiên (genesis block)
        Block genesis = new Block("0", new ArrayList<>());
        genesis.mineBlock();
        chain.add(genesis);
    }

    public void addBlock(Block newBlock) {
        newBlock.mineBlock();
        chain.add(newBlock);
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
