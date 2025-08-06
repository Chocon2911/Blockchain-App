package Src.Model;

import java.util.ArrayList;
import java.util.List;

import Src.Main.Util;

public class Miner
{
    //==========================================Variable==========================================
    private final Blockchain blockchain;
    private Wallet wallet;
    private List<Thread> runningThreads;





//============================================Them============================================
    private static final int DIFFICULTY = Block.getDifficulty();

    public static void mineBlock(List<Transaction> tempmem, Map<String, Float> balances, Map<String, PublicKey> addressBook) throws Exception {
        List<Transaction> validTransactions = new ArrayList<>();

        for (Transaction tx : tempmem) {
            System.out.println("Đang kiểm tra giao dịch");
            boolean valid = true;

            // Kiểm tra chữ ký
            PublicKey senderPublicKey = addressBook.get(tx.sender);
            if (senderPublicKey == null || !tx.verifySignature(senderPublicKey)) {
                System.out.println("Chữ ký không hợp lệ.");
                valid = false;
            }

            // Kiểm tra số dư
            float balance = balances.getOrDefault(tx.sender);
            if (tx.amount> balance) {
                System.out.println("Không đủ số dư.");
                valid = false;
            }

            if (valid) {
                validTransactions.add(tx);
                balances.put(tx.sender, balance - tx.amount);
                balances.put(tx.receiver, balances.getOrDefault(tx.receiver) + tx.amount);
            }
        }

        if (validTransactions.isEmpty()) {
            System.out.println("Không có giao dịch hợp lệ để khai thác.");
            return;
        }

        // Giải bài toán PoW
        String blockData = validTransactions.toString();
        int nonce = 0;
        String hash = "";
        while (true) {
            String input = blockData + nonce;
            hash = applySha256(input);
            if (hash.startsWith("0".repeat(DIFFICULTY))) {
                break;
            }
            nonce++;
        }

        System.out.println("\nBlock đã được khai thác thành công!");
        System.out.println("Hash: " + hash);
        System.out.println("Giao dịch được xác thực: ");
        for (Transaction tx : validTransactions) {
            System.out.println(tx);
        }

        // Xóa khỏi tempmem
        tempmem.removeAll(validTransactions);
    }

    public static String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
//============================================Them============================================







    public Blockchain getBlockchain() { return this.blockchain; }
    public Wallet getWallet() { return this.wallet; }
    public Long getHashRate() {
        Long start = System.currentTimeMillis();
        Long end = System.currentTimeMillis();
        Long times = 0L;
        while (end - 1000 < start) {
            end = System.currentTimeMillis();
            times++;
        }

        return (times) / (end - start);
    }

    //========================================Constructor=========================================
    public Miner(Wallet wallet, Blockchain blockchain) {
        this.runningThreads = new ArrayList<>();
        this.wallet = wallet;
        this.blockchain = blockchain;
    }

    //===========================================Method===========================================
    public void startMine() {
        Thread thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    this.blockchain.mine(this.wallet);
                }
            } catch (Exception e) {
                System.out.println("Exception Mining Thread: " + e.getMessage());
            }

            System.out.println("Mining Thread is stopped.");
        });

        this.runningThreads.add(thread);
        thread.start();
    }

    public void stopMine() {
        for (Thread thread : this.runningThreads) {
            thread.interrupt();
        }

        this.runningThreads.clear();
        synchronized (this.blockchain) {
            Util.getInstance().SaveBlockchainJson(this.blockchain);
        }
        synchronized (this.wallet) {
            Util.getInstance().SaveWalletJson(this.wallet);
        }
        System.out.println("Threads stopped");
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }
}
