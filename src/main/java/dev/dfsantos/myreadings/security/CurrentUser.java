package dev.dfsantos.myreadings.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Helper para obter o id do usuário autenticado a partir do {@code SecurityContext},
 * populado pelo {@code JwtAuthenticationFilter} com o {@code sub} (UUID) do token.
 *
 * Services devem chamar {@link #id()} para obter o {@code userId} usado em qualquer
 * consulta/gravação escopada por usuário — nunca aceitar esse valor como parâmetro
 * vindo do cliente (request body/query), conforme a convenção de isolamento por
 * usuário.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança");
        }
        return userId;
    }
}
