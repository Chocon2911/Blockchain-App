package Service;

import Model.Transaction;

import java.util.UUID;

public class TransactionService {
    public boolean validateTransaction(Transaction tx) {
        try {
            return tx.verifySignature();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addTransaction(Transaction tx) {
        Network.BroadcastTransaction(tx);
        return UUID.randomUUID().toString();
    }
}
