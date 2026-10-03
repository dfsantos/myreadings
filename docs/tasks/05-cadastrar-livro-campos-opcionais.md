# US-05 — Cadastrar livro sem preencher campos opcionais

**Grupo da spec:** Cadastro de livros/leituras
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero cadastrar um livro sem preencher todos os campos opcionais (ex: sem capa, sem editora), para não ser bloqueado por informação que não tenho em mãos.

## Critérios de aceite

- [x] `POST /api/v1/books` enviando apenas `title` e `author` retorna `201`.
- [x] Campos opcionais omitidos ficam `null` na resposta, exceto `status`, que assume o valor default `QUERO_LER`.

## Tarefas

- [x] Confirmar que `BookCreateRequest` não possui `@NotNull`/`@NotBlank` em nenhum campo além de `title` e `author`.
- [x] Implementar valor default `status = QUERO_LER` quando omitido na criação (em `BookService.create` ou como default da entidade `Book`).
- [x] Teste de integração: criação enviando somente `title` e `author` retorna `201` com os demais campos `null` e `status = QUERO_LER`.
- [x] Teste de integração: criação informando `title`, `author` e `coverUrl` (sem os demais opcionais) retorna `201` com `coverUrl` persistida e o restante `null`.
