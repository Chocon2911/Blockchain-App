package Data.Service;

public class CreateTransactionServiceData extends NotificationServiceData {
    private String txId;

    public CreateTransactionServiceData(String message, String txId) {
        super("create_transaction", message);
        this.txId = txId;
    }
}
