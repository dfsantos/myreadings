package dev.dfsantos.myreadings.security;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Backstop: a validação do token normalmente falha dentro do
// JwtAuthenticationFilter (antes do DispatcherServlet), onde este advice
// não atua — tratado lá via JwtAuthenticationEntryPoint. Cobre o caso de
// algum controller/service futuro, de qualquer módulo, parsear um token
// diretamente (ex: endpoint de refresh) e deixar a exceção vazar — por
// isso este advice NÃO é escopado por basePackages (é infraestrutura
// cross-cutting, não lógica de um módulo de negócio específico).
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("unauthorized");
        problemDetail.setDetail("Token de autenticação ausente, inválido ou expirado");
        return problemDetail;
    }
}
