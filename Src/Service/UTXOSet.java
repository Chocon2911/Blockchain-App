//package Model;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.Map;
//
//public class UTXOSet {
//    private Map<String, UTXO> utxos = new HashMap<>();
//
//
//    private String makeKey(String txId, int index) {
//        return txId + ":" + index;
//    }
//
//    public UTXO getUTXO(String txId, int index) {
//        return utxos.get(makeKey(txId, index));
//    }
//
//    public void addUTXO(UTXO utxo) {
//        utxos.put(makeKey(utxo.getTxId(), utxo.getIndex()), utxo);
//    }
//
//    public void removeUTXO(String txId, int index) {
//        utxos.remove(makeKey(txId, index));
//    }
//
//    public double getBalance(String pubAdd) {
//        return utxos.values().stream()
//                .filter(u -> u.getPubAdd().equals(pubAdd))
//                .mapToDouble(UTXO::getValue)
//                .sum();
//    }
//
//
//
//}
//


package Service;

import Model.Transaction;
import Model.TxIn;
import Model.TxOut;
import Model.UTXO;
import com.google.gson.*;
import org.iq80.leveldb.*;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class UTXOSet {
    private static DB db;
    private static final String dbPath = "Db/UTXOSet.db";
    static {
        try {
            Options options = new Options();
            options.createIfMissing(true);
            db = factory.open(new File(dbPath), options);

            // In ra tất cả key-value trong DB
            try (DBIterator iterator = db.iterator()) {
                iterator.seekToFirst();
                System.out.println("===== Database Content =====");
                while (iterator.hasNext()) {
                    Map.Entry<byte[], byte[]> entry = iterator.next();
                    String key = new String(entry.getKey(), StandardCharsets.UTF_8);
                    String value = new String(entry.getValue(), StandardCharsets.UTF_8);
                    System.out.println(key + " = " + value);
                }
                System.out.println("============================");
            }

        } catch (IOException e) {
            throw new RuntimeException("Can't open database", e);
        }
    }

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

    private static String makeKey(String txId, int index) {
        return txId + ":" + index;
    }

    public static UTXO getUTXO(String txId, int index) {
        byte[] data = db.get(bytes(makeKey(txId, index)));
        if (data == null) return null;
        String json = new String(data, StandardCharsets.UTF_8);
        return gson.fromJson(json, UTXO.class);
    }

    public static List<UTXO> getUTXOsByPubAdd(String pubAdd) {
        System.out.println("getUTXOsByPubAdd: " + pubAdd);
        List<UTXO> utxos = new java.util.ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String json = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                UTXO utxo = gson.fromJson(json, UTXO.class);
                System.out.println("utxo: " + utxo.getPubAdd());
                if (utxo.getPubAdd().equals(pubAdd)) {
                    utxos.add(utxo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return utxos;
    }

    public static List<UTXO> getUnlockedUTXOsByPubAdd(String pubAdd) {
        List<UTXO> utxos = new java.util.ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String json = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                System.out.println("json: " + json);
                UTXO utxo = gson.fromJson(json, UTXO.class);
                if (utxo.getPubAdd().equals(pubAdd) && !utxo.getIsLocked()) {
                    utxos.add(utxo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return utxos;
    }

    public static void addUTXO(UTXO utxo) {
        String key = makeKey(utxo.getTxId(), utxo.getIndex());
        String json = gson.toJson(utxo);
        db.put(bytes(key), bytes(json));
    }

    public static void removeUTXO(String txId, int index) {
        System.out.println("Remove UTXO: " + new String(bytes(makeKey(txId, index)), StandardCharsets.UTF_8));
        db.delete(bytes(makeKey(txId, index)));
    }

    public static long getBalance(String pubAdd) {
        long total = 0;
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String json = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                UTXO utxo = gson.fromJson(json, UTXO.class);
                if (utxo.getPubAdd().equals(pubAdd) && !utxo.getIsLocked()) {
                    total += utxo.getValue();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return total;
    }

    public static void updateWithTransaction(Transaction tx) {
        // Xóa UTXO đã tiêu
        for (TxIn in : tx.getInputs()) {
            removeUTXO(in.getPrevTxId(), in.getOutputIndex());
        }

        // Thêm UTXO mới
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            TxOut out = tx.getOutputs().get(i);
            addUTXO(new UTXO(tx.getTxId(), i, out.getValue(), out.getPublicAdd(), false));
        }

        for (int i = 0; i < tx.utxos.size(); i++) {
            addUTXO(tx.utxos.get(i));
        }
    }


    public static void closeDB() throws IOException {
        db.close();
    }

    private static byte[] bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
