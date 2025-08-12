package Model;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Transaction {
    public int version;
    public List<TxIn> inputs;
    public List<TxOut> outputs;
    public long locktime;
    public long timestamp;
    public String txId;

    private byte[] signature;
    private PublicKey senderPublicKey;

    public Transaction(int version, List<TxIn> inputs, List<TxOut> outputs, long locktime) {
        this.version = version;
        this.inputs = inputs;
        this.outputs = outputs;
        this.locktime = locktime;
        this.timestamp = new Date().getTime();
        this.txId = calculateTxId();
    }
    private String calculateTxId() {
        StringBuilder sb = new StringBuilder();
        sb.append(version).append(locktime).append(timestamp);
        for (TxIn in : inputs) {
            sb.append(in.prevTxId).append(in.outputIndex).append(Arrays.toString(in.scriptSig));
        }
        for (TxOut out : outputs) {
            sb.append(out.value).append(out.scriptPubKey);
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 Error", e);
        }
    }
    // Simplified stub to avoid external Wallet dependency in test path
    public void signTransaction(PublicKey pubKey, byte[] signature) {
        this.senderPublicKey = pubKey;
        this.signature = signature;
    }

    public boolean verifySignature() throws Exception {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaVerify.initVerify(this.senderPublicKey);
        ecdsaVerify.update(calculateTxId().getBytes());
        return ecdsaVerify.verify(signature);
    }
    public boolean checkBalanceEnough(List<TxOut> utxoSet, long amount) {
        long balance = 0;
        for (TxOut utxo : utxoSet) {
            if (utxo.isOwnBy(String.valueOf(senderPublicKey))) {
                balance += utxo.getValue();
                if (balance >= amount) {
                    return true;
                }
            }
        }
        return false;
    }
    @Override
    public String toString() {
        return "Transaction{\n  txid=" + txId +
                ",\n  version=" + version +
                ",\n  inputs=" + inputs +
                ",\n  outputs=" + outputs +
                ",\n  locktime=" + locktime +
                ",\n  timestamp=" + timestamp +
                "\n}";
    }

    public String getHash() { return null; }
}
