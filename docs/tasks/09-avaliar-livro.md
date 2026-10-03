# US-09 — Dar nota (avaliação) a um livro

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-07 — Marcar status de leitura](07-marcar-status-leitura.md)

## História de usuário

> Como usuário, quero dar uma nota (avaliação) a um livro que terminei, para lembrar o que achei dele depois.

## Critérios de aceite

- [x] `rating` aceita valores inteiros de `1` a `5`.
- [x] `rating` fora da faixa (`0`, `6`, etc.) retorna `400`.
- [x] `rating` é opcional — livro pode ficar sem avaliação indefinidamente.

## Tarefas

- [x] Adicionar `@Min(1)` e `@Max(5)` no campo `rating` de `BookCreateRequest` e `BookUpdateRequest`.
- [x] Confirmar que `rating` nulo é aceito em ambos os DTOs (não obrigatório).
- [x] Confirmar que a coluna `rating` na tabela `books` permite `NULL` e tem `CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)` (já previsto na migração `V2__create_books_table.sql` da US-04).
- [x] Teste de integração: PATCH definindo `rating = 5` retorna `200` com o valor persistido.
- [x] Teste de integração: PATCH definindo `rating = 6` retorna `400`.
- [x] Teste de integração: PATCH definindo `rating = 0` retorna `400`.
- [x] Teste de integração: criação sem informar `rating` retorna `201` com `rating = null`.

## Observações / Pendências

- Nenhuma alteração de produção foi necessária para esta história: `rating`
  (`Integer`, `@Min(1)` `@Max(5)`) já existia em `BookCreateRequest` e
  `BookUpdateRequest`, e a coluna `rating` em `V2__create_books_table.sql`
  já tinha `CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)` sem
  `NOT NULL` — tudo antecipado durante a implementação da US-04. Mesmo
  padrão já registrado para a US-05.
- Trabalho desta história ficou restrito a 3 testes de integração novos em
  `BookControllerTest` (`patchSettingRatingToFiveReturns200AndPersistsRating`,
  `patchSettingRatingAboveFiveReturns400`,
  `patchSettingRatingBelowOneReturns400`). O critério "criação sem
  informar `rating` retorna `201` com `rating = null`" já estava coberto
  por testes preexistentes da US-04/US-05
  (`createWithoutStatusDefaultsToQueroLer`,
  `createWithTitleAuthorAndCoverUrlReturns201WithCoverUrlAndRemainingFieldsNull`),
  que já afirmam `$.rating` nulo na criação.
- Verificado lendo `BookCreateRequest.java`, `BookUpdateRequest.java`,
  `Book.java`, `V2__create_books_table.sql` e os testes citados em
  `BookControllerTest.java`, e confirmado com `./gradlew test` (suíte
  completa verde).
