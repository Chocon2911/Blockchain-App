package Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class JsonBroadcaster {
    private static final String ipAddressFilePath = "Db/IpAddresses.json";

    public static void broadcast(String json) {
        List<String> ipAddresses = getLocalIpAddresses();
        int connectedCount = 0;

        for (String address : ipAddresses) {
            boolean connected = sendJsonToIp(address, json);
            if (!connected) removeIpFromLocalFile(address); else connectedCount++;
            if (connectedCount >= 8) return;
        }
    }

    public static boolean sendJsonToIp(String ip, String jsonLine) {
        try (Socket socket = new Socket()) {
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
            return true;
        } catch (IOException e) {
            System.err.println("Socket error with " + ip + ": " + e.getMessage());
            return false;
        }
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
            if (content.startsWith("[")) ipList = parseJsonStringArray(content);
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

    public static void removeIpFromLocalFile(String ipToRemove) {
        try {
            List<String> ipList = getLocalIpAddresses();
            if (ipList == null) ipList = new ArrayList<>();
            if (ipList.remove(ipToRemove)) writeJsonStringArrayToFile(ipList, ipAddressFilePath);
        } catch (Exception e) {
            System.err.println("Error removing ip from local file: " + e.getMessage());
        }
    }

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
                if (inStr) { res.add(cur.toString()); cur.setLength(0); inStr = false; }
                else { inStr = true; }
            } else if (inStr) {
                if (c == '\\') { if (i + 1 < jsonArray.length()) { cur.append(jsonArray.charAt(i + 1)); i++; } }
                else cur.append(c);
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
}
