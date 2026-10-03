# US-08 — Registrar datas de início e término da leitura

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-07 — Marcar status de leitura](07-marcar-status-leitura.md)

## História de usuário

> Como usuário, quero registrar a data de início e a data de término da leitura, para acompanhar quanto tempo levei.

## Critérios de aceite

- [ ] `PATCH /api/v1/books/{id}` (ou `POST` na criação) aceita `startDate` e `endDate` em formato ISO-8601 (`YYYY-MM-DD`).
- [ ] Informar `endDate` anterior a `startDate` retorna `400`.
- [ ] Datas válidas são persistidas e retornadas corretamente.

## Tarefas

- [ ] Confirmar que `startDate`/`endDate` (`LocalDate`) estão presentes em `BookCreateRequest` e `BookUpdateRequest`.
- [ ] Criar exceção customizada `InvalidDateRangeException` (`common/InvalidDateRangeException.java`).
- [ ] Implementar validação cruzada em `BookService` (nos métodos `create` e `update`): se `startDate` e `endDate` estiverem ambos presentes, `endDate` deve ser `>= startDate`; senão, lançar `InvalidDateRangeException`.
- [ ] Mapear `InvalidDateRangeException` → `400` no `GlobalExceptionHandler`, com mensagem explicando a violação.
- [ ] Teste de integração: PATCH com `endDate` anterior a `startDate` retorna `400`.
- [ ] Teste de integração: PATCH com `startDate` e `endDate` válidos (fim >= início) retorna `200` e persiste ambas as datas.
- [ ] Teste de integração: PATCH informando somente `startDate` (sem `endDate`) é aceito normalmente.
