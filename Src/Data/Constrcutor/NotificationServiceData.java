package Data.Constrcutor;

public abstract class NotificationServiceData extends ServiceData {
    private String message;

    public NotificationServiceData(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
