package org.ggp.base.util.crypto;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Formatter;

public class BaseHashing {
    // Computes the SHA1 hash of a given input string, and represents
    // that hash as a hexadecimal string.
    public static String computeSHA1Hash(String theData) {
        try {
            MessageDigest SHA1 = MessageDigest.getInstance("SHA1");
            DigestInputStream theDigestStream = new DigestInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(
                            theData.getBytes("UTF-8"))), SHA1);
            while (theDigestStream.read() != -1);
            byte[] theHash = SHA1.digest();

            Formatter hexFormat = new Formatter();
            for (byte x : theHash) {
                hexFormat.format("%02x", x);
            }
            return hexFormat.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}