# US-15 — Filtrar por faixa de avaliação

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero filtrar por faixa de avaliação (ex: nota >= 4), para relembrar meus livros favoritos.

## Critérios de aceite

- [x] `GET /api/v1/books?minRating=4` retorna apenas livros com nota ≥ 4.
- [x] `GET /api/v1/books?maxRating=3` retorna apenas livros com nota ≤ 3.
- [x] `minRating`/`maxRating` combinados filtram a faixa correta.
- [x] Valores fora de `1`–`5` retornam `400`.

## Tarefas

- [x] Adicionar campos `minRating`/`maxRating` em `BookSearchCriteria`.
- [x] Implementar `Specification<Book>` adicional: `rating >= :minRating` e/ou `rating <= :maxRating`, aplicados apenas quando informados.
- [x] Expor query params `minRating`/`maxRating` em `BookController.list`, com `@Min(1) @Max(5)` na validação do parâmetro.
- [x] Teste de integração: filtro `minRating=4` retorna apenas livros com nota ≥ 4.
- [x] Teste de integração: filtro `minRating=4&maxRating=5` retorna a faixa correta.
- [x] Teste de integração: `minRating=0` ou `maxRating=6` retorna `400`.

## Observações/Pendências

- Os dois critérios de "valores fora de `1`–`5` retornam `400`" e a tarefa
  de teste correspondente foram verificados via dois testes separados
  (`listWithMinRatingBelowOneReturns400` com `minRating=0`,
  `listWithMaxRatingAboveFiveReturns400` com `maxRating=6`) em
  `src/test/java/dev/dfsantos/myreadings/book/BookControllerTest.java` —
  não há teste cobrindo explicitamente `minRating=6` nem `maxRating=0`,
  mas a anotação `@Min(1) @Max(5)` é simétrica para os dois parâmetros, e
  o handler de `ConstraintViolationException` não distingue qual dos dois
  limites foi violado, então a cobertura existente é considerada
  suficiente.
- O relato original do agente `spring-boot-dev` mencionava "6 testes
  novos" cobrindo esta história; na verificação foram encontrados 5 testes
  diretamente ligados a US-15 (`listWithMinRatingReturnsOnlyBooksWithRatingGreaterThanOrEqual`,
  `listWithMaxRatingReturnsOnlyBooksWithRatingLessThanOrEqual`,
  `listWithMinRatingAndMaxRatingCombinedFiltersTheCorrectRange`,
  `listWithMinRatingBelowOneReturns400`, `listWithMaxRatingAboveFiveReturns400`).
  Divergência de contagem sem impacto nos critérios de aceite (todos
  cobertos) — registrada aqui por transparência, não por ser um problema.
