package Service;


import Main.Util;
import Model.Block;
import Model.Transaction;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinerService {
    public static int getAllProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void startMine(int processorsLimit, String publicAddress,
                                 AtomicBoolean canRun, List<Transaction> addedTransactions) {
        AtomicBoolean foundNonce = new AtomicBoolean(false);
        Block block = BlockchainService.getBlock(BlockchainService.getBlockCount() - 1);

        int index = block.getIndex() + 1;
        String version = Util.version;
        String previousHash = block.getHash();
        BigInteger previousNChainWork = block.getNChainWork();
        int difficulty = block.getDifficulty();
        List<Transaction> mempool = addedTransactions;

        Block newBlock = new Block(index, version, previousHash, previousNChainWork, difficulty, mempool);
        for (int i = 0; i < processorsLimit; i++) {
            createMiningThread(newBlock, publicAddress, i, processorsLimit, foundNonce, canRun);
        }
    }

    private static void createMiningThread(Block block, String publicAddress, int index,
                                           int processorsLimit, AtomicBoolean isFound, AtomicBoolean canRun) {
        int count = 0;
        while (!isFound.get()) {
            if (!canRun.get()) return;
            int nonce = index * processorsLimit * count;
            isFound.set(block.mineBlock(nonce));
            count++;

            if (!isFound.get()) continue;
            PeerService.broadcastBlock(block);
        }
    }
}
