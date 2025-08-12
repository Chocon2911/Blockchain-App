package Service;

import java.io.*;
import java.net.*;

public class PeerServiceLite {
    private static final int port = 18080;

    // Send JSON string or file path to localhost:port and print content
    public static void broadcast(String json) {
        String content = json;
        try {
            File maybeFile = new File(json);
            if (maybeFile.exists() && maybeFile.isFile()) {
                content = readAll(maybeFile);
            }
        } catch (Exception ignored) {}

        System.out.println("Broadcasting JSON content to localhost:" + 5000);
        String target = "127.0.0.1:" + 5000;
        boolean ok = sendJsonToIp(target, content);
        System.out.println("Broadcast to: " + target + " => " + (ok ? "SUCCESS" : "FAIL"));
        System.out.println("JSON content:\n" + content);
    }

    public static boolean sendJsonToIp(String ip, String jsonLine) {
        try (Socket socket = new Socket()) {
            String host = ip;
            int targetPort = port;
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
            return true;
        } catch (IOException e) {
            System.err.println("Socket error with " + ip + ": " + e.getMessage());
            return false;
        }
    }

    public static void listenAndPrintJsonOnPort(int p) {
        try (ServerSocket serverSocket = new ServerSocket(p)) {
            System.out.println("[Lite] JSON listener on port " + p + "...");
            while (true) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) sb.append(line);
                    String json = sb.toString();
                    System.out.println("[Lite] From " + socket.getRemoteSocketAddress() + ":\n" + json);
                    out.write("OK\n");
                    out.flush();
                } catch (IOException e) {
                    System.out.println("[Lite] Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[Lite] Can't open port: " + e.getMessage());
        }
    }

    public static void listenAndPrintJson() { listenAndPrintJsonOnPort(port); }

    private static String readAll(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        } catch (IOException e) { return null; }
    }
}
