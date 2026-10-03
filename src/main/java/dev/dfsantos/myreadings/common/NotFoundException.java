package dev.dfsantos.myreadings.common;

/**
 * Lançada quando um recurso não existe ou não pertence ao usuário autenticado.
 * Em ambos os casos o {@code GlobalExceptionHandler} responde {@code 404} — nunca
 * {@code 403} — para não revelar a existência do recurso a quem não é o dono.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
