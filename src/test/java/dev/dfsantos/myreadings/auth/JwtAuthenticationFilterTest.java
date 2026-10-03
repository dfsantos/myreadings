package dev.dfsantos.myreadings.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitário (Mockito, sem contexto Spring) do filtro em isolamento — cobre a
 * regra de que um token válido deixa o {@code userId} disponível no SecurityContext
 * e que um token ausente/inválido não autentica a requisição.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "segredo-de-teste-apenas-para-o-filtro-unitario-1234567890";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET, 1440);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenPopulatesSecurityContextWithUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateToken(userId);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(userId, principal);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthorizationHeaderLeavesSecurityContextEmpty() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenLeavesSecurityContextEmptyAndStillContinuesChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido-e-malformado");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void expiredTokenLeavesSecurityContextEmpty() throws Exception {
        // expiration-minutes negativo força um token já expirado no momento da emissão.
        JwtTokenProvider providerWithPastExpiration = new JwtTokenProvider(SECRET, -1);
        String expiredToken = providerWithPastExpiration.generateToken(UUID.randomUUID());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
