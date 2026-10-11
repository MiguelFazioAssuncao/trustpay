package com.trustpay.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {
    @Test
    void acceptsOnlyConfiguredOriginsAndRejectsWildcardCredentials() {
        var source = new CorsConfig(" https://app.example.com,https://app.example.com,https://admin.example.com ")
                .corsConfigurationSource();
        var configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/transactions"));
        assertNotNull(configuration);
        assertEquals(List.of("https://app.example.com", "https://admin.example.com"), configuration.getAllowedOrigins());
        assertEquals("https://app.example.com", configuration.checkOrigin("https://app.example.com"));
        assertNull(configuration.checkOrigin("https://untrusted.example.com"));
        assertThrows(IllegalStateException.class, () -> new CorsConfig("*"));
    }

    @Test
    void missingConfigurationDisablesCrossOriginAccess() {
        var configuration = new CorsConfig("").corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/transactions"));
        assertNotNull(configuration);
        assertNull(configuration.checkOrigin("https://app.example.com"));
    }
}
