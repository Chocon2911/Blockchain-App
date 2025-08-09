package Service;

import Model.Transaction;
import Model.TxIn;
import Model.TxOut;
import Model.UTXO;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")

public class TransactionService {

    public static Queue<Transaction> mempool;



    @PostMapping
    public String createTransaction(@RequestBody Transaction tx) {
        boolean isValid = validateTransaction(tx);
        if (!isValid) return "Invalid signature";

        String txId = addTransaction(tx);
        return "Transaction accepted. ID: " + txId;
    }

    public boolean validateTransaction(Transaction tx) {
        try {
            return tx.verifySignature();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String addTransaction(Transaction tx) {
        broadcastTransaction(tx);
        return UUID.randomUUID().toString();
    }


    public static void broadcastTransaction(Transaction tx) {
        System.out.println("Phát tán giao dịch lên mạng lưới blockchain");
        mempool.add(tx);
        System.out.println("Giao dịch đang chờ trong bộ nhớ tạm. Số giao dịch: " + mempool.size());
    }

    public static List<Transaction> getMempool() {
        return mempool.stream().toList();
    }

    //=========================================Queue util=========================================
    static {
        mempool = new Queue<>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Transaction> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(Transaction transaction) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Transaction> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public boolean offer(Transaction transaction) {
                return false;
            }

            @Override
            public Transaction remove() {
                return null;
            }

            @Override
            public Transaction poll() {
                return null;
            }

            @Override
            public Transaction element() {
                return null;
            }

            @Override
            public Transaction peek() {
                return null;
            }
        };
    }


    public static List<TxOut> buildUTXOSet(List<Transaction> allTransactions) {
        Map<String, TxOut> utxoMap = new HashMap<>();

        // 1. Thêm tất cả output vào UTXO set tạm thời
        for (Transaction tx : allTransactions) {
            for (int i = 0; i < tx.outputs.size(); i++) {
                TxOut out = tx.outputs.get(i);
                String utxoKey = tx.txId + ":" + i;
                utxoMap.put(utxoKey, new TxOut(tx.txId, i, out.value, out.scriptPubKey));
            }
        }

        // 2. Loại bỏ những output đã được tiêu (theo inputs)
        for (Transaction tx : allTransactions) {
            for (TxIn in : tx.inputs) {
                String utxoKey = in.prevTxId + ":" + in.outputIndex;
                utxoMap.remove(utxoKey); // Đã tiêu → xóa
            }
        }

        // 3. Trả về danh sách UTXO hiện tại
        return new ArrayList<>(utxoMap.values());
    }
}
