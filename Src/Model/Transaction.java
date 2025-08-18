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
        this.utxos = new ArrayList<>();
        this.timestamp = new Date().getTime();

        Wallet wallet = new Wallet(privateKeySender, publicKeySender);
        List<UTXO> senderUtxos = UTXOSet.getUnlockedUTXOsByPubAdd(wallet.getAddress());
        if (senderUtxos.size() == 0) { System.out.println("No UTXOs found"); }

        long totalInput = 0;
        long required = amount + fee;

        for (UTXO utxo : senderUtxos) {
            if (utxo.getIsLocked()) continue;
            utxo.setIsLocked(true);
            UTXOSet.addUTXO(utxo);

            System.out.println("index: " + utxo.getIndex());
            this.inputs.add(new TxIn(utxo.getTxId(), utxo.getIndex(), publicKeySender));
            System.out.println("In Index: " + utxo.getIndex());
            totalInput += utxo.getValue();
            System.out.println("Value: " + totalInput);

            if (totalInput >= required) break;
        }

        System.out.println("Total input: " + totalInput);
        if (totalInput < required) {
            throw new RuntimeException("Insufficient balance to cover amount + fee");
        }

        outputs.add(new TxOut(amount, publicAddressReceiver));
        long change = totalInput - amount - fee;
        if (change > 0) {
            this.utxos.add(new UTXO(1, change, wallet.getAddress(), true));
            outputs.add(new TxOut(change, wallet.getAddress()));
        }

        this.hash = getTxId();
        for (UTXO utxo : this.utxos) {
            utxo.setTxId(hash);
        }
    }

    public Transaction(long blockReward, List<Transaction> txsInBlock, Wallet wallet) {
        this.version = "0.0.1";
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.utxos = new ArrayList<>();
        this.timestamp = new Date().getTime();
        this.locktime = this.timestamp + 300000; // 5 minutes

        // ----- Total fee from all transactions -----
        long totalFee = 0;
        for (Transaction tx : txsInBlock) {
            totalFee += tx.calculateFee(); // dùng calculateFee() của từng transaction
        }

        // ----- Create transaction output base on fee + block reward -----
        long totalReward = blockReward + totalFee;
        TxOut rewardOut = new TxOut(totalReward, wallet.getAddress());
        this.outputs.add(rewardOut);

        // ----- create transaction hash -----
        this.hash = getTxId();
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
                rawData.append(in.getPrevTxId()).append(in.getOutputIndex()).append(Arrays.toString(in.getSignature()));
            }
            for (TxOut out : outputs) {
                rawData.append(out.getValue()).append(out.getPublicAdd());
            }

            rawData.append(locktime).append(timestamp);
            rawData.append(version);
            rawData.append(locktime);

            byte[] hash = digest.digest(rawData.toString().getBytes());
            return bytesToHex(hash);
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public List<UTXO> getUtxos() { return utxos; }

    public String calculateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder rawData = new StringBuilder();
            for (TxIn in : inputs) {
                rawData.append(in.getPrevTxId()).append(in.getOutputIndex());
            }
            for (TxOut out : outputs) {
                rawData.append(out.getValue()).append(out.getPublicAdd());
            }

            rawData.append(locktime).append(timestamp);
            rawData.append(version);
            rawData.append(locktime);

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
        return inputs.size() == 0 &&
                inputs.get(0).getOutputIndex() == 0;
    }

    public long calculateFee() {
        long sumInputs = 0;
        for (TxIn in : this.inputs) {
            if (in.getUTXO() != null) {
                sumInputs += in.getUTXO().getValue();
            }
        }

        long sumOutputs = 0;
        for (TxOut out : this.outputs) {
            sumOutputs += out.getValue();
        }

        long fee = sumInputs - sumOutputs;
        return fee > 0 ? fee : 0;
    }
}
