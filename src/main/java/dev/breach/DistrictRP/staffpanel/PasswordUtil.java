package dev.breach.DistrictRP.staffpanel;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plain) {
        return BCrypt.withDefaults().hashToString(12, plain.toCharArray());
    }

    public static boolean verify(String plain, String hash) {
        if (plain == null || hash == null) return false;
        try {
            return BCrypt.verifyer().verify(plain.toCharArray(), hash).verified;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isValidEmail(String s) {
        if (s == null || s.length() < 4 || s.length() > 190) return false;
        return s.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}