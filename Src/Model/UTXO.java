package Model;

import java.security.PublicKey;

public class UTXO {
    private String txId;
    private int index;
    private long value;
    private PublicKey owner;

    public UTXO(String txId, int index, long value, PublicKey owner) {
        this.txId = txId;
        this.index = index;
        this.value = value;
        this.owner = owner;
    }

    public boolean isOwnedBy(PublicKey pubKey) {
        return this.owner.equals(pubKey);
    }

    public long getValue() {
        return this.value;
    }
}

