package Model;

import Service.UTXOSet;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Transaction {
    public String version;
    public List<TxIn> inputs;
    public List<TxOut> outputs;
    public List<UTXO> utxos;
    public long locktime;
    public long timestamp;
    public String hash;

    public Transaction(PrivateKey privateKeySender, PublicKey publicKeySender,
                       String publicAddressReceiver, long amount, long fee) {
        this.version = "0.0.1";
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();

        // Lấy UTXO của người gửi, KHÔNG phải receiver
        Wallet wallet = new Wallet(privateKeySender, publicKeySender);
        List<UTXO> senderUtxos = UTXOSet.getUTXOsByPubAdd(wallet.getAddress());

        long totalInput = 0;
        long required = amount + fee;

        // Chọn UTXO cho đến khi đủ amount + fee
        for (UTXO utxo : senderUtxos) {
            if (utxo.getIsLocked()) continue; // skip UTXO đã bị lock

            // Lock UTXO khi được sử dụng
            utxo.setIsLocked(true);
            UTXOSet.addUTXO(utxo);

            inputs.add(new TxIn(utxo.getTxId(), utxo.getIndex(), publicKeySender));
            totalInput += utxo.getValue();

            if (totalInput >= required) break;
        }

        if (totalInput < required) {
            throw new RuntimeException("Insufficient balance to cover amount + fee");
        }

        // Output cho receiver
        outputs.add(new TxOut(amount, publicAddressReceiver));

        // Nếu còn dư thì trả lại cho sender (change)
        long change = totalInput - amount - fee;
        if (change > 0) {
            this.utxos.add(new UTXO(1, change, wallet.getAddress(), true));
        }

        this.timestamp = new Date().getTime();

        // Tạo hash cho transaction (giản lược, tuỳ bạn có SHA-256 hay double SHA-256)
        this.hash = getTxId();
    }

    public Transaction(String pubAdd, long reward, PublicKey pubKey, Wallet wallet) {
        this.version = "0.0.1";
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.timestamp = new Date().getTime();
        this.locktime = this.timestamp + 300000; // ví dụ 5 phút

        // ----- Tạo coinbase input -----
        String zeroPrevTxId = "0000000000000000000000000000000000000000000000000000000000000000";
        int voutIndex = -1; // Coinbase tx không tham chiếu output thực tế

        // Ở coinbase tx, scriptSig là dữ liệu tùy ý (extra nonce, thông điệp miner)
        TxIn coinbaseIn = new TxIn(zeroPrevTxId, voutIndex, pubKey);
        try {
            byte[] signature = wallet.sign(this.calculateHash().getBytes());
            coinbaseIn.setSignature(signature);
            this.inputs.add(coinbaseIn);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR creating Transaction failed (coinbase transaction)");
        }

        // ----- Tạo output trả phần thưởng về miner -----
        TxOut rewardOut = new TxOut(reward, pubAdd);
        this.outputs.add(rewardOut);
    }

    public void addInput(TxIn in) {
        inputs.add(in);
    }

    public void addOutput(TxOut out) {
        outputs.add(out);
    }

    public List<TxIn> getInputs() {
        return inputs;
    }

    public List<TxOut> getOutputs() {
        return outputs;
    }
    public int getSize() {
        return (inputs.size() * 180) + (outputs.size() * 34) + 10;
    }
    public String getTxId() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder rawData = new StringBuilder();
            for (TxIn in : inputs) {
                rawData.append(in.prevTxId).append(in.outputIndex).append(Arrays.toString(in.scriptSig));
            }
            for (TxOut out : outputs) {
                rawData.append(out.getValue()).append(out.getPublicAdd());
            }

            byte[] hash = digest.digest(rawData.toString().getBytes());
            return bytesToHex(hash);
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public String calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder rawData = new StringBuilder();
            for (TxIn in : inputs) {
                rawData.append(in.prevTxId).append(in.outputIndex);
            }
            for (TxOut out : outputs) {
                rawData.append(out.getValue()).append(out.getPublicAdd());
            }

            byte[] hash = digest.digest(rawData.toString().getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String toString() {
        return "Transaction{\n  txid=" + this.getTxId() +
                ",\n  version=" + version +
                ",\n  inputs=" + inputs +
                ",\n  outputs=" + outputs +
                ",\n  locktime=" + locktime +
                ",\n  timestamp=" + timestamp +
                "\n}";
    }

    public boolean isCoinbase() {
        return inputs.size() == 1 &&
                inputs.get(0).getPrevTxId().matches("^0+$") &&
                inputs.get(0).getPrevIndex() == -1;
    }
}
