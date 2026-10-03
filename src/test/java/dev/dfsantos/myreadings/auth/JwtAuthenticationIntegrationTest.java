package dev.dfsantos.myreadings.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o isolamento automático por usuário (US-03) no nível de filtro/filter chain:
 * uma rota protegida qualquer sem handler mapeado serve para validar o comportamento
 * do JwtAuthenticationFilter + SecurityFilterChain — a verificação de autorização do
 * Spring Security ocorre antes do dispatch para o controller, então:
 * - sem token ou com token inválido/expirado: a cadeia rejeita a requisição com 401
 *   antes mesmo de saber que a rota não tem handler;
 * - com token válido: a requisição é autenticada e chega ao DispatcherServlet, que
 *   retorna 404 por falta de handler — o 404 (em vez de 401) é a evidência de que a
 *   autenticação ocorreu.
 *
 * Usa uma rota fictícia sem nenhum {@code @RequestMapping} correspondente (em vez de,
 * por exemplo, {@code GET /api/v1/books/{id}}) propositalmente: o catálogo de livros
 * ganha novos métodos HTTP em {@code /api/v1/books/{id}} ao longo das histórias de CRUD
 * (US-04+: {@code POST}; US-07+: {@code PATCH}; etc.) e, assim que qualquer método passa
 * a ter handler num caminho, um método diferente sem handler nesse mesmo caminho responde
 * 405 (método não suportado), não 404 — o que invalidaria esta verificação. Uma rota que
 * nenhum controller jamais mapeia mantém o teste estável independente da evolução do CRUD.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest {

    private static final String PROTECTED_ROUTE = "/api/v1/rota-sem-handler-mapeado";

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("jwt-authentication-integration-test.db"));
    }

    @Test
    void protectedRouteWithoutAuthorizationHeaderReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_ROUTE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithMalformedTokenReturns401() throws Exception {
        mockMvc.perform(get(PROTECTED_ROUTE)
                        .header("Authorization", "Bearer token-completamente-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithExpiredTokenReturns401() throws Exception {
        String expiredToken = generateTokenExpiringAt(Instant.now().minusSeconds(60));

        mockMvc.perform(get(PROTECTED_ROUTE)
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRouteWithValidTokenIsAuthenticatedAndReachesDispatch() throws Exception {
        String validToken = generateTokenExpiringAt(Instant.now().plusSeconds(3600));

        // 404: a rota não tem handler mapeado em nenhum controller, mas isso só é
        // possível de constatar porque a autenticação foi aceita — sem token válido,
        // a resposta seria 401, como nos testes acima.
        mockMvc.perform(get(PROTECTED_ROUTE)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    private String generateTokenExpiringAt(Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
