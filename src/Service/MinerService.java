package Service;

import Model.Block;
import Model.Transaction;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinerService {
    public static int getAllProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void startMine(int processorsLimit, String publicAddress,
                                 AtomicBoolean canRun, List<Transaction> addedTransactions) {
        AtomicBoolean foundNonce = new AtomicBoolean(false);
        Block newBlock = BlockchainService.createNewBlock(addedTransactions);
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
            BlockchainService.addBlock(block);
            PeerService.broadcastBlock(block);
        }
    }
}
