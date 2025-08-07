package Src.TransactionService.Service;

import Src.TransactionService.Model.Transaction;
import Src.TransactionService.Util.Network;
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
        Network.broadcastTransaction(tx);
        return UUID.randomUUID().toString();
    }
}
