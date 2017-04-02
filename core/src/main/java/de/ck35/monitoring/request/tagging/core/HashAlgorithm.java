package de.ck35.monitoring.request.tagging.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementation of an String based hash algorithm which creates hex string hashes.
 * The underlying message digest algorithm can be configured. The default is MD5. 
 * 
 * @author Christian Kaspari
 * @since 1.1.0
 */
public class HashAlgorithm {

    private static final String DEFAULT_ALGORITHM_NAME = "MD5";
    private static final char[] ALPHABET = "0123456789ABCDEF".toCharArray(); 
    
    private volatile Supplier<MessageDigest> messageDigestSupplier;
    
    public HashAlgorithm() {
        setAlgorithmName(DEFAULT_ALGORITHM_NAME);
    }

    public String hash(String value) {
        StringBuilder result = new StringBuilder();
        for (byte b : messageDigestSupplier.get().digest(value.getBytes())) {
            int highBits = (b & 0xF0) >> 4;
            int lowBits = b & 0xF;
            result.append(ALPHABET[highBits]);
            result.append(ALPHABET[lowBits]);
        }
        return result.toString();
    }
    
    public void setAlgorithmName(String algorithmName) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Hash algorithm: '" + algorithmName + "' is not supported!", e);
        }
        try {
            Objects.requireNonNull((MessageDigest) messageDigest.clone());
            messageDigestSupplier = cloningSupplier(messageDigest);
        } catch (CloneNotSupportedException e) {
            messageDigestSupplier = instantiatingSupplier(algorithmName);
        }
    }
    
    public String getAlgorithmName() {
        return messageDigestSupplier.get().getAlgorithm();
    }
    
    static Supplier<MessageDigest> cloningSupplier(MessageDigest messageDigest) {
        return () -> {
            try {
                return (MessageDigest) messageDigest.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Used cloning supplier but message digest: '" + messageDigest + "' does not support cloning.", e);
            }
        };
    }
    
    static Supplier<MessageDigest> instantiatingSupplier(String algorithmName) {
        return () -> {
            try {
                return MessageDigest.getInstance(algorithmName);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Used instantiating supplier but algorithm: '" + algorithmName + "' is unknown!", e);
            }
        };
    }
}