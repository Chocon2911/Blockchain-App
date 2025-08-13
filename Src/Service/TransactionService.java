package Service;

import Model.*;
import org.iq80.leveldb.WriteBatch;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class TransactionService {

    public static Queue<Transaction> mempool;
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


    public static boolean validateTransaction(Transaction tx) throws IOException {
        // 1. Transaction phải có input và output
        if (tx.getInputs() == null || tx.getInputs().isEmpty()) {
            System.out.println("Transaction không có input");
            return false;
        }
        if (tx.getOutputs() == null || tx.getOutputs().isEmpty()) {
            System.out.println("Transaction không có output");
            return false;
        }

        long totalInputValue = 0;
        long totalOutputValue = 0;

        // 2. Kiểm tra từng input
        for (TxIn txIn : tx.getInputs()) {
            // Lấy UTXO từ UTXOSet
            UTXO utxo = UTXOSet.getUTXO(txIn.getPrevTxId(), txIn.getPrevIndex());
            if (utxo == null) {
                System.out.println("UTXO không tồn tại hoặc đã tiêu: "
                        + txIn.getPrevTxId() + ":" + txIn.getPrevIndex());
                return false;
            }

            // Xác minh chữ ký với publicKey của UTXO
            try {
                boolean isValidSignature = CryptoUtil.verifySignature(
                        txIn.getSignature(),
                        tx.getTxId().getBytes(),
                        txIn.getPubKey()
                );

                if (!isValidSignature) {
                    System.out.println("Chữ ký không hợp lệ cho input: " + txIn.getPrevTxId());
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            totalInputValue += utxo.getValue();
        }
//UTXO laf 1 phan output chinhr total output value
        // Tính tổng giá trị output
        for (TxOut txOut : tx.getOutputs()) {
            totalOutputValue += txOut.getValue();
        }

        if (totalInputValue < totalOutputValue) {
            System.out.println("Giá trị input nhỏ hơn output");
            return false;
        }

        return true;
    }
    
    

    //=========================================Queue util=========================================


    public boolean verifyTransaction(Transaction tx) throws IOException {
        // 1. Kiểm tra số lượng input/output
        if (tx.getInputs().isEmpty() || tx.getOutputs().isEmpty()) {
            System.out.println("❌ Transaction không có input hoặc output");
            return false;
        }

        // 2. Kiểm tra kích thước transaction
        if (tx.getSize() > 100_000) { // Giới hạn 100 KB
            System.out.println("❌ Transaction vượt quá kích thước cho phép");
            return false;
        }

        long totalInputValue = 0;
        long totalOutputValue = 0;

        // 3. Kiểm tra từng input
        for (TxIn in : tx.getInputs()) {
            // 3.1 Kiểm tra UTXO tồn tại
            UTXO referencedUTXO = UTXOSet.getUTXO(in.getPrevTxId(), in.getPrevIndex());
            if (referencedUTXO == null) {
                System.out.println("❌ UTXO không tồn tại hoặc đã bị tiêu");
                return false;
            }

            // 3.2 Xác minh chữ ký
            boolean signatureValid = CryptoUtil.verifySignature(
                    in.getRawDataToSign(), // dữ liệu cần ký
                    in.getSignature(),
                    in.getPubKey()
            );
            if (!signatureValid) {
                System.out.println("❌ Chữ ký không hợp lệ");
                return false;
            }

            totalInputValue += referencedUTXO.getValue();
        }

        // 4. Kiểm tra giá trị output
        for (TxOut out : tx.getOutputs()) {
            if (out.getValue() < 0) {
                System.out.println("❌ Output có giá trị âm");
                return false;
            }
            totalOutputValue += out.getValue();
        }

        // 5. Kiểm tra tổng giá trị
        if (totalInputValue < totalOutputValue) {
            System.out.println("❌ Tổng input < tổng output");
            return false;
        }

        // 6. Phí giao dịch hợp lệ (có thể = 0)
        long fee = totalInputValue - totalOutputValue;
        if (fee < 0) {
            System.out.println("❌ Fee âm");
            return false;
        }

        // 7. Pass tất cả
        System.out.println("✅ Transaction hợp lệ. Fee: " + fee);
        return true;
    }




}
