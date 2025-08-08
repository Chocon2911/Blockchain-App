package Service;

import Model.Block;
import Model.Transaction;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerService {
    private static final String ipAddressFilePath = "Db/IpAddresses.json";
    private static final int port = 18080;
    private static final String seedAddress = "127.0.0.1:18080";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void updateCurrNodeAddress(String address) {

    }

    //======================================Api Call Support======================================
    private static String fetchJsonFromApi(String urlString) {
        try {
            URL url = new URL(seedAddress + urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                return response.toString();
            }
            else {
                System.out.println("HTTP GET request failed. Response Code: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean postJson(String apiUrl, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            return statusCode >= 200 && statusCode < 300;
        } catch (Exception e) {
            return false;
        }
    }

    //=======================================DNS Seed Node========================================
    public static List<String> getIpAddresses() {
        List<String> ipList = new ArrayList<>();

        try {
            String jsonResponse = fetchJsonFromApi("/dnsseed/nodes");

            if (jsonResponse != null) {
                JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
                JsonArray nodesArray = jsonObject.getAsJsonArray("nodes");

                for (JsonElement element : nodesArray) {
                    ipList.add(element.getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ipList;
    }

    public static List<String> getLocalIpAddresses() {
        List<String> ipList = new ArrayList<>();
        try {
            Gson gson = new Gson();
            FileReader reader = new FileReader(ipAddressFilePath);
            Type listType = new TypeToken<List<String>>() {}.getType();
            ipList = gson.fromJson(reader, listType);
            reader.close();
        } catch (Exception e) {
            System.err.println("Error reading ip addresses file: " + e.getMessage());
        }
        return ipList;
    }

    //============================================Node============================================
    public static boolean performHandshake(String ip, int port) {
        try (Socket socket = new Socket(ip, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Gửi message "version"
            String versionMsg = "{ \"type\": \"version\", \"protocol\": 70015 }";
            out.writeUTF(versionMsg);

            // Nhận message "verack"
            String response = in.readUTF();
            if (response.contains("verack")) {
                System.out.println("Successfully hansake with " + ip);
                return true;
            }

        } catch (IOException e) {
            System.err.println("Can't connect to " + ip + ": " + e.getMessage());
        }
        return false;
    }

    //=========================================Broadcast==========================================
    public static void broadcastBlock(Block block) {

    }

    public static void broadcastTransaction(Transaction transaction) {

    }

    //===========================================Listen===========================================
    public static void listen4NewTransaction() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService threadPool = Executors.newCachedThreadPool();
    }

    public static void listen4NewBlock() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService threadPool = Executors.newCachedThreadPool();
    }

    private static void listenOnPort(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuilder received = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        received.append(line);
                    }

                    String json = received.toString();
                    if (!handleReceivedJson(json)) {
                        System.out.println("Invalid json: " + json);
                    }

                } catch (IOException e) {
                    System.out.println("Connection lost: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Can't open port: " + e.getMessage());
        }
    }

    private static boolean handleReceivedJson(String json) {
        // Check json format if Block or Transaction then handle it
        return true;
    }
}
