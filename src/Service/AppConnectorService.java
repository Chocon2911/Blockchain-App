package Service;

import Data.App.CheckBalanceAppData;
import Data.App.CreateBlockAppData;
import Data.App.CreateTranasactionAppData;
import Data.App.CreateWalletAppData;
import Data.Service.NotificationServiceData;
import Data.Service.CheckBalanceServiceData;
import Data.Service.CreateWalletServiceData;
import Model.Transaction;
import Model.Wallet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AppConnectorService {
    //==========================================Variable==========================================
    private static final int appPort = 2025;
    private static final String appAddress = "127.0.0.1";
    private static Gson gson = new Gson();

    //============================================App=============================================
    public static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String command;
            while ((command = in.readLine()) != null) {
                // Parse request từ JSON string
                JsonObject request = JsonParser.parseString(command).getAsJsonObject();
                String action = request.get("action").getAsString();

                String response = "";

                switch (action) {
                    case "create_wallet":
                        CreateWalletAppData createWalletData = new CreateWalletAppData(request);
                        response = createWallet(createWalletData);
                        break;

                    case "create_transaction":
                        CreateTranasactionAppData createTransactionData = new CreateTranasactionAppData(request);
                        response = createTransaction(createTransactionData);
                        break;

                    case "check_balance":
                        CheckBalanceAppData checkBalanceData = new CheckBalanceAppData(request);
                        response = checkBalance(checkBalanceData);
                        break;
                    case "create_block":
                        CreateBlockAppData createBlockData = new CreateBlockAppData(request);
                        response = createBlock(createBlockData);
                        break;
                    case "get_processor_amount":
                        break;
                    case "turn_off":

                    default:
                        response = gson.toJson(new NotificationServiceData("error", "Invalid action"));
                }

                out.println(response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //=======================================Create Wallet========================================
    private static String createWallet(CreateWalletAppData data) {
        try {
            Wallet wallet = new Wallet();
            return new CreateWalletServiceData(wallet).toJson();
        } catch (Exception e) {
            System.out.println("ERROR creating Wallet");
            e.printStackTrace();
            return null;
        }
    }

    //=====================================Create Transaction=====================================
    private static String createTransaction(CreateTranasactionAppData data) {
        try {
            String publicAddressSender = TransactionService.getPublicAddress(data.getPublicKeySenderAdapter());
            long balance = UTXOSet.getBalance(publicAddressSender);
            if (balance < data.getAmount() + data.getFee()) {
                return gson.toJson(new NotificationServiceData("create_transaction", "Insufficient balance"));
            }

            if (data.getFee() < 0) {
                return gson.toJson(new NotificationServiceData("create_transaction", "Invalid fee"));
            }

            Transaction tx = new Transaction(data.getPrivateKeySenderAdapter(),
                    data.getPublicKeySenderAdapter(), data.getPublicAddressReceiver(),
                    data.getAmount(), data.getFee());
            return gson.toJson(new NotificationServiceData("create_transaction", "Success"));
        } catch (Exception e) {
            System.out.println("ERROR creating transaction");
            e.printStackTrace();
        }

        return null;
    }

    //=======================================Check Balance========================================
    private static String checkBalance(CheckBalanceAppData data) {
        try {
            if (data.getPublicAddress() == null) {
                return gson.toJson(new NotificationServiceData("check_balance", "Invalid public address"));
            }
            long balance = UTXOSet.getBalance(data.getPublicAddress());
            if (UTXOSet.getBalance(data.getPublicAddress()) < 0) {
                return gson.toJson(new NotificationServiceData("check_balance", "Invalid public address"));
            }
            return gson.toJson(new CheckBalanceServiceData(balance));
        } catch (Exception e) {
            System.out.println("ERROR checking balance");
            e.printStackTrace();
        }
        return null;
    }

    //========================================Create Block========================================
    private static String createBlock(CreateBlockAppData data) {
        if (data.getThreadAmount() <= 0 || data.getThreadAmount() > ComputerService.getProcessorCount() - 1) {
            return gson.toJson(new NotificationServiceData("create_block", "Invalid thread amount"));
        }
        return gson.toJson(new NotificationServiceData("create_block", "Success"));
    }
}
