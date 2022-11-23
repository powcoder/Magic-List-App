https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package utilities;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.PrivateKey;

/**
 * Created by Corey on 3/15/2017.
 * Project: Magic List Maker - Server
 * <p></p>
 * Purpose of Class:
 */
public class DecryptionUtility {

    /**
     * String to hold name of the encryption algorithm.
     */
    private static final String ALGORITHM = "RSA";

    /**
     * String to hold the name of the private key file.
     */
    private static final String PRIVATE_KEY_FILE = "conf/private.key";

    public static String decrypt(byte[] encryptedBytes) {
        PrivateKey privateKey = getPrivateKey();
        if (privateKey == null) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(encryptedBytes));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static PrivateKey getPrivateKey() {
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(PRIVATE_KEY_FILE));
            return (PrivateKey) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
