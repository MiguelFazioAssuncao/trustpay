package com.trustpay.backend.security;

import java.util.Locale;

public final class EmailAddress {

    public static final String STORE_EMAIL = "store@trustpay.com";

    private EmailAddress() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
