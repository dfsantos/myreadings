package dev.dfsantos.myreadings.user;

/**
 * Lançada quando já existe um usuário cadastrado com o e-mail informado no registro.
 */
public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("Já existe um usuário cadastrado com o e-mail " + email);
    }
}
