package Model;

import Service.TransactionService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bitcoinj.core.Base58;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPrivateKeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;

public class Wallet {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String address;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    public Wallet() throws Exception {
        generateKeyPair();
    }

    public Wallet(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    private void generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
        keyGen.initialize(ecSpec, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() { return privateKey; }
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public byte[] sign(byte[] data) throws Exception {
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
        ecdsaSign.initSign(privateKey);
        ecdsaSign.update(data);
        return ecdsaSign.sign();
    }

    public String getAddress() {
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

    public boolean validateWallet() {
        try {
            // Kiểm tra nếu cả 2 key không null
            if (privateKey == null || publicKey == null) {
                return false;
            }

            // Tạo dữ liệu test
            byte[] testData = "test".getBytes();

            // Ký dữ liệu bằng privateKey
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaSign.initSign(privateKey);
            ecdsaSign.update(testData);
            byte[] signature = ecdsaSign.sign();

            // Xác thực bằng publicKey
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(testData);
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
