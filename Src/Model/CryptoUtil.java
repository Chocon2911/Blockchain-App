package Model;


import java.security.PublicKey;
import java.security.Signature;

public class CryptoUtil {

    public static boolean verifySignature(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data);
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

