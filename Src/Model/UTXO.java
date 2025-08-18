package Model;

import java.security.PublicKey;

public class UTXO {
    private String txId;
    private int index;
    private long value;
    private String pubAdd;
    private boolean isLocked;

    public UTXO(int index, long value, String pubAddress, boolean isLocked) {
        this.index = index;
        this.value = value;
        this.pubAdd = pubAddress;
        this.isLocked = isLocked;
    }

    public UTXO(String txId, int index, long value, String pubAdd, boolean isLocked) {
        this.txId = txId;
        this.index = index;
        this.value = value;
        this.pubAdd = pubAdd;
        this.isLocked = isLocked;
    }



    public String getTxId() {
        return txId;
    }

    public int getIndex() {
        return index;
    }

    public long getValue() {
        return value;
    }

    public String getPubAdd() { return pubAdd; }

    public boolean getIsLocked() { return isLocked; }

    public void setIsLocked(boolean isLocked) { this.isLocked = isLocked; }
    public void setTxId(String txId) { this.txId = txId; }

    @Override
    public String toString() {
        return "UTXO{" +
                "txId='" + txId + '\'' +
                ", index=" + index +
                ", value=" + value +
                ", publicKey=" + pubAdd +
                '}';
    }
}
