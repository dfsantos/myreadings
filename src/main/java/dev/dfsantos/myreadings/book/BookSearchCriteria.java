package dev.dfsantos.myreadings.book;

/**
 * Agrupa os filtros opcionais de {@code GET /api/v1/books}, usados para montar a
 * {@link org.springframework.data.jpa.domain.Specification} combinada em
 * {@link BookService#search}.
 *
 * <p>{@code minRating}/{@code maxRating} (US-15) não fazem parte do bind implícito do
 * record no {@code BookController}: validação Bean ({@code @Min}/{@code @Max}) em
 * parâmetro de método de controller exige a anotação diretamente no parâmetro
 * {@code @RequestParam}, não em um campo de record bindado implicitamente. Por isso o
 * controller recebe esses dois campos separadamente e monta este record manualmente.
 *
 * @param q termo de busca livre, aplicado contra {@code title} e {@code author} (US-12)
 * @param status filtro exato por status de leitura (US-13)
 * @param genre filtro exato (case-insensitive) por gênero/categoria (US-14)
 * @param minRating nota mínima (inclusive), {@code 1}-{@code 5} (US-15)
 * @param maxRating nota máxima (inclusive), {@code 1}-{@code 5} (US-15)
 */
public record BookSearchCriteria(
        String q, ReadingStatus status, String genre, Integer minRating, Integer maxRating) {
}
