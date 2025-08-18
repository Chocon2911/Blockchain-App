package Main;

import Service.AppConnectorService;
import Service.BlockchainService;
import Service.TransactionService;
import Service.UTXOSet;

public class App {
    public static void main(String[] args) {
        new BlockchainService();
        new TransactionService();
        new UTXOSet();
        AppConnectorService.handleClient();
    }
}