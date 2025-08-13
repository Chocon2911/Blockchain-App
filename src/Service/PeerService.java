package Service;

import Model.Block;
import Model.Transaction;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.net.Socket;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerService {
    //==========================================Variable==========================================
    private static final String ipAddressFilePath = "Db/IpAddresses.json";
    private static final int port = 18080;
    private static final String seedAddress = "127.0.0.1:18080";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final int maxConnection = 117;
    private static final Set<Socket> activeConnections = ConcurrentHashMap.newKeySet();

    private static final String ipAddressPath = "Db/IpAddresses.json";


     // danh sách host:port
    private static final Gson gson = new Gson();




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



    //========================================IP Addresses========================================
    public static List<String> getGlobalIpAddresses() {
        List<String> ipList = new ArrayList<>();

        try {
            String jsonResponse = fetchJsonFromApi("/get_nodes");

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

    public static void addIpToLocalFile(String newIp) {
        try {
            List<String> ipList = getLocalIpAddresses();
            if (!ipList.contains(newIp)) {
                ipList.add(newIp);
                Gson gson = new Gson();
                FileWriter writer = new FileWriter(ipAddressFilePath);
                gson.toJson(ipList, writer);
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error writing to ip address file: " + e.getMessage());
        }
    }

    public static boolean sendBlockToIp(String ip, Block block) {
        try (Socket socket = new Socket()) {
            // Thiết lập timeout kết nối
            socket.connect(new InetSocketAddress(ip, 8333), 3000); // timeout 3s

            // Gửi block dưới dạng JSON
            OutputStream outputStream = socket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            String blockJson = new Gson().toJson(block);
            writer.write(blockJson);
            writer.newLine(); // kết thúc dòng để phía nhận biết
            writer.flush();

            // Đọc phản hồi (nếu cần)
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = reader.readLine();
            return "OK".equalsIgnoreCase(response);

        } catch (IOException e) {
            System.err.println("Socket error with " + ip + ": " + e.getMessage());
            return false;
        }
    }

    public static void listenForConnections() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening port: " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                if (activeConnections.size() >= maxConnection) {
                    // Quá nhiều kết nối, trả về thông báo từ chối
                    try (OutputStream out = clientSocket.getOutputStream()) {
                        out.write("Too many connections. Try again later.".getBytes());
                    } catch (IOException ignored) {}
                    clientSocket.close();
                    continue;
                }

                // Thêm kết nối vào danh sách đang hoạt động
                activeConnections.add(clientSocket);
                System.out.println("Connected to: " + clientSocket.getRemoteSocketAddress());

                // Tạo luồng riêng cho mỗi kết nối
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Đọc và xử lý dữ liệu (ví dụ đơn giản)
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Nhận được từ " + socket.getRemoteSocketAddress() + ": " + line);
                out.write(("Echo: " + line + "\n").getBytes());
            }

        } catch (IOException e) {
            System.out.println("Ngắt kết nối với: " + clientSocket.getRemoteSocketAddress());
        } finally {
            activeConnections.remove(clientSocket);
        }
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
                System.out.println("Successfully handshake with " + ip);
                return true;
            }

        } catch (IOException e) {
            System.err.println("Can't connect to " + ip + ": " + e.getMessage());
        }
        return false;
    }

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
        return "127.0.0.1:" + port; // fallback nếu không lấy được IP mạng
    }

    public static void createIpAddressessFile(List<String> ips) {
        try {
            File folder = new File("Db");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File file = new File(ipAddressPath);
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(ips, writer); // ghi nguyên list thành JSON array
            }

            System.out.println("Đã lưu file địa chỉ peer: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo file địa chỉ: " + e.getMessage());
        }
    }




    //=========================================Broadcast==========================================
    public static void broadcastBlock(Block block) {

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
//==========================================getPeer===========================================

    public List getPeersFromNode(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Gửi lệnh GETADDR
            out.println("GETADDR");
            out.flush();

            // Nhận JSON
            String json = in.readLine();
            return gson.fromJson(json, List.class);
        }
    }

    /**
     * Broadcast transaction tới tất cả peers (theo giới hạn kết nối)
     */
    public static void broadcastTransaction(Transaction tx) {
        String txJson = gson.toJson(tx);
        List<String> peerAddresses;
        // đọc file json
        try (FileReader reader = new FileReader(ipAddressPath)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            peerAddresses = gson.fromJson(reader, listType);
        } catch (IOException e) {
            System.err.println("Lỗi đọc file địa chỉ peer: " + e.getMessage());
            return;
        }

        for (String addr : peerAddresses) {
            String[] parts = addr.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // truyền file json của object transaction cho nó
                out.println("TX " + txJson);
                out.flush();
                System.out.println("Đã gửi TX tới " + addr);
            } catch (IOException e) {
                System.err.println("Không thể gửi TX tới " + addr + ": " + e.getMessage());
            }
        }
    }


//==========================l?ng nghe v… nh?n file json t? node kh c==========================
    public static void listenForTransactions(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening for transactions on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Kết nối từ: " + socket.getRemoteSocketAddress());

                new Thread(() -> handleTransaction(socket)).start();
                System.out.println("Send succesfully");
            }

        } catch (IOException e) {
            System.err.println("Lỗi khi mở cổng: " + e.getMessage());
        }
    }

    private static void handleTransaction(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = in.readLine();
            System.out.println(json);
            if (json == null) {
                out.println("ERROR: No data received");
                return;
            }

            // Parse JSON thành object Transaction
            Transaction tx = gson.fromJson(json, Transaction.class);

            // Xác thực transaction
            boolean valid = TransactionService.validateTransaction(tx);

            if (valid) {
                System.out.println("✅ Transaction hợp lệ: " + tx.getTxId());
                out.println("OK");
            } else {
                System.out.println("❌ Transaction không hợp lệ: " + tx.getTxId());
                out.println("FAIL");
            }

        } catch (IOException e) {
            System.err.println("Lỗi khi nhận transaction: " + e.getMessage());
        }
    }

}
