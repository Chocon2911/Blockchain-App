package Service;

import Data.App.CreateTranasactionAppData;
import Data.App.CreateWalletAppData;
import Data.Service.CreateTrasactionServiceData;
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

                JsonObject response = new JsonObject();

                switch (action) {
                    case "create_wallet":
                        CreateWalletAppData data = new CreateWalletAppData(request);
                        createWallet(data);

                        response.addProperty("status", "success");

                        JsonObject wallet = new JsonObject();
                        wallet.addProperty("public_key", "PUB123456");
                        wallet.addProperty("private_key", "PRIV654321");

                        response.add("wallet", wallet);
                        break;

                    case "mining":
                        response.addProperty("status", "success");
                        response.addProperty("message", "Mining completed");
                        break;

                    case "create_transaction":
                        String from = request.get("from").getAsString();
                        String to = request.get("to").getAsString();
                        double amount = request.get("amount").getAsDouble();
                        // Giả lập xử lý giao dịch
                        response.addProperty("status", "success");
                        response.addProperty("tx_id", "TX999999");
                        break;

                    case "check_balance":
                        String publicKey = request.get("public_key").getAsString();
                        // Giả lập dữ liệu số dư
                        response.addProperty("status", "success");
                        response.addProperty("balance", 1000.5);
                        break;
                    case "create_block":
                        String previousHash = request.get("previous_hash").getAsString();
                    default:
                        response.addProperty("status", "error");
                        response.addProperty("message", "Unknown action");
                }

                // Trả về JSON dạng String
                out.println(response.toString());
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
                return gson.toJson(new CreateTrasactionServiceData("Insufficient balance"));
            }

            if (data.getFee() < 0) {
                return gson.toJson(new CreateTrasactionServiceData("Invalid fee"));
            }

            Transaction tx = new Transaction(data.getPublicAddressReceiver(), data.getAmount(), data.getPublicKeySenderAdapter(), data.getPrivateKeySenderAdapter());
        } catch (Exception e) {
            System.out.println("ERROR creating transaction");
            e.printStackTrace();
        }
        return null;
    }

    //=======================================Check Balance========================================
    private static String checkBalance() {

    }

    //===========================================Mining===========================================
    private static String mining() {

    }

    //========================================Create Block========================================
    private static String createBlock() {

    }
}
