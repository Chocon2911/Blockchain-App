package Service;

import Data.App.*;
import Data.Service.*;
import Model.Transaction;
import Model.Wallet;
import com.google.gson.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;

public class AppConnectorService {
    //==========================================Variable==========================================
    private static final int appPort = 5000;
    private static String appAddress = "127.0.0.1";
    public static final Gson gson = new GsonBuilder()
            // byte[] <-> Base64
            .registerTypeHierarchyAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(Base64.getEncoder().encodeToString(src))
            )
            .registerTypeHierarchyAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, typeOfT, context) ->
                    Base64.getDecoder().decode(json.getAsString())
            )
            // PublicKey <-> Base64
            .registerTypeHierarchyAdapter(PublicKey.class, (JsonSerializer<PublicKey>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(Base64.getEncoder().encodeToString(src.getEncoded()))
            )
            .registerTypeHierarchyAdapter(PublicKey.class, (JsonDeserializer<PublicKey>) (json, typeOfT, context) -> {
                try {
                    byte[] bytes = Base64.getDecoder().decode(json.getAsString());
                    KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Hoặc "RSA" nếu bạn dùng RSA
                    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
                    return keyFactory.generatePublic(spec);
                } catch (Exception e) {
                    throw new JsonParseException(e);
                }
            })
            .create();

    public static String getMyAddress(int port) {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface netIf = nets.nextElement();
                if (netIf.isLoopback() || !netIf.isUp()) continue;

                Enumeration<InetAddress> addresses = netIf.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress() + ":" + port;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1:" + port;
    }

    //============================================App=============================================
    public static void handleClient() {
        appAddress = getMyAddress(appPort);
        System.out.println("My address: " + appAddress);

        try (ServerSocket serverSocket = new ServerSocket(appPort)) {
            System.out.println("Server is listening on port " + appPort);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    new Thread(() -> {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                            while (true) {
                                System.out.println("Waiting for command...");
                                String command = in.readLine();
                                System.out.println("Received command: " + command);
                                if (command == null) {
                                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                                    break;
                                }

                                try {
                                    JsonObject request = JsonParser.parseString(command).getAsJsonObject();
                                    String action = request.get("action").getAsString();
                                    String response;

                                    switch (action) {
                                        case "create_wallet":
                                            System.out.println("Wallet created");
                                            CreateWalletAppData createWalletData = new CreateWalletAppData(request);
                                            response = createWallet(createWalletData);
                                            break;

                                        case "create_transaction":
                                            System.out.println("Transaction created");
                                            CreateTranasactionAppData createTransactionData = new CreateTranasactionAppData(request);
                                            response = createTransaction(createTransactionData);
                                            break;

                                        case "check_balance":
                                            System.out.println("Balance checked");
                                            CheckBalanceAppData checkBalanceData = new CheckBalanceAppData(request);
                                            response = checkBalance(checkBalanceData);
                                            break;

                                        case "create_block":
                                            System.out.println("Block created");
                                            CreateBlockAppData createBlockData = new CreateBlockAppData(request, out);
                                            response = createBlock(createBlockData);
                                            break;

                                        case "get_creating_block_data":
                                            System.out.println("Get creating block data");
                                            GetCreatingBlockAppData getProcessorAmountData = new GetCreatingBlockAppData(request);
                                            response = getCreatingBlock(getProcessorAmountData);
                                            System.out.println(response);
                                            break;

                                        case "turn_off":
                                            response = gson.toJson(new NotificationServiceData("info", "Server shutting down client..."));
                                            out.println(response);
                                            clientSocket.close();
                                            return;

                                        default:
                                            response = gson.toJson(new NotificationServiceData("error", "Invalid action"));
                                    }

                                    out.println(response);

                                } catch (Exception ex) {
                                    String error = gson.toJson(new NotificationServiceData("error", "Invalid request format"));
                                    out.println(error);
                                    System.out.println(ex);
                                }
                            }

                        } catch (IOException e) {
                            System.out.println("Connection error: " + e.getMessage());
                        }
                    }).start(); // chạy client handler trong thread riêng

                } catch (IOException e) {
                    System.out.println("Error accepting client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Lỗi server:");
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
        if (data.getPrivateKeySenderAdapter() == null || data.getPublicKeySenderAdapter() == null) {
            return gson.toJson(new CreateTransactionServiceData("Invalid private or public key", ""));
        }

        try {
            String publicAddressSender = TransactionService.getPublicAddress(data.getPublicKeySenderAdapter());
            long balance = UTXOSet.getBalance(publicAddressSender);
            System.out.println("Fee: " + data.getFee());
            System.out.println("Amount: " + data.getAmount());
            System.out.println("Fee + AMount: " + (data.getAmount() + data.getFee()));
            System.out.println("Balance: " + balance);
            if (data.getFee() < 0) {
                return gson.toJson(new CreateTransactionServiceData("Invalid fee", ""));
            }

            if (balance < data.getAmount() + data.getFee()) {
                return gson.toJson(new CreateTransactionServiceData("Insufficient balance", ""));
            }

            Transaction tx = new Transaction(data.getPrivateKeySenderAdapter(),
                    data.getPublicKeySenderAdapter(), data.getPublicAddressReceiver(),
                    data.getAmount(), data.getFee());
            System.out.println(TransactionService.toTransactionJson(tx));
            TransactionService.addToMempool(tx);
            return gson.toJson(new CreateTransactionServiceData("Success", tx.getTxId()));
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
            System.out.println("Balance of " + data.getPublicAddress() + ": " + balance);
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
        if (!data.isGood) return gson.toJson(new NotificationServiceData("create_block", "Failed"));
        if (data.getThreadAmount() <= 0 || data.getThreadAmount() > ComputerService.getProcessorCount() - 1) {
            return gson.toJson(new NotificationServiceData("create_block", "Invalid thread amount"));
        }
        return gson.toJson(new NotificationServiceData("create_block", "Success"));
    }

    //====================================Get Processor Amount====================================
    private static String getCreatingBlock(GetCreatingBlockAppData data) {
        return gson.toJson(new GetCreatingBlockServiceData());
    }
}
