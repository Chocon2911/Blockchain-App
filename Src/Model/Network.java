import java.util.*;

public class Network {
    public static List<Transaction> tempmem = new ArrayList<>();

    public static void broadcastTransaction(Transaction tx) {
        System.out.println("Phát tán giao dịch lên mạng lưới blockchain");
        tempmem.add(tx);
        System.out.println("Giao dịch đang chờ trong bộ nhớ tạm. Số giao dịch: " + tempmem.size());
    }
}
