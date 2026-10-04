package dev.dfsantos.myreadings.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ponto único de resposta para requisições não autenticadas a rotas protegidas —
 * cobre tanto a ausência de header {@code Authorization} quanto um token
 * inválido/expirado (o {@link JwtAuthenticationFilter} deixa o contexto de
 * segurança vazio nesse caso, e é este entry point que o Spring Security invoca a
 * seguir). Escreve o corpo diretamente como Problem+JSON (RFC 7807) para manter o
 * mesmo formato de erro usado pelo {@code GlobalExceptionHandler} nas exceções
 * lançadas por controllers — como este ponto roda antes do
 * {@code DispatcherServlet}, o {@code @RestControllerAdvice} não teria chance de
 * atuar aqui.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":"unauthorized","status":401,"detail":"Token de autenticação ausente, inválido ou expirado"}""");
    }
}
