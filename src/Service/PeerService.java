package Service;

import Model.Block;
import Model.Transaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PeerService {
    private static final int port = 18080;
    private static final String seedAddress = "127.0.0.1:8080";
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void updateCurrNodeAddress(String address) {

    }

    public static List<String> getPeers() {

    }

    //======================================Api Call Support======================================
    public static String fetchJsonOrErrorCode(String apiUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(apiUrl))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return response.body();
            } else {
                return String.valueOf(statusCode);
            }
        } catch (Exception e) {
            return e.getMessage();
        }
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

    public static void broadcastBlock(Block block) {
    }

    public static void broadcastTransaction(Transaction transaction) {

    }

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
        // Check json format if Block or Transaction and handle it
        return true;
    }
}
