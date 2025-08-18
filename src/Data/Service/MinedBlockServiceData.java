package Data.Service;

import Data.Constrcutor.ServiceData;

public class MinedBlockServiceData extends ServiceData {
    private String message;

    public MinedBlockServiceData(String message) {
        this.action = "mined_block";
        this.message = message;
    }
}
