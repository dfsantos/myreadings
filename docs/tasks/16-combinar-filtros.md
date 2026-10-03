# US-16 — Combinar múltiplos filtros

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#10-rastreabilidade-com-a-spec)
**Depende de:** [US-12](12-buscar-por-titulo-autor.md), [US-13](13-filtrar-por-status.md), [US-14](14-filtrar-por-genero.md), [US-15](15-filtrar-por-faixa-avaliacao.md)

## História de usuário

> Como usuário, quero combinar múltiplos filtros (ex: gênero + status), para refinar a busca.

## Critérios de aceite

- [x] `GET /api/v1/books?genre=Ficção científica&status=LIDO` aplica AND entre os dois critérios.
- [x] Combinação de 3 ou mais filtros simultâneos (`q`, `status`, `genre`, `minRating`/`maxRating`) retorna apenas o subconjunto que satisfaz todos.
- [x] Filtros não informados são simplesmente ignorados (não restringem o resultado).

## Tarefas

- [x] Compor todas as `Specification`s parciais (texto, status, gênero, rating) em `BookService.search(...)` usando `Specification.allOf(...)`/`.and(...)`, ignorando as que não foram informadas no `BookSearchCriteria`.
- [x] Garantir que `BookController.list` constrói um único `BookSearchCriteria` a partir de todos os query params recebidos antes de chamar `BookService.search`.
- [x] Teste de integração: combinação `genre=Ficção científica&status=LIDO` retorna apenas livros que satisfazem ambos.
- [x] Teste de integração: combinação de 4 filtros (`q`, `status`, `genre`, `minRating`) retorna o subconjunto correto.
- [x] Teste de integração: nenhum filtro informado se comporta como a listagem simples (US-11).

## Observações

Nenhuma alteração de produção foi necessária para esta história: `BookService.search`
já compunha `hasUserId` com `.and(...)` condicional para cada filtro presente em
`BookSearchCriteria` (sem `.or()` nem `.and()` incondicional), e `BookController.list`
já montava um `BookSearchCriteria` completo a partir do bind implícito (`q`/`status`/
`genre`) combinado com `minRating`/`maxRating` validados via `@RequestParam`. O
trabalho desta história foi adicionar os 3 testes de integração que comprovam o
comportamento AND já existente: `listWithGenreAndStatusCombinedAppliesAndBetweenBothCriteria`,
`listWithFourFiltersCombinedReturnsOnlyTheSubsetMatchingAllOfThem` e
`listWithoutAnyFilterIgnoresAllOfThemAndBehavesLikeSimpleListing`, em
`src/test/java/dev/dfsantos/myreadings/book/BookControllerTest.java`. Verificado lendo
`BookService.java`, `BookController.java`, `BookSpecifications.java` e
`BookSearchCriteria.java`, e confirmado com `./gradlew test --rerun` (BUILD SUCCESSFUL,
41 testes em `BookControllerTest`, 0 falhas).
