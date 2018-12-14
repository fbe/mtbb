package name.felixbecker.mtbb;

import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class BouncyCastle {

    // FIXME
    // Based on:
    // https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java
    // Refactor / remove deprecation

    public void createKeyManager() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
        KeyStore ks = KeyStore.getInstance("BKS");
    }

    public BouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {

        final BouncyCastle bc = new BouncyCastle();
        final KeyPair keyPair = bc.generateKeyPair();

        X509Certificate selfSignedX509Certificate = bc.generateSelfSignedX509Certificate(keyPair);

        StringWriter stringWriter = new StringWriter(1000);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(selfSignedX509Certificate);
        jcaPEMWriter.flush();

        System.out.println(selfSignedX509Certificate);
        System.out.println("========================================================================");
        System.out.println(stringWriter.toString());
        System.out.println(keyPair.getPrivate());
        System.out.println(keyPair.getPublic());
    }

    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(4096, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public X509Certificate generateSelfSignedX509Certificate(KeyPair keyPair) throws CertificateEncodingException, InvalidKeyException, IllegalStateException,
            NoSuchProviderException, NoSuchAlgorithmException, SignatureException {

        // build a certificate generator
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("cn=example");

        // add some options
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(new X509Name("dc=name"));
        certGen.setIssuerDN(dnName); // use the same
        // yesterday
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        // in 2 years
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        // finally, sign the certificate with the private key of the same KeyPair
        return certGen.generate(keyPair.getPrivate(), "BC");
    }

}
