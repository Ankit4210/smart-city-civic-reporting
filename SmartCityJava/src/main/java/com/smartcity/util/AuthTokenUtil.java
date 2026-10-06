package com.smartcity.util;

import com.smartcity.model.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class AuthTokenUtil {
    private static final long TOKEN_LIFETIME_SECONDS = 8 * 60 * 60;
    private static final byte[] PROCESS_SECRET = loadSecret();

    private AuthTokenUtil() {}

    public static String issue(User user) {
        long expiresAt = System.currentTimeMillis() / 1000 + TOKEN_LIFETIME_SECONDS;
        String payload = user.getId() + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public static int userIdFrom(String token) {
        if (token == null) {
            return -1;
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || !MessageDigest.isEqual(
                sign(parts[0]).getBytes(StandardCharsets.US_ASCII),
                parts[1].getBytes(StandardCharsets.US_ASCII))) {
            return -1;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split(":", -1);
            int userId = Integer.parseInt(claims[0]);
            long expiresAt = Long.parseLong(claims[1]);
            return userId > 0 && expiresAt > System.currentTimeMillis() / 1000 ? userId : -1;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return -1;
        }
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(PROCESS_SECRET, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Unable to sign authentication token", e);
        }
    }

    private static byte[] loadSecret() {
        String configuredSecret = System.getenv("SMARTCITY_AUTH_SECRET");
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            if (configuredSecret.length() < 32) {
                throw new IllegalStateException("SMARTCITY_AUTH_SECRET must contain at least 32 characters.");
            }
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        return createSecret();
    }

    private static byte[] createSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return secret;
    }
}
