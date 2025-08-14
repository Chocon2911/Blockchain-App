package Main;

import Service.PeerService;
import Model.Block;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class AnTest {
    public static void main(String[] args) {
    System.out.println("Test send/receive Block over loopback (new requirement)");

        final int listenPort = 5000;

        // 1) Khởi động listener cho Block (nghe tối đa 117 node)
        new Thread(() -> PeerService.listenForBlock(listenPort)).start();
        sleep(600);

        // 2) Đảm bảo có địa chỉ 127.0.0.1:5000 trong danh bạ local để broadcast tới
        PeerService.addIpToLocalFile("127.0.0.1:" + listenPort);

        // 3) Tạo 1 Block mẫu để phát tán
        int index = 1;
        String version = "0.0.0";
        String previousHash = "0000000000000000000000000000000000000000000000000000000000000000";
        BigInteger previousNChainWork = null;
        int difficulty = 1;
    Block block = new Block(index, version, previousHash, previousNChainWork, difficulty, null);

        // 4) Phát tán block đến tối đa 8 node (ở đây có 127.0.0.1:5000)
        PeerService.broadcastBlock(block);

    System.out.println("Broadcasted block.");

        // Giữ tiến trình một lúc để xem log
        sleep(1500);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
