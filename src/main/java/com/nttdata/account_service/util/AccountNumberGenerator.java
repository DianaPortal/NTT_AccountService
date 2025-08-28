package com.nttdata.account_service.util;

import java.security.SecureRandom;

public final class AccountNumberGenerator {
    private static final SecureRandom RND = new SecureRandom();

    private AccountNumberGenerator() {
    }

    public static String numeric(int length) {
        return RND.ints(length, 0, 10)
                .mapToObj(String::valueOf)
                .reduce("", String::concat);
    }
}
