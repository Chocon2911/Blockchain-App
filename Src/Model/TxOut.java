package Model;

import java.security.*;
import java.security.PublicKey;

public class TxOut {
    private long value;
    private String publicAdd;


    public TxOut(long value, String publicAdd) {
        this.value = value;
        this.publicAdd = publicAdd;
    }

    public long getValue() {
        return value;
    }
    public String getPublicAdd() {
        return publicAdd;
    }

    @Override
    public String toString() {
        return "TxOut{value=" + value + ", to=" + publicAdd + "}";
    }
}
