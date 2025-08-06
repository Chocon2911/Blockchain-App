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

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public List<Block> getChain() {
        return chain;
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);

            if (!current.getHash().equals(current.calculateHash())) {
                System.out.println("Hash không khớp tại block " + i);
                return false;
            }

            if (!current.getPreviousHash().equals(previous.getHash())) {
                System.out.println("Liên kết previousHash bị lỗi tại block " + i);
                return false;
            }
        }
        return true;
    }
}
