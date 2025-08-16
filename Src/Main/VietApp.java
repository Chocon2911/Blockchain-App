package Main;

import Model.*;
import Service.PeerService;
import Service.TransactionService;

import java.util.ArrayList;
import java.util.List;

public class VietApp {
    public static void main(String[] args) {
        List<String> ips = new ArrayList<String>();
        ips.add(PeerService.getMyAddress(5000));
        PeerService.createIpAddressessFile(ips);
        try {
            new Thread(() -> PeerService.listenForTransactions(5000)).start();
            Wallet wallet1 = new Wallet();

            Transaction tx = new Transaction(wallet1.getAddress(), 50000, wallet1.getPublicKey(), wallet1);
            wallet1.sign(tx.calculateHash().getBytes());
            if (!TransactionService.validateTransaction(tx)) System.out.println("Transaction invalid");
            PeerService.broadcastTransaction(tx);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR creating Wallet");
        }

    }
}
