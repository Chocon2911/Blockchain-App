package Data.Service;

import Data.Constrcutor.ServiceData;
import Service.ComputerService;
import Service.TransactionService;

import java.util.List;

public class GetCreatingBlockServiceData extends ServiceData {
    private int processorAmount;
    private List<String> txIds;

    public GetCreatingBlockServiceData() {
        this.action = "get_processor_amount";
        this.processorAmount = ComputerService.getProcessorCount() - 1;
        this.txIds = TransactionService.getAllKeys();
    }

    public int getProcessorAmount() { return processorAmount; }
    public List<String> getTxIds() { return txIds; }
}
