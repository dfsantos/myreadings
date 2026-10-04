package dev.dfsantos.myreadings.book;

/**
 * Lançada quando {@code endDate} e {@code startDate} estão ambos presentes no
 * estado final de uma entidade (após aplicar o request de criação ou de
 * atualização parcial) e {@code endDate} é anterior a {@code startDate}.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
