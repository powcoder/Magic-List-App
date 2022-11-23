https://powcoder.com
代写代考加微信 powcoder
Assignment Project Exam Help
Add WeChat powcoder
package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import play.Logger;
import play.libs.Json;
import utilities.ListUtility;
import utilities.StringUtility;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

abstract class GoogleCloudStorageClient {

    private static final String PROJECT_ID = "magic-list-165706";
    private static final List<String> STORAGE_SCOPES =
            ListUtility.asList("https://www.googleapis.com/auth/devstorage.read_write");

    private final Storage storage;
    private final String bucketName;
    private final Logger.ALogger logger = Logger.of(getClass());

    /* package-private */ GoogleCloudStorageClient(String pathToCredentialFile, String bucketName) {
        this.bucketName = bucketName;

        Credentials credentials = getCredentialsFromFile(pathToCredentialFile);
        this.storage = StorageOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(PROJECT_ID)
                .build()
                .getService();
    }

    public Optional<Blob> downloadFileFromBucket(String fileId) throws StorageException {
        return Optional.ofNullable(storage.get(bucketName).get(fileId));
    }

    public Blob uploadFileToBucket(String fileId, byte[] bytes, String contentType) throws StorageException {
        return storage.get(bucketName).create(fileId, bytes, contentType);
    }

    public Optional<Boolean> deleteFileFromBucket(String fileId) {
        try {
            boolean value = storage.get(bucketName)
                    .get(fileId)
                    .delete();
            return Optional.of(value);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    // MARK - PRIVATE METHODS

    private Credentials getCredentialsFromFile(String pathToCredentialFile) {
        JsonNode node;
        try {
            node = Json.parse(new FileInputStream(pathToCredentialFile));
        } catch (FileNotFoundException e) {
            String[] currentDirectory = new File("./").list();
            logger.error("Working Directory: {}", StringUtility.makeString(", ", currentDirectory));
            throw new RuntimeException(e);
        }

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String clientId = node.get("client_id").asText();
        String clientEmail = node.get("client_email").asText();
        String rawPrivateKey = node.get("private_key").asText();
        String privateKeyId = node.get("private_key_id").asText();

        byte[] rawPrivateKeyBytes = DatatypeConverter.parseBase64Binary(new String(rawPrivateKey.getBytes(), Charset.forName("UTF-8")));
        KeySpec privateKeySpec = new PKCS8EncodedKeySpec(rawPrivateKeyBytes);

        PrivateKey privateKey;
        try {
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        ServiceAccountCredentials credentials = new ServiceAccountCredentials(clientId, clientEmail, privateKey, privateKeyId, STORAGE_SCOPES);

        logger.debug("Credentials Account: {}", credentials.getAccount());
        logger.debug("Credentials Scopes: {}", credentials.getScopes());

        return credentials;
    }

}
