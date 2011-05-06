package util.crypto;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import external.Base64Coder.Base64Coder;

public class BaseCryptography {    
    public static void main(String args[]) {
        EncodedKeyPair theKeys = generateKeys();
        String theSK = theKeys.thePrivateKey;
        String thePK = theKeys.thePublicKey;
        
        String theData = "Hello world!";
        String theSignature = signData(theSK, theData);        
        try {
            System.out.println("SK = " + theSK);
            System.out.println("PK = " + thePK);
            System.out.println("Data = " + theData);
            System.out.println("Signature = " + theSignature);
            System.out.println("Signature valid? " + verifySignature(thePK, theSignature, theData));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EncodedKeyPair generateKeys() {
        try {
            // Generate a 2048-bit RSA key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keypair = keyGen.genKeyPair();
            PrivateKey privateKey = keypair.getPrivate();
            PublicKey publicKey = keypair.getPublic();            
            return new EncodedKeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    public static String signData(String thePrivateKey, String theData) {
        PrivateKey theSK = decodePrivateKey(thePrivateKey);
        if (theSK == null) return null;
        
        try {
            Signature sig = Signature.getInstance("SHA1WithRSA");
            sig.initSign(theSK);
            sig.update(theData.getBytes("UTF-8"));
            return encodeSignature(sig.sign());
        } catch (SignatureException e) {
        } catch (UnsupportedEncodingException e) {
        } catch (InvalidKeyException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }
    
    public static boolean verifySignature(String thePublicKey, String theSignature, String theData) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        PublicKey thePK = decodePublicKey(thePublicKey);
        if (thePK == null) throw new SignatureException("Could not reconstruct public key.");

        byte[] theSigBytes = decodeSignature(theSignature);
        if (theSigBytes == null) throw new SignatureException("Could not reconstruct signature.");

        Signature sig = Signature.getInstance("SHA1WithRSA");
        sig.initVerify(thePK);
        sig.update(theData.getBytes("UTF-8"));
        return sig.verify(theSigBytes);
    }

    /* Class to hold a pair of string-encoded keys */
    public static class EncodedKeyPair {
        public final String thePublicKey;
        public final String thePrivateKey;
        
        public EncodedKeyPair(PublicKey thePK, PrivateKey theSK) {
            thePublicKey = encodeKey(thePK);
            thePrivateKey = encodeKey(theSK);
        }
    }    
    
    /* Functions for encoding and decoding public and private keys */
    private static String encodeKey(PublicKey thePK) {
        return "0" + encodeBytes(thePK.getEncoded());
    }
    private static String encodeKey(PrivateKey theSK) {
        return "0" + encodeBytes(theSK.getEncoded());
    }
    private static String encodeSignature(byte[] theSignatureBytes) {
        return "0" + encodeBytes(theSignatureBytes);
    }
    private static PublicKey decodePublicKey(String thePK) {
        if (!thePK.startsWith("0")) return null;
        thePK = thePK.replaceFirst("0", "");
        
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(new X509EncodedKeySpec(decodeBytes(thePK)));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeySpecException e) {
            return null;
        }
    }
    private static PrivateKey decodePrivateKey(String theSK) {
        if (!theSK.startsWith("0")) return null;
        theSK = theSK.replaceFirst("0", "");
        
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new PKCS8EncodedKeySpec(decodeBytes(theSK)));
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (InvalidKeySpecException e) {
            return null;
        }
    }
    private static byte[] decodeSignature(String theSig) {
        if (!theSig.startsWith("0")) return null;
        theSig = theSig.replaceFirst("0", "");        
        
        return decodeBytes(theSig);
    }    
    
    /* Functions for encoding/decoding arrays of bytes */
    private static String encodeBytes(byte[] theBytes) {
        return new String(Base64Coder.encode(theBytes)).replace('/', '-').replace('=','_');
    }
    private static byte[] decodeBytes(String theString) {
        return Base64Coder.decode(theString.replace('-', '/').replace('_','='));
    }    
}