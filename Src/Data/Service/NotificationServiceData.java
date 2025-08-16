package Data.Service;

import Data.Constrcutor.ServiceData;

public class NotificationServiceData extends ServiceData {
    private String message;

    public NotificationServiceData(String action, String message) {
        this.action = action;
        this.message = message;
    }

    public String getMessage() { return message; }
}
