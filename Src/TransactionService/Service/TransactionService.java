package TransactionService.Service;

import TransactionService.Model.Transaction;
import TransactionService.Util.Network;

import java.util.List;
import java.util.UUID;

public class TransactionService {
    private static String mempoolPath = "db/Mempool.json";

    public boolean validateTransaction(Transaction tx) {
        try {
            return tx.verifySignature();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addTransaction(Transaction tx) {
        Network.broadcastTransaction(tx);
        return UUID.randomUUID().toString();
    }

    public static List<Transaction> getMempool(){
        return null;
    }

    public static Transaction getLastTransaction() {

    }

    public static void addTransaction(Transaction) {
        String jsonPath = "db/Mempool.json";
     }
}
