<filename>mu-server-master/src/test/java/io/muserver/HttpsConfigBuilderTest.java<fim_prefix>

        package io.muserver;

import okhttp3.Response;
import org.junit.Test;
import scaffolding.MuAssert;
import scaffolding.ServerUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static scaffolding.ClientUtils.*;

public class HttpsConfigBuilderTest {

    @Test
    public void canCreateAnUnsignedOne() {
        HttpsConfigBuilder.unsignedLocalhost();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIfTheClasspathIsInvalid() {
        HttpsConfigBuilder.httpsConfig()
                .withKeystoreType("JKS")
                .withKeystorePassword("Very5ecure")
                .withKeyPassword("ActuallyNotSecure")
                .withKeystoreFromClasspath("/io/muserver/resources/wrong.jks");
    }

    @Test
    public void canCreateFromTheFileSystem() throws IOException {
        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.httpsConfig()
                .withKeystoreType("PKCS12")
                .withKeystorePassword("Very5ecure")
                .withKeyPassword("Very5ecure")
                .withKeystore(new File("src/main/resources/io/muserver/resources/localhost.p12"));
        test(sslContextBuilder);
    }

    @Test
    public void canCreateFromTheClasspath() throws IOException {
        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.httpsConfig()
                .withKeystoreType("PKCS12")
                .withKeystorePassword("Very5ecure")
                .withKeyPassword("Very5ecure")
                .withKeystoreFromClasspath("/io/muserver/resources/localhost.p12");
        test(sslContextBuilder);
    }

    @Test
    public void canCreateFromInputStream() throws IOException {
        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.httpsConfig()
                .withKeystoreType("PKCS12")
                .withKeystorePassword("Very5ecure")
                .withKeyPassword("Very5ecure")
                .withKeystore(new FileInputStream("src/main/resources/io/muserver/resources/localhost.p12"));
        test(sslContextBuilder);
    }

    @Test
    public void canCreateFromKeystoreManagerFactory() throws Exception {
        KeyManagerFactory kmf;
        try (InputStream keystoreStream = getClass().getResourceAsStream("/io/muserver/resources/localhost.p12")) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(keystoreStream, "Very5ecure".toCharArray());
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "Very5ecure".toCharArray());
        }
        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.httpsConfig()
                .withKeyManagerFactory(kmf);
        test(sslContextBuilder);
    }

    @Test
    public void canUsePreConstructedKeystore() throws Exception {
        KeyStore ks;
        char[] keystorePassword = "Very5ecure".toCharArray();
        try (InputStream keystoreStream = getClass().getResourceAsStream("/io/muserver/resources/localhost.p12")) {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(keystoreStream, keystorePassword);
        }
        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.httpsConfig()
                .withKeystore(ks, keystorePassword);
        test(sslContextBuilder);
    }

    @Test
    public void protocolsCanBeSpecified() throws Exception {
        AtomicReference<SSLInfo> initialSSLInfo = new AtomicReference<>();
        AtomicReference<SSLInfo> eventualSSLInfo = new AtomicReference<>();

        HttpsConfigBuilder sslContextBuilder = HttpsConfigBuilder.unsignedLocalhost();
        MuServer server = ServerUtils.httpsServerForTest()
                .withHttpsConfig(sslContextBuilder)
                .addHandler(Method.GET, "/", (req, resp, pp) -> {
                    initialSSLInfo.set(req.server().sslInfo());
                    resp.write("Hello");
                })
                .start();
        String cipher;
        try (Response resp = call(request(server.uri()))) {
            assertThat(resp.code(), is(200));
            assertThat(resp.body().string(), is("Hello"));
            cipher = getCipher(server.uri());
        } finally {
            MuAssert.stopAndCheck(server);
        }

        sslContextBuilder = HttpsConfigBuilder.unsignedLocalhost()
                .withCipherFilter((supportedCiphers, defaultCiphers) -> {
                    List<String> selected = new ArrayList<>(defaultCiphers);
                    selected.remove(cipher);
                    return selected;
                });
        server = ServerUtils.httpsServerForTest()
                .withHttpsConfig(sslContextBuilder)
                .addHandler(Method.GET, "/", (req, resp, pp) -> {
                    eventualSSLInfo.set(req.server().sslInfo());
                    resp.write("Hello");
                })
                .start();
        try (Response resp = call(request(server.uri()))) {
            assertThat(resp.code(), is(200));
            assertThat(resp.body().string(), is("Hello"));
            assertThat(getCipher(server.uri()), not(equalTo(cipher)));
        } finally {
            MuAssert.stopAndCheck(server);
        }

        assertThat(initialSSLInfo.get().ciphers(), hasItem(cipher));
        assertThat(eventualSSLInfo.get().ciphers(), not(hasItem(cipher)));

    }

    private String getCipher(URI uri) throws Exception {
        <fim_suffix>
        SSLContext sslContext = sslContextForTesting(veryTrustingTrustManager());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
        HttpsURLConnection conn = (HttpsURLConnection) uri.toURL().openConnection();
        conn.connect();
        return conn.getCipherSuite();
    }

    private static void test(HttpsConfigBuilder httpsConfigBuilder) throws IOException {
        MuServer server = ServerUtils.httpsServerForTest()
                .withHttpsConfig(httpsConfigBuilder)
                .addHandler(Method.GET, "/", (req, resp, pp) -> resp.write("Hello"))
                .start();
        try (Response resp = call(request(server.uri()))) {
            assertThat(resp.code(), is(200));
            assertThat(resp.body().string(), is("Hello"));
        } finally {
            MuAssert.stopAndCheck(server);
        }
    }

    @Test(expected = MuException.class)
    public void throwsIfThePasswordIsWrong() {
        ServerUtils.httpsServerForTest()
                .withHttpsConfig(HttpsConfigBuilder.httpsConfig()
                        .withKeystoreType("PKCS12")
                        .withKeystorePassword("Very5ecured")
                        .withKeyPassword("Very5ecured")
                        .withKeystore(new File("src/main/resources/io/muserver/resources/localhost.p12")))
                .start();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIfTheFileDoesNotExist() {
        HttpsConfigBuilder.httpsConfig()
                .withKeystoreType("JKS")
                .withKeystorePassword("Very5ecure")
                .withKeyPassword("Very5ecure")
                .withKeystore(new File("src/test/blah"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsIfClasspathIsIncorrect() {
        HttpsConfigBuilder.httpsConfig()
                .withKeystoreFromClasspath("/some/path/that/is/no/there.jks");
    }
}
<fim_middle>