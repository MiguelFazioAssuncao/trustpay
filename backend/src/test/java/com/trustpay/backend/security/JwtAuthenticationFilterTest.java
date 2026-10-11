package com.trustpay.backend.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwtService = new JwtService(JwtServiceTest.SECRET, 60_000);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsValidCaseInsensitiveBearerAndUsesCurrentAuthorities() throws Exception {
        JwtAuthenticationFilter filter = filter(User.withUsername("user@example.com")
                .password("hash").roles("ADMIN").build());
        var request = request("bearer " + token());
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsValidTokensWhenAccountHasBeenDisabled() throws Exception {
        assertDenied(User.withUsername("user@example.com").password("hash").roles("USER")
                .disabled(true).build());
    }

    @Test
    void rejectsValidTokensWhenAccountHasBeenLocked() throws Exception {
        assertDenied(User.withUsername("user@example.com").password("hash").roles("USER")
                .accountLocked(true).build());
    }

    @Test
    void rejectsMalformedAndDeletedAccountTokensWithoutContinuingChain() throws Exception {
        var response = new MockHttpServletResponse();
        filter(User.withUsername("user@example.com").password("hash").roles("USER").build())
                .doFilter(request("Bearer invalid"), response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);

        var deletedResponse = new MockHttpServletResponse();
        new JwtAuthenticationFilter(jwtService, username -> {
            throw new UsernameNotFoundException("removed");
        }, new JwtAuthenticationEntryPoint()).doFilter(request("Bearer " + token()), deletedResponse, chain);
        assertEquals(401, deletedResponse.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void publicLoginIgnoresOldBearerTokensButOtherAuthPathsAreFiltered() throws Exception {
        var filter = filter(User.withUsername("user@example.com").password("hash").roles("USER").build());
        var login = request("Bearer invalid");
        login.setMethod("POST");
        login.setServletPath("/auth/login");
        var response = new MockHttpServletResponse();
        filter.doFilter(login, response, chain);
        verify(chain).doFilter(login, response);
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/authentic")));
    }

    private void assertDenied(UserDetails user) throws Exception {
        var response = new MockHttpServletResponse();
        filter(user).doFilter(request("Bearer " + token()), response, chain);
        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentAsString().contains("\"error\": \"UNAUTHORIZED\""));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    private JwtAuthenticationFilter filter(UserDetails user) {
        return new JwtAuthenticationFilter(jwtService, username -> user, new JwtAuthenticationEntryPoint());
    }

    private String token() {
        return jwtService.generateToken("user@example.com", Map.of("role", "USER"));
    }

    private MockHttpServletRequest request(String authorization) {
        var request = new MockHttpServletRequest("GET", "/transactions");
        request.setServletPath("/transactions");
        request.addHeader("Authorization", authorization);
        return request;
    }
}
