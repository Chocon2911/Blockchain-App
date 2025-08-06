import java.util.*;

public class BlockchainDemo {
    public static void main(String[] args) throws Exception {
        Wallet sender = new Wallet();
        Wallet receiver = new Wallet();

        Map<String, Float> balances = new HashMap<>();
        Map<String, PublicKey> addressBook = new HashMap<>();

        balances.put(sender.getAddress(), 500.0f);
        addressBook.put(sender.getAddress(), sender.getPublicKey());
        addressBook.put(receiver.getAddress(), receiver.getPublicKey());

        Transaction tx = new Transaction(
                sender.getAddress(),
                receiver.getAddress(),
                100.0f,
        );

        tx.signTransaction(sender);

        System.out.println("Giao dịch vừa được tạo:");
        System.out.println(tx);

        Network.broadcastTransaction(tx);

        Miner.mineBlock(Network.tempmem, balances, addressBook);
    }
}
