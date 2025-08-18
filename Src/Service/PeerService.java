package Service;

import Model.Block;
import Model.Transaction;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerService {
    //==========================================Variable==========================================
    private static final String ipAddressFilePath = "Db/IpAddresses.json";
    private static final int newBlockPort = 18080;
    private static final int newTransactionPort = 18081;
    private static final int blockLocatorPort = 18082;
    private static final int blockchainPort = 18083;
    private static final String seedAddress = "http://127.0.0.1:18080";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final int maxConnection = 117;
    private static final Set<Socket> activeConnections = ConcurrentHashMap.newKeySet();
    private static final String ipAddressPath = "Db/IpAddresses.json";


     // danh sách host:port
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


    //===================================Mined Block Connection===================================
    //===Broadcast===
    public static void broadcastBlock(Block block) {
        if (block == null) {
            System.out.println("[broadcastBlock] Block null, bỏ qua.");
            return;
        }

        String blockJson = gson.toJson(block);
        if (blockJson == null || blockJson.isEmpty()) {
            System.out.println("[broadcastBlock] Cannot serialize Block to JSON.");
            return;
        }

        List<String> ipAddresses = getLocalIpAddresses();
        int connectedCount = 0;

        for (String address : ipAddresses) {
            boolean connected = sendJsonToIp(address, newBlockPort, blockJson);

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

         List<String> globalIpAddresses = getGlobalIpAddresses();
         for (String address : globalIpAddresses) {
             if (ipAddresses.contains(address)) continue;

             boolean connected = sendJsonToIp(address, newBlockPort, blockJson);
             if (connected) {
                 connectedCount++;
                 addIpToLocalFile(address);
             }
             if (connectedCount >= 8) break;
         }

        System.out.println("[broadcastBlock] Done. Successful peers: " + connectedCount);
    }

    public static boolean sendJsonToIp(String ip, int port, String jsonLine) {
        try (Socket socket = new Socket()) {
            // Support "ip" or "ip:port" formats
            String host = ip;
            if (ip.contains(":")) {
                String[] parts = ip.split(":", 2);
                host = parts[0];
            }

            socket.connect(new InetSocketAddress(host, port), 3000);
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
    public static void listenForBlock(int p) {
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
                        Block block = gson.fromJson(json, Block.class);
                        boolean valid = BlockchainService.examineBlock(block, clientSocket.getInetAddress().getHostAddress());
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



    //=================================New Transaction Connection=================================
    public static void broadcastTransaction(Transaction tx) {
        String txJson = TransactionService.toTransactionJson(tx);
        List<String> peerAddresses;
        // Read ip addresses file
        try (FileReader reader = new FileReader(ipAddressPath)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            peerAddresses = gson.fromJson(reader, listType);
        } catch (IOException e) {
            System.err.println("Can't read ip addresses: " + e.getMessage());
            return;
        }

        for (String addr : peerAddresses) {
            String[] parts = addr.split(":");
            String host = parts[0];

            try (Socket socket = new Socket(host, newTransactionPort)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                // truyền file json của object transaction cho nó
                out.println(txJson);
                out.flush();
                System.out.println("Sent TX to " + addr);
            } catch (IOException e) {
                System.err.println("Can't send TX to " + addr + ": " + e.getMessage());
            }
        }
    }

    public static void listenForTransactions(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening for transactions on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection from: " + socket.getRemoteSocketAddress());

                new Thread(() -> handleTransaction(socket)).start();
                System.out.println("Send succesfully");
            }

        } catch (IOException e) {
            System.err.println("ERROR listening for transactions: " + e.getMessage());
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

            Transaction tx = TransactionService.fromTransactionJson(json);
            boolean valid = TransactionService.validateTransaction(tx);

            if (valid) {
                System.out.println("Transaction valid: " + tx.getTxId());
                out.println("OK");
            } else {
                System.out.println("Transaction invalid: " + tx.getTxId());
                out.println("FAIL");
            }
        } catch (IOException e) {
            System.err.println("ERROR handling transaction: " + e.getMessage());
        }
    }



    //================================Blockchain Reorg Connection=================================
    public static void broadcastBlockLocator(String address) {
        try {
            String[] parts = address.split(":");
            String host = parts[0];

            List<String> locator = createBlockLocator();

            Socket socket = new Socket(host, blockLocatorPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(gson.toJson(locator));

            System.out.println("Sent block locator (" + locator.size() + " hashes) to " + address);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listenForBlockLocator() {
        try (ServerSocket serverSocket = new ServerSocket(blockLocatorPort)) {
            System.out.println("Listening for block locator on port " + blockLocatorPort + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String locatorJson = in.readLine();
                List<String> locator = gson.fromJson(locatorJson, List.class);

                System.out.println("Received block locator with " + locator.size() + " hashes.");

                // Find fork point
                Block forkBlock = findForkPoint(locator);
                if (forkBlock != null) {
                    System.out.println("Fork point at index: " + forkBlock.getIndex());

                    // Track from fork point to tip
                    List<Block> blocksToSend = new ArrayList<>();
                    int tipIndex = BlockchainService.getBlockCount() - 1;
                    for (int i = forkBlock.getIndex() + 1; i <= tipIndex; i++) {
                        Block b = BlockchainService.getBlock(i);
                        if (b != null) {
                            blocksToSend.add(b);
                        }
                    }

                    // Send blockchain to peer
                    broadcastBlockchain(blocksToSend, serverSocket.getInetAddress().getHostAddress());

                } else {
                    System.out.println("No common block found.");
                }
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> createBlockLocator() {
        List<String> locator = new ArrayList<>();
        int index = BlockchainService.getBlockCount() - 1;
        int step = 1;
        int added = 0;

        while (index >= 0) {
            Block block = BlockchainService.getBlock(index);
            locator.add(block.getHash());
            added++;

            if (added >= 4) step *= 2;
            index -= step;

            if (index < 0) {
                Block genesis = BlockchainService.getBlock(0);
                locator.add(genesis.getHash());
                break;
            }
        }
        return locator;
    }

    private static Block findForkPoint(List<String> locator) {
        for (String hash : locator) {
            int totalBlocks = BlockchainService.getBlockCount();
            for (int i = totalBlocks - 1; i >= 0; i--) {
                Block localBlock = BlockchainService.getBlock(i);
                if (localBlock.getHash().equals(hash)) {
                    return localBlock;
                }
            }
        }
        return null;
    }

    //===================================Blockchain Connection====================================
    public static void broadcastBlockchain(List<Block> blockchain, String address) {
        try {
            Socket socket = new Socket(address, blockchainPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String json = gson.toJson(blockchain);
            out.println(json);

            System.out.println("Broadcasted " + blockchain.size() + " blocks to " + address + ":" + blockchainPort);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listenForBlockchain() {
        try (ServerSocket serverSocket = new ServerSocket(blockchainPort)) {
            System.out.println("Listening for blockchain data on port " + blockchainPort + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String json = in.readLine();
                Block[] blocks = gson.fromJson(json, Block[].class);
                List<Block> receivedBlocks = Arrays.asList(blocks);

                if (receivedBlocks.isEmpty()) {
                    System.out.println("Received empty blockchain data. Skipping.");
                    clientSocket.close();
                    continue;
                }

                System.out.println("Received " + receivedBlocks.size() + " blocks from peer.");

                // Track earliest index
                int earliestIndex = receivedBlocks.stream()
                        .mapToInt(Block::getIndex)
                        .min()
                        .orElse(Integer.MAX_VALUE);

                // Delete blocks before earliest index
                int localCount = BlockchainService.getBlockCount();
                for (int i = earliestIndex; i < localCount; i++) {
                    BlockchainService.removeBlock(); // Xoá trong LevelDB
                }

                // Update block count
                File jsonFile = new File("Db/BlockCount.json");
                try (FileWriter writer = new FileWriter(jsonFile, false)) {
                    writer.write(String.valueOf(earliestIndex));
                }

                // 3️⃣ Add block to Db in order
                receivedBlocks.sort(Comparator.comparingInt(Block::getIndex));
                for (Block b : receivedBlocks) {
                    BlockchainService.addBlock(b);
                    System.out.println("Added block #" + b.getIndex());
                }

                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
