package Model;

import Service.UTXOSet;

import java.security.MessageDigest;
import java.security.PublicKey;

public class TxIn {
    private String prevTxId;
    private int outputIndex;
    private byte[] scriptSig;
    private PublicKey pubKey;

    public TxIn(String txId, int outputIndex, PublicKey pubKey) {
        this.prevTxId = txId;
        this.outputIndex = outputIndex;
        this.pubKey = pubKey;
        this.scriptSig = null;
    }

    public String getPrevTxId() {
        return prevTxId;
    }

    public int getOutputIndex() {
        return outputIndex;
    }
    public PublicKey getPubKey() {
        return pubKey;
    }
    public byte[] getSignature() {
        return scriptSig;
    }

    public UTXO getUTXO() {
        return UTXOSet.getUTXO(this.prevTxId, this.outputIndex);
    }

    public void setSignature(byte[] signature) {
        this.scriptSig = signature;
    }

    @Override
    public String toString() {
        return "TxIn{Previoustxid=" + prevTxId.substring(0, 8) + "... , outputIndex=" + outputIndex + "}";
    }

    public byte[] getRawDataToSign() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = this.getPrevTxId() + this.getOutputIndex();
            return digest.digest(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
