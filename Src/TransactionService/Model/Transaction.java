package Src.TransactionService.Model;

import java.security.*;
import java.util.Base64;

public class Transaction {
    String sender;
    String receiver;
    float amount;

    private byte[] signature;
    private PublicKey senderPublicKey;

    public Transaction(String sender, String receiver, float amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }
    public byte[] getMessageBytes() {
        return (sender + "->" + receiver + ":" + amount).getBytes();
    }

    public void signTransaction(Wallet senderWallet) throws Exception {
        this.signature = senderWallet.sign(getMessageBytes());
        this.senderPublicKey = senderWallet.getPublicKey();
    }

    public boolean verifySignature() throws Exception {
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaVerify.initVerify(this.senderPublicKey);
        ecdsaVerify.update(getMessageBytes());
        return ecdsaVerify.verify(signature);
    }

    public void printInfo() {
        System.out.println("Tx: " + sender + " → " + receiver + " : " + amount + " BTC");
        System.out.println("Chữ ký (base64): " + Base64.getEncoder().encodeToString(signature));
    }
}
