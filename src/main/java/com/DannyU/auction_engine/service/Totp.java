package com.DannyU.auction_engine.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

// Time-based one-time passwords (RFC 6238, the codes of Google Authenticator & co.): HMAC-SHA1 over the number of
// 30-second steps since 1970, 6 digits. Secrets are 20 random bytes, shared with the app as Base32 text.
public final class Totp {

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {
    }

    public static String newSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32(bytes);
    }

    // Accepts the code of the current 30 s step and of the one before/after (clock drift on phones)
    public static boolean verify(String secret, String code, long epochSeconds) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long step = epochSeconds / 30;
        for (long offset = -1; offset <= 1; offset++) {
            if (code(secret, step + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String code(String secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // otpauth:// link that authenticator apps read from the QR code
    public static String uri(String issuer, String account, String secret) {
        return "otpauth://totp/" + encode(issuer) + ":" + encode(account) + "?secret=" + secret + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    static String base32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                out.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        }
        return out.toString();
    }

    static byte[] decodeBase32(String text) {
        String clean = text.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer out = ByteBuffer.allocate(clean.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : clean.toCharArray()) {
            int value = BASE32.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Cod secret invalid");
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                out.put((byte) ((buffer >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        return out.array();
    }

    private static String encode(String text) {
        return java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
