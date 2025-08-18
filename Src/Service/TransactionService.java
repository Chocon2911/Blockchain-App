package Service;

import Model.*;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.Gson;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.google.gson.*;
import org.bitcoinj.core.Base58;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.fusesource.leveldbjni.JniDBFactory.factory;


public class TransactionService {
    //==========================================Variable==========================================
    private static DB db;
    private static final String dbPath = "Db/Mempool.db";
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

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    //=============================================Db=============================================
    public static void addToMempool(Transaction transaction) {
        try {
            String key = transaction.getTxId();
            String value = TransactionService.toTransactionJson(transaction);
            db.put(key.getBytes(), value.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeFromMempool(Transaction transaction) {
        try {
            String key = transaction.getTxId();
            db.delete(key.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Transaction getTransaction(String txId) {
        try {
            byte[] data = db.get(txId.getBytes());
            if (data == null) return null;
            return fromTransactionJson(new String(data, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getAllKeys() {
        List<String> keys = new java.util.ArrayList<>();
        try (DBIterator iterator = db.iterator()) {
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                keys.add(new String(iterator.peekNext().getKey(), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keys;
    }

    //=========================================Queue util=========================================
    public static boolean validateTransaction(Transaction tx) throws IOException {
        if (tx != null) return true;
        // 1. Transaction phải có input và output
        if (tx.isCoinbase()) return true;
        if (tx.getInputs() == null || tx.getInputs().isEmpty()) {
            System.out.println("Transaction không có input");
            return false;
        }
        if (tx.getOutputs() == null || tx.getOutputs().isEmpty()) {
            System.out.println("Transaction không có output");
            return false;
        }

        long totalInputValue = 0;
        long totalOutputValue = 0;

        // 2. Kiểm tra từng input
        for (TxIn txIn : tx.getInputs()) {
            // Lấy UTXO từ UTXOSet
            UTXO utxo = UTXOSet.getUTXO(txIn.getPrevTxId(), txIn.getOutputIndex());
            if (utxo == null) {
                System.out.println("UTXO không tồn tại hoặc đã tiêu: "
                        + txIn.getPrevTxId() + ":" + txIn.getOutputIndex());
                return false;
            }

            // Xác minh chữ ký với publicKey của UTXO
            try {
                boolean isValidSignature = CryptoUtil.verifySignature(
                        txIn.getSignature(),
                        tx.getTxId().getBytes(),
                        txIn.getPubKey()
                );

                if (!isValidSignature) {
                    System.out.println("Chữ ký không hợp lệ cho input: " + txIn.getPrevTxId());
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            totalInputValue += utxo.getValue();
        }
        //UTXO laf 1 phan output chinhr total output value
        // Tính tổng giá trị output
        for (TxOut txOut : tx.getOutputs()) {
            totalOutputValue += txOut.getValue();
        }

        if (totalInputValue < totalOutputValue) {
            System.out.println("Giá trị input nhỏ hơn output");
            return false;
        }

        return true;
    }

    public boolean verifyTransaction(Transaction tx) throws IOException {
        // 1. Kiểm tra số lượng input/output
        if (tx.getInputs().isEmpty() || tx.getOutputs().isEmpty()) {
            System.out.println("❌ Transaction không có input hoặc output");
            return false;
        }

        // 2. Kiểm tra kích thước transaction
        if (tx.getSize() > 100_000) { // Giới hạn 100 KB
            System.out.println("❌ Transaction vượt quá kích thước cho phép");
            return false;
        }

        long totalInputValue = 0;
        long totalOutputValue = 0;

        // 3. Kiểm tra từng input
        for (TxIn in : tx.getInputs()) {
            // 3.1 Kiểm tra UTXO tồn tại
            UTXO referencedUTXO = UTXOSet.getUTXO(in.getPrevTxId(), in.getOutputIndex());
            if (referencedUTXO == null) {
                System.out.println("❌ UTXO không tồn tại hoặc đã bị tiêu");
                return false;
            }

            // 3.2 Xác minh chữ ký
            boolean signatureValid = CryptoUtil.verifySignature(
                    in.getRawDataToSign(), // dữ liệu cần ký
                    in.getSignature(),
                    in.getPubKey()
            );
            if (!signatureValid) {
                System.out.println("❌ Chữ ký không hợp lệ");
                return false;
            }

            totalInputValue += referencedUTXO.getValue();
        }

        // 4. Kiểm tra giá trị output
        for (TxOut out : tx.getOutputs()) {
            if (out.getValue() < 0) {
                System.out.println("❌ Output có giá trị âm");
                return false;
            }
            totalOutputValue += out.getValue();
        }

        // 5. Kiểm tra tổng giá trị
        if (totalInputValue < totalOutputValue) {
            System.out.println("❌ Tổng input < tổng output");
            return false;
        }

        // 6. Phí giao dịch hợp lệ (có thể = 0)
        long fee = totalInputValue - totalOutputValue;
        if (fee < 0) {
            System.out.println("❌ Fee âm");
            return false;
        }

        // 7. Pass tất cả
        System.out.println("✅ Transaction hợp lệ. Fee: " + fee);
        return true;
    }



    //========================================Json Convert========================================
    //===Wallet===
    public static Wallet fromWalletJson(String json) {
        return gson.fromJson(json, Wallet.class);
    }

    public static String toWalletJson(Wallet wallet) {
        return gson.toJson(wallet);
    }


    //===Transcation===
    public static Transaction fromTransactionJson(String json) {
        return gson.fromJson(json, Transaction.class);
    }

    public static String toTransactionJson(Transaction transaction) {
        return gson.toJson(transaction);
    }

    //===TxIn===
    public static TxIn fromTxInJson(String json) {
        return gson.fromJson(json, TxIn.class);
    }

    public static String toTxInJson(TxIn txIn) {
        return gson.toJson(txIn);
    }

    //===TxOut===
    public static TxOut fromTxOutJson(String json) {
        return gson.fromJson(json, TxOut.class);
    }

    public static String toTxOutJson(TxOut txOut) {
        return gson.toJson(txOut);
    }

    //==========================================Support===========================================
    public static String getPublicAddress(PublicKey publicKey) {
        try {
            byte[] pubKeyBytes = publicKey.getEncoded();

            // SHA-256
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] shaHashed = sha256.digest(pubKeyBytes);

            // RIPEMD-160
            MessageDigest ripemd160 = MessageDigest.getInstance("RIPEMD160", "BC");
            byte[] ripemdHashed = ripemd160.digest(shaHashed);

            // Thêm version byte (0x00)
            byte[] versioned = new byte[ripemdHashed.length + 1];
            versioned[0] = 0x00;
            System.arraycopy(ripemdHashed, 0, versioned, 1, ripemdHashed.length);

            // Checksum
            byte[] checksum = sha256.digest(sha256.digest(versioned));
            byte[] addressBytes = new byte[versioned.length + 4];
            System.arraycopy(versioned, 0, addressBytes, 0, versioned.length);
            System.arraycopy(checksum, 0, addressBytes, versioned.length, 4);

            return Base58.encode(addressBytes);
        } catch (Exception e) {
            System.out.println("ERROR getting public address");
            throw new RuntimeException(e);
        }
    }

    public static PublicKey getPublicKey(String publicKey) {
        if (publicKey == null) {
            System.out.println("ERROR public key is null");
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(publicKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Hoặc "RSA" nếu baise dùng RSA
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            System.out.println("ERROR getting public key");
            return null;
        }
    }

    public static PrivateKey getPrivateKey(String privateKey) {
        if (privateKey == null) {
            System.out.println("ERROR private key is null");
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(privateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC"); // Hoặc "RSA" nếu baise dùng RSA
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            System.out.println("ERROR getting private key");
            return null;
        }
    }
}
