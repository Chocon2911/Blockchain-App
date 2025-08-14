package Model;

import java.security.MessageDigest;
import java.security.PublicKey;

public class TxIn {
    public String prevTxId; // Hash của giao dịch trước
    public int outputIndex; // Vị trí của output
    public byte[] scriptSig; // Chữ ký (giả lập)
    public PublicKey pubKey;

    public TxIn(String txid, int outputIndex, PublicKey pubKey) {
        this.prevTxId = txid;
        this.outputIndex = outputIndex;
        this.pubKey = pubKey;
        this.scriptSig = null;
    }

    public String getPrevTxId() {
        return prevTxId;
    }

    public int getPrevIndex() {
        return outputIndex;
    }
    public PublicKey getPubKey() {
        return pubKey;
    }
    public byte[] getSignature() {
        return scriptSig;
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
            String data = this.getPrevTxId() + this.getPrevIndex();
            return digest.digest(data.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
