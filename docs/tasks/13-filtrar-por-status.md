# US-13 — Filtrar por status de leitura

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero filtrar minha lista por status de leitura (ex: só "lendo"), para ver o que está em andamento.

## Critérios de aceite

- [x] `GET /api/v1/books?status=LIDO` retorna apenas livros com esse status.
- [x] Valor de `status` fora do enum `ReadingStatus` retorna `400`.

## Tarefas

- [x] Adicionar campo `status` (`ReadingStatus`) em `BookSearchCriteria`.
- [x] Implementar `Specification<Book>` adicional: `status = :status` quando informado.
- [x] Expor query param `status` em `BookController.list`, com binding direto para o enum (Spring converte automaticamente; erro de conversão deve ser tratado).
- [x] Mapear erro de conversão de enum inválido (`MethodArgumentTypeMismatchException`) → `400` no `GlobalExceptionHandler`.
- [x] Teste de integração: filtro `status=LIDO` retorna apenas livros com esse status.
- [x] Teste de integração: `status=VALOR_INVALIDO` retorna `400`.
