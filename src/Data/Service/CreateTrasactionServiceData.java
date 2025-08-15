package Data.Service;

import Data.Constrcutor.NotificationServiceData;

public class CreateTrasactionServiceData extends NotificationServiceData {
    public CreateTrasactionServiceData(String message) {
        super(message);
        this.action = "create_transaction";
    }
}
