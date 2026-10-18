package tech.smartboot.feat.gateway.config;

/**
 * 下游 key 的哈希工具。
 * <p>
 * 配置文件里只存 sha256 的前 16 位十六进制（也接受完整 64 位），泄露配置不等于泄露 key。
 */
public final class KeyHash {
    private KeyHash() {
    }

    public static String sha256Hex(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 兼容 "sha256:xxxx" 与裸哈希两种写法，统一成小写十六进制。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim().toLowerCase();
        if (v.startsWith("sha256:")) {
            v = v.substring("sha256:".length());
        }
        return v;
    }

    public static boolean matches(String token, String storedHash) {
        if (token == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        String actual = sha256Hex(token);
        return actual.startsWith(storedHash) && storedHash.length() >= 8;
    }

    public static String mask(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
