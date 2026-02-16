package com.strataguard.core.util;

import java.security.SecureRandom;

public final class VerificationCodeUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    private VerificationCodeUtils() {
    }

    public static String generate() {
        int code = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
