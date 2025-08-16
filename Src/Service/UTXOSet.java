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
import org.iq80.leveldb.*;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


import com.google.gson.Gson;

public class UTXOSet {
    private static DB db;
    private static final Gson gson = new Gson();
    private static final String dbPath = "Db/UTXOSet.db";

    static {
        try {
            Options options = new Options();
            options.createIfMissing(true);
            db = factory.open(new File(dbPath), options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        List<UTXO> utxos = new java.util.ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String json = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                UTXO utxo = gson.fromJson(json, UTXO.class);
                if (utxo.getPubAdd().equals(pubAdd)) {
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
        db.delete(bytes(makeKey(txId, index)));
    }

    public static long getBalance(String pubAdd) {
        long total = -1;
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                String json = new String(iterator.peekNext().getValue(), StandardCharsets.UTF_8);
                UTXO utxo = gson.fromJson(json, UTXO.class);
                if (utxo.getPubAdd().equals(pubAdd)) {
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
            removeUTXO(in.getPrevTxId(), in.getPrevIndex());
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
