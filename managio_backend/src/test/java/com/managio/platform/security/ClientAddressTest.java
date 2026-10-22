package com.managio.platform.security;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which client address rate limits and sign-in records use (ClientAddressFilter). Clients can send any
 * {@code X-Forwarded-For} they like, so only the web app's pass-through, proven by the shared secret, may name
 * the address.
 */
class ClientAddressTest extends IntegrationTest {

    private static final String SECRET = "test-proxy-secret-shared-with-the-web-app";
    private static final String SIGN_IN = "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}";

    private MvcTestResult signIn(RequestPostProcessor connection, String... headers) {
        var request = mvc.post().uri("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(SIGN_IN)
                .with(connection);
        for (int i = 0; i < headers.length; i += 2) {
            request = request.header(headers[i], headers[i + 1]);
        }
        return request.exchange();
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    /** Ten attempts a minute are allowed per address; the eleventh is refused. */
    private void useUpSignInLimit(RequestPostProcessor connection, String... headers) {
        for (int i = 0; i < 10; i++) {
            assertThat(signIn(connection, headers)).hasStatus(401);
        }
    }

    @Test
    void forgedForwardedForHeadersShareTheConnectionsLimit() {
        for (int i = 0; i < 10; i++) {
            assertThat(signIn(from("203.0.113.10"), "X-Forwarded-For", "198.51.100." + i)).hasStatus(401);
        }
        problem(signIn(from("203.0.113.10"), "X-Forwarded-For", "198.51.100.99"), 429, "rate_limited");
        problem(signIn(from("203.0.113.10"), "X-Real-IP", "198.51.100.98"), 429, "rate_limited");
        // A different connection has its own limit.
        assertThat(signIn(from("203.0.113.11"))).hasStatus(401);
    }

    @Test
    void theWebAppNamesTheClientWhenItProvesItselfWithTheSecret() {
        String[] client = {"X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "198.51.100.7"};
        useUpSignInLimit(from("10.0.0.5"), client);
        problem(signIn(from("10.0.0.5"), client), 429, "rate_limited");

        // Another visitor arriving through the same web app is not affected.
        assertThat(signIn(from("10.0.0.5"), "X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "198.51.100.8"))
                .hasStatus(401);
        // Requests through the web app don't use up the web app's own connection address either.
        assertThat(signIn(from("10.0.0.5"))).hasStatus(401);
    }

    @Test
    void aWrongOrMissingSecretMeansTheClaimedAddressIsIgnored() {
        for (int i = 0; i < 10; i++) {
            assertThat(signIn(from("203.0.113.20"), "X-Managio-Proxy-Secret", "guessed-secret-guessed-secret-guessed",
                    "X-Managio-Client-Ip", "198.51.100." + i)).hasStatus(401);
        }
        problem(signIn(from("203.0.113.20"), "X-Managio-Client-Ip", "198.51.100.50"), 429, "rate_limited");
        problem(signIn(from("203.0.113.20"), "X-Managio-Proxy-Secret", SECRET.toUpperCase(),
                "X-Managio-Client-Ip", "198.51.100.51"), 429, "rate_limited");
    }

    @Test
    void aClaimedAddressThatIsNotAnIpFallsBackToTheConnection() {
        useUpSignInLimit(from("203.0.113.30"), "X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "evil.example");
        problem(signIn(from("203.0.113.30"), "X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "1.2.3.4, 5.6.7.8"),
                429, "rate_limited");
    }

    @Test
    void ipv6AddressesInOneNetworkShareALimit() {
        for (int i = 1; i <= 10; i++) {
            assertThat(signIn(from("10.0.0.5"), "X-Managio-Proxy-Secret", SECRET,
                    "X-Managio-Client-Ip", "2001:db8:1:2::" + Integer.toHexString(i))).hasStatus(401);
        }
        problem(signIn(from("10.0.0.5"), "X-Managio-Proxy-Secret", SECRET,
                "X-Managio-Client-Ip", "2001:db8:1:2:ffff:ffff:ffff:ffff"), 429, "rate_limited");
        assertThat(signIn(from("10.0.0.5"), "X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "2001:db8:1:3::1"))
                .hasStatus(401);
    }

    @Test
    void signInRecordsKeepTheResolvedAddress() {
        signIn(from("10.0.0.5"), "X-Managio-Proxy-Secret", SECRET, "X-Managio-Client-Ip", "198.51.100.77");
        signIn(from("203.0.113.40"), "X-Forwarded-For", "198.51.100.78");

        assertThat(jdbc.sql("SELECT ip_address FROM sign_in_events ORDER BY id").query(String.class).list())
                .containsExactly("198.51.100.77", "203.0.113.40");
    }

    @Test
    void addressesAreParsedStrictly() {
        assertThat(ClientAddresses.parse(" 198.51.100.7 ")).contains("198.51.100.7");
        assertThat(ClientAddresses.parse("::ffff:198.51.100.7")).contains("198.51.100.7");
        assertThat(ClientAddresses.parse("2001:DB8::1")).contains("2001:db8:0:0:0:0:0:1");
        assertThat(ClientAddresses.parse("256.1.1.1")).isEmpty();
        assertThat(ClientAddresses.parse("fe80::1%eth0")).isEmpty();
        assertThat(ClientAddresses.parse("example.com")).isEmpty();
        assertThat(ClientAddresses.parse("")).isEmpty();
        assertThat(ClientAddresses.parse(null)).isEmpty();
        assertThat(ClientAddresses.rateLimitKey("2001:db8:1:2:3:4:5:6")).isEqualTo("2001:db8:1:2:0:0:0:0/64");
        assertThat(ClientAddresses.rateLimitKey("198.51.100.7")).isEqualTo("198.51.100.7");
    }
}
