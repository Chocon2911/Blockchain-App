package Model;

public class Transaction {
    private String receiverKey;
    private long amount;

    public String getReceiverKey() { return receiverKey; }
    public long getAmount() { return amount; }

    public Transaction(String receiverKey, long amount) {
        this.receiverKey = receiverKey;
        this.amount = amount;
    }

    public String getHash() { return null; }
}