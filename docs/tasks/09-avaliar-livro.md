# US-09 — Dar nota (avaliação) a um livro

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-07 — Marcar status de leitura](07-marcar-status-leitura.md)

## História de usuário

> Como usuário, quero dar uma nota (avaliação) a um livro que terminei, para lembrar o que achei dele depois.

## Critérios de aceite

- [ ] `rating` aceita valores inteiros de `1` a `5`.
- [ ] `rating` fora da faixa (`0`, `6`, etc.) retorna `400`.
- [ ] `rating` é opcional — livro pode ficar sem avaliação indefinidamente.

## Tarefas

- [ ] Adicionar `@Min(1)` e `@Max(5)` no campo `rating` de `BookCreateRequest` e `BookUpdateRequest`.
- [ ] Confirmar que `rating` nulo é aceito em ambos os DTOs (não obrigatório).
- [ ] Confirmar que a coluna `rating` na tabela `books` permite `NULL` e tem `CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)` (já previsto na migração `V2__create_books_table.sql` da US-04).
- [ ] Teste de integração: PATCH definindo `rating = 5` retorna `200` com o valor persistido.
- [ ] Teste de integração: PATCH definindo `rating = 6` retorna `400`.
- [ ] Teste de integração: PATCH definindo `rating = 0` retorna `400`.
- [ ] Teste de integração: criação sem informar `rating` retorna `201` com `rating = null`.
