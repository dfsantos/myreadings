package dev.dfsantos.myreadings.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Extrai o token JWT do header {@code Authorization: Bearer <token>} e, se válido,
 * autentica a requisição populando o {@code SecurityContext} com o id do usuário
 * (UUID) como principal — sem carregar o {@code User} do banco a cada requisição,
 * já que o token já contém o {@code sub} necessário.
 *
 * Se o header estiver ausente ou o token for inválido/expirado, o contexto é
 * deixado vazio (requisição permanece anônima): quem decide a resposta de erro é
 * o mecanismo padrão do Spring Security (authorizeHttpRequests +
 * AuthenticationEntryPoint configurado em SecurityConfig), e não este filtro. Isso
 * evita duplicar lógica de formatação de erro aqui e mantém "sem token" e "token
 * inválido" com o mesmo tratamento (401 consistente).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                UUID userId = jwtTokenProvider.extractUserId(token);
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Token malformado/expirado/assinatura inválida ou sub que não é um UUID
                // válido: contexto fica vazio, requisição segue anônima.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
