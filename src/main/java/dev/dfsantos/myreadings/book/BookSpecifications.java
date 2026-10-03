package dev.dfsantos.myreadings.book;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Predicados reutilizáveis para montar a {@link Specification} de {@code Book} usada em
 * {@code BookService.search}. US-16 adiciona aqui os predicados correspondentes aos
 * demais filtros de {@code BookSearchCriteria}.
 */
final class BookSpecifications {

    private BookSpecifications() {
    }

    static Specification<Book> hasUserId(UUID userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId);
    }

    /**
     * Busca por substring case-insensitive em {@code title} OU {@code author}. Usa
     * {@code LOWER(...)} nas duas pontas em vez de {@code ILIKE}, já que o dialeto SQLite
     * não tem suporte nativo a {@code ILIKE} (ver plano técnico, seção 4.2).
     */
    static Specification<Book> matchesSearchTerm(String q) {
        String likePattern = "%" + q.toLowerCase() + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("author")), likePattern));
    }

    static Specification<Book> hasStatus(ReadingStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Igualdade exata (não substring, diferente de {@link #matchesSearchTerm}) e
     * case-insensitive em {@code genre}: gênero é texto livre, não enum (ver plano técnico).
     */
    static Specification<Book> matchesGenre(String genre) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("genre")), genre.toLowerCase());
    }

    /**
     * {@code rating >= minRating}. Separado de {@link #hasRatingLessThanOrEqualTo} (em vez
     * de um único {@code hasRatingBetween(min, max)}) para que {@code BookService.search}
     * possa combinar cada ponta da faixa com {@code .and(...)} independentemente, igual aos
     * demais filtros desta classe — {@code minRating} e {@code maxRating} podem chegar um
     * sem o outro.
     */
    static Specification<Book> hasRatingGreaterThanOrEqualTo(Integer minRating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), minRating);
    }

    /** {@code rating <= maxRating}. Ver {@link #hasRatingGreaterThanOrEqualTo}. */
    static Specification<Book> hasRatingLessThanOrEqualTo(Integer maxRating) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("rating"), maxRating);
    }
}
