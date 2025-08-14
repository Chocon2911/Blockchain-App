package Service;

import Model.Block;
import Model.Transaction;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PeerS {
    //==========================================Variable==========================================
    private static final String ipAddressFilePath = "Db/IpAddresses.json";
    private static final int port = 18080;
    private static final String seedAddress = "http://127.0.0.1:18080";
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



    //============================== Minimal Block JSON (de)serialization ==============================
    private static String blockToJson(Block b) {
        // Serialize các trường thiết yếu (không có getter version -> đặt mặc định)
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"type\":\"block\",");
        sb.append("\"index\":").append(b.getIndex()).append(',');
        sb.append("\"previousHash\":\"").append(escapeJson(b.getPreviousHash() == null ? "" : b.getPreviousHash())).append("\",");
        sb.append("\"difficulty\":").append(b.getDifficulty());
        sb.append('}');
        return sb.toString();
    }

    private static Block jsonToBlock(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            int index = extractJsonValueInt(json, "index", 0);
            String previousHash = extractJsonValueString(json, "previousHash", "");
            int difficulty = extractJsonValueInt(json, "difficulty", 1);

            // version mặc định vì không có getter; previousNChainWork=null; transactions rỗng
            return new Block(index, "0.0.0", previousHash, null, difficulty, new ArrayList<>());
        } catch (Exception e) {
            return null;
        }
    }

    private static int extractJsonValueInt(String json, String key, int def) {
        String v = extractJsonValueString(json, key, null);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private static String extractJsonValueString(String json, String key, String def) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);
        if (i < 0) return def;
        int colon = json.indexOf(':', i + k.length());
        if (colon < 0) return def;
        int j = colon + 1;
        while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
        if (j >= json.length()) return def;
        if (json.charAt(j) == '"') {
            int start = j + 1;
            int end = json.indexOf('"', start);
            if (end < 0) return def;
            return json.substring(start, end);
        } else {
            int end = j;
            while (end < json.length() && ",}\n\r\t ".indexOf(json.charAt(end)) == -1) end++;
            return json.substring(j, end);
        }
    }

    private static boolean validateBlock(Block block) {
        return false;
    }



    //===================================== Local JSON/File Utils =====================================
    private static String readAll(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static List<String> parseJsonStringArray(String jsonArray) {
        List<String> res = new ArrayList<>();
        if (jsonArray == null) return res;
        boolean inStr = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '"') {
                if (inStr) {
                    res.add(cur.toString());
                    cur.setLength(0);
                    inStr = false;
                } else {
                    inStr = true;
                }
            } else if (inStr) {
                if (c == '\\') { // escape
                    if (i + 1 < jsonArray.length()) {
                        char n = jsonArray.charAt(i + 1);
                        cur.append(n);
                        i++;
                    }
                } else {
                    cur.append(c);
                }
            }
        }
        return res;
    }

    private static void writeJsonStringArrayToFile(List<String> list, String path) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) bw.write(',');
                bw.write('"');
                bw.write(escapeJson(list.get(i)));
                bw.write('"');
            }
            bw.write(']');
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }



    //========================================IP Addresses========================================
    public static List<String> getGlobalIpAddresses() {
        List<String> ipList = new ArrayList<>();

        try {
            String jsonResponse = fetchJsonFromApi("/get_nodes");
            if (jsonResponse == null || jsonResponse.isEmpty()) return ipList;
            int nodesIdx = jsonResponse.indexOf("\"nodes\"");
            if (nodesIdx < 0) return ipList;
            int startArr = jsonResponse.indexOf('[', nodesIdx);
            int endArr = jsonResponse.indexOf(']', startArr + 1);
            if (startArr < 0 || endArr < 0) return ipList;
            String arr = jsonResponse.substring(startArr, endArr + 1);
            ipList = parseJsonStringArray(arr);
        } catch (Exception e) {
            System.err.println("Error parsing global ip addresses: " + e.getMessage());
        }

        return ipList;
    }

    public static List<String> getLocalIpAddresses() {
        List<String> ipList = new ArrayList<>();
        try {
            File file = new File(ipAddressFilePath);
            if (!file.exists()) return ipList;
            String content = readAll(file);
            if (content == null) return ipList;
            content = content.trim();
            if (content.isEmpty()) return ipList;
            if (content.startsWith("[")) {
                ipList = parseJsonStringArray(content);
            } else {
                // Fallback: newline-delimited format
                try (BufferedReader br = new BufferedReader(new StringReader(content))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) ipList.add(line);
                    }
                }
            }
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
                writeJsonStringArrayToFile(ipList, ipAddressFilePath);
            }
        } catch (IOException e) {
            System.err.println("Error writing to ip address file: " + e.getMessage());
        }
    }

    // thêm hàm này
    public static void removeIpFromLocalFile(String ipToRemove) {
        try {
            List<String> ipList = getLocalIpAddresses();
            if (ipList == null) ipList = new ArrayList<>();
            if (ipList.remove(ipToRemove)) {
                writeJsonStringArrayToFile(ipList, ipAddressFilePath);
            }
        } catch (Exception e) {
            System.err.println("Error removing ip from local file: " + e.getMessage());
        }
    }

    public static boolean sendBlockToIp(String ip, Block block) {
        try (Socket socket = new Socket()) {
            // Hỗ trợ định dạng "ip" hoặc "ip:port"
            String host = ip;
            int targetPort = 8333;
            if (ip.contains(":")) {
                String[] parts = ip.split(":", 2);
                host = parts[0];
                try {
                    targetPort = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            // Thiết lập timeout kết nối
            socket.connect(new InetSocketAddress(host, targetPort), 3000); // timeout 3s

            // Gửi block dưới dạng chuỗi JSON đơn giản (tránh phụ thuộc thư viện)
            OutputStream outputStream = socket.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            String payload = escapeJson(block.toString());
            String blockJson = "{\"type\":\"block\",\"payload\":\"" + payload + "\"}";
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



    //============================================Node============================================
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


    //======================================Block Connection======================================
    //===Broadcast===
    public static void broadcastBlock(Block block) {
        if (block == null) {
            System.out.println("[broadcastBlock] Block null, bỏ qua.");
            return;
        }

        String blockJson = blockToJson(block);
        if (blockJson == null || blockJson.isEmpty()) {
            System.out.println("[broadcastBlock] Cannot serialize Block to JSON.");
            return;
        }

        List<String> ipAddresses = getLocalIpAddresses();
        int connectedCount = 0;

        // Ưu tiên gửi tới danh sách local
        for (String address : ipAddresses) {
            boolean connected = sendJsonToIp(address, blockJson);

            if (!connected) {
                removeIpFromLocalFile(address);
            } else {
                connectedCount++;
            }

            if (connectedCount >= 8) {
                System.out.println("[broadcastBlock] Done. Successful peers: " + connectedCount);
                return;
            }
        }

         // Bổ sung từ danh sách global nếu chưa đủ 8
         List<String> globalIpAddresses = getGlobalIpAddresses();
         for (String address : globalIpAddresses) {
             if (ipAddresses.contains(address)) continue;

             boolean connected = sendJsonToIp(address, blockJson);
             if (connected) {
                 connectedCount++;
                 addIpToLocalFile(address);
             }
             if (connectedCount >= 8) break;
         }

        System.out.println("[broadcastBlock] Done. Successful peers: " + connectedCount);
    }

    public static boolean sendJsonToIp(String ip, String jsonLine) {
        try (Socket socket = new Socket()) {
            // Support "ip" or "ip:port" formats
            String host = ip;
            int targetPort = 8333;
            if (ip.contains(":")) {
                String[] parts = ip.split(":", 2);
                host = parts[0];
                try { targetPort = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
            }

            socket.connect(new InetSocketAddress(host, targetPort), 3000);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(jsonLine);
            writer.newLine();
            writer.flush();

            // For raw JSON, we don't require a specific ACK; send success if no exception
            return true;
        } catch (IOException e) {
            System.err.println("Socket error with " + ip + ": " + e.getMessage());
            return false;
        }
    }

    //===Listen===
    public static void ListenForBlock(int p) {
        try (ServerSocket serverSocket = new ServerSocket(p)) {
            System.out.println("Block listener running on port " + p + " (max 117 concurrent)...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                if (activeConnections.size() >= maxConnection) {
                    try (OutputStream out = clientSocket.getOutputStream()) {
                        out.write("Too many connections. Try again later.".getBytes());
                    } catch (IOException ignored) {}
                    clientSocket.close();
                    continue;
                }

                activeConnections.add(clientSocket);

                new Thread(() -> {
                    try (Socket socket = clientSocket;
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                        StringBuilder received = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            received.append(line);
                        }

                        String json = received.toString();
                        Block block = jsonToBlock(json);
                        boolean valid = validateBlock(block);
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("AnBlock.json"))) {
                            writer.write(json);
                            System.out.println("BLOCK JSON saved to AnBlock.json");
                        } catch (IOException e) {
                            System.err.println("Error writing BLOCK JSON: " + e.getMessage());
                        }
                        System.out.println("[ListenOnPort] Received block from " + socket.getRemoteSocketAddress() + ", valid=" + valid);
                        out.write((valid ? "OK" : "INVALID") + "\n");
                        out.flush();

                        if (valid) {
                            broadcastBlock(block);
                        }

                    } catch (Exception e) {
                        System.out.println("[ListenOnPort] Error: " + e.getMessage());
                    } finally {
                        activeConnections.remove(clientSocket);
                    }
                }).start();
            }
        } catch (IOException e) {
            System.out.println("[ListenOnPort] Can't open port: " + e.getMessage());
        }
    }



    //===================================Transaction Connection===================================
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

    private static void handleTransaction(Socket socket){
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


    