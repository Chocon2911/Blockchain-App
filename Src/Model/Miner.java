package Src.Model;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import static Src.Model.Miner.Config.DIFFICULTY;

public class Miner
{

    //============================================Them============================================
    public class Config {
        public static final int DIFFICULTY = 4;
    }

    public static void mineBlock(Queue<Transaction> tempmem, Map<String, Float> balances, Map<String, PublicKey> addressBook, Blockchain blockchain) throws Exception {
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
            float balance = balances.getOrDefault(tx.sender, 0.0f);
            if (tx.amount> balance) {
                System.out.println("Không đủ số dư.");
                valid = false;
            }

            if (valid) {
                validTransactions.add(tx);
                balances.put(tx.sender, balance - tx.amount);
                float receiverBalance = balances.getOrDefault(tx.receiver, 0.0f);
                balances.put(tx.receiver, receiverBalance + tx.amount);

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
}
