//package Service;
//
//import Model.Transaction;
//import Model.UTXO;
//import Model.TxIn;
//import Model.TxOut;
//import org.iq80.leveldb.*;
//
//import java.io.*;
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
//import static org.fusesource.leveldbjni.JniDBFactory.factory;
//import static org.iq80.leveldb.impl.Iq80DBFactory.*;
//
//
//public class UTXOSetService implements Closeable {
//    private final DB db;
//
//    public UTXOSetService(File path) throws IOException {
//        Options options = new Options();
//        options.createIfMissing(true);
//        db = factory.open(path, options);
//    }
//
//    /** Tạo key: txId(string) + index(int) */
//    private byte[] makeKey(String txId, int index, String pubAdd) {
//        byte[] txBytes = txId.getBytes(StandardCharsets.UTF_8);
//        ByteBuffer bb = ByteBuffer.allocate(txBytes.length + 4);
//        bb.put(txBytes);
//        bb.putInt(index);
//        bb.put(pubAdd.getBytes());
//        return bb.array();
//    }
//
//    /** Serialize UTXO thành byte[] */
//    private byte[] serializeUTXO(UTXO utxo) throws IOException {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        DataOutputStream dos = new DataOutputStream(baos);
//        dos.writeUTF(utxo.getTxId());
//        dos.writeInt(utxo.getIndex());
//        dos.writeLong(utxo.getValue());
//        dos.writeUTF(utxo.getPubAdd());
//        dos.flush();
//        return baos.toByteArray();
//    }
//
//    /** Deserialize byte[] thành UTXO */
//    private UTXO deserializeUTXO(byte[] data) throws IOException {
//        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
//        String txId = dis.readUTF();
//        int index = dis.readInt();
//        long value = dis.readLong();
//        String pubAdd = dis.readUTF();
//        return new UTXO(txId, index, value, pubAdd);
//    }
//
//    /** Thêm UTXO */
//    public void putUTXO(UTXO utxo) throws IOException {
//        db.put(makeKey(utxo.getTxId(), utxo.getIndex(), utxo.getPubAdd()), serializeUTXO(utxo));
//    }
//
//    /** Lấy UTXO */
//    public UTXO getUTXO(String txId, int index, String pubAdd) throws IOException {
//        byte[] data = db.get(makeKey(txId, index,  pubAdd));
//        if (data == null) return null;
//        return deserializeUTXO(data);
//    }
//
//    /** Xóa UTXO */
//    public void deleteUTXO(String txId, int index,  String pubAdd) throws IOException {
//        db.delete(makeKey(txId, index, pubAdd));
//    }
//
//    /** Cập nhật batch khi xử lý 1 transaction */
//    public void applyTransaction(List<TxIn> inputs, List<TxOut> outputs, String txId, String pubAdd) throws IOException {
//        try (WriteBatch batch = db.createWriteBatch()) {
//            // Xóa những UTXO bị tiêu
//            for (TxIn in : inputs) {
//                byte[] key = makeKey(in.getPrevTxId(), in.getPrevIndex(), pubAdd);
//                batch.delete(key);
//            }
//            // Thêm UTXO mới
//            for (int i = 0; i < outputs.size(); i++) {
//                TxOut out = outputs.get(i);
//                UTXO utxo = new UTXO(txId, i, out.getValue(), out.getPublicAdd());
//                batch.put(makeKey(txId, i, pubAdd), serializeUTXO(utxo));
//            }
//            db.write(batch);
//        }
//    }
//
//    /** Đóng DB */
//    @Override
//    public void close() throws IOException {
//        db.close();
//    }
//
//
//    public void updateUTXO(Transaction tx) throws IOException {
//        try (WriteBatch batch = db.createWriteBatch()) {
//            String txId = tx.getTxId();
//
//            // 1. Xóa các UTXO hiện tại trong db trùng publicAddress + txId + index
//            for (TxOut out : tx.getOutputs()) {
//                String publicAddress = out.getPublicAdd();
//
//                // Duyệt key: publicAddress + txId + index
//                // Vì txOut không có index trực tiếp, bạn cần duyệt với index từ 0
//                for (int index = 0; index < tx.getOutputs().size(); index++) {
//                    String keyStr = publicAddress + "|" + txId + "|" + index;
//                    byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
//                    batch.delete(key);
//                }
//            }
//
//            // 2. Thêm UTXO mới với key = publicAddress + txId + index
//            for (int index = 0; index < tx.getOutputs().size(); index++) {
//                TxOut out = tx.getOutputs().get(index);
//                String publicAddress = out.getPublicAdd();
//                String keyStr = publicAddress + "|" + txId + "|" + index;
//                byte[] key = keyStr.getBytes(StandardCharsets.UTF_8);
//
//                // Tạo UTXO object
//                UTXO utxo = new UTXO(txId, index, out.getValue(), publicAddress);
//
//                // Serialize UTXO (bạn cần tự viết serializeUTXO)
//                byte[] val = serializeUTXO(utxo);
//                batch.put(key, val);
//            }
//
//            db.write(batch);
//        }
//    }
//}
