package Src.Model;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class BlockchainDemo {
    public static void main(String[] args) throws Exception {
        Wallet sender = new Wallet();
        Wallet receiver = new Wallet();

        Map<String, Float> balances = new HashMap<>();
        Map<String, PublicKey> addressBook = new HashMap<>();

        balances.put(sender.getAddress(), 500.0f);
        addressBook.put(sender.getAddress(), sender.getPublicKey());
        addressBook.put(receiver.getAddress(), receiver.getPublicKey());

        Transaction tx = new Transaction(
                sender.getAddress(),
                receiver.getAddress(),
                100.0f
        );

        tx.signTransaction(sender);

        System.out.println("Giao dịch vừa được tạo:");
        tx.printInfo();
        Network.broadcastTransaction(tx);

        Blockchain blockchain = new Blockchain(); // tạo chuỗi mới
        List<Transaction> validTransactions = List.of();
        Block newBlock = new Block(blockchain.getLatestBlock().getHash(), validTransactions);
        blockchain.addBlock(newBlock);


        // Kiểm tra tính hợp lệ
        System.out.println("Blockchain hợp lệ: " + blockchain.isChainValid());
    }
}

