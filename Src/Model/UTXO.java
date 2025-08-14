package Model;

import java.security.PublicKey;

public class UTXO {
    private String txId;       // Transaction ID chứa output này
    private int index;         // Vị trí của output trong transaction
    private long value;      // Giá trị coin
    private String pubAdd; // Chủ sở hữu

    public UTXO(String txId, int index, long value, String pubAdd) {
        this.txId = txId;
        this.index = index;
        this.value = value;
        this.pubAdd = pubAdd;
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

    public String getPubAdd() {
        return pubAdd;
    }

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
