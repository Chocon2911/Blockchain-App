package Service;

import Data.Service.NotificationServiceData;
import Model.Block;
import Model.Transaction;
import com.google.gson.*;

import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinerService {
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

    public static int getAllProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void startMine(int processorsLimit,
                                 List<Transaction> addedTransactions, Block newBlock, PrintWriter out) {
        AtomicBoolean foundNonce = new AtomicBoolean(false);
        System.out.println("Start mining...");
        for (int i = 0; i < processorsLimit; i++) {
            createMiningThread(newBlock, i + 1, processorsLimit, foundNonce, out);
        }
    }

    private static void createMiningThread(Block block, int index,
                                           int processorsLimit, AtomicBoolean isFound, PrintWriter out) {
        int count = 0;
        while (!isFound.get()) {
            int nonce = index + processorsLimit * count;
            isFound.set(block.mineBlock(nonce));
            count++;

            if (!isFound.get()) continue;
            if (!BlockchainService.examineBlock(block, null)) {
                String response = gson.toJson(new NotificationServiceData("mined_block", "Failed"));
                System.out.println(response);
                out.println(response);
                return;
            }
            for (Transaction tx : block.getTransactions()) {
                UTXOSet.updateWithTransaction(tx);
            }

            System.out.println("Mined a block!");
            BlockchainService.addBlock(block);
            PeerService.broadcastBlock(block);
            String response = gson.toJson(new NotificationServiceData("mined_block", "Success"));
            System.out.println(response);
            out.println(response);
        }
    }
}
