package Model;

public class TxOut {
    public String txId;
    public static long value; // Số lượng Satoshi
    public int index;
    public String scriptPubKey; // Địa chỉ nhận (giả lập script)
    public boolean isUTXO;

    public TxOut(String txId, int index, long value, String scriptPubKey) {
        this.txId = txId;
        this.value = value;
        this.index = index;
        this.scriptPubKey = scriptPubKey;
    }

    @Override
    public String toString() {
        return "TxOut{value=" + value + ", to=" + scriptPubKey + "}";
    }

    public boolean isOwnBy(String pubKey) {
        return this.scriptPubKey.equals(pubKey);
    }

    public static long getValue() {
        return value;
    }
}
