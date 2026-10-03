# US-08 — Registrar datas de início e término da leitura

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-07 — Marcar status de leitura](07-marcar-status-leitura.md)

## História de usuário

> Como usuário, quero registrar a data de início e a data de término da leitura, para acompanhar quanto tempo levei.

## Critérios de aceite

- [x] `PATCH /api/v1/books/{id}` (ou `POST` na criação) aceita `startDate` e `endDate` em formato ISO-8601 (`YYYY-MM-DD`).
- [x] Informar `endDate` anterior a `startDate` retorna `400`.
- [x] Datas válidas são persistidas e retornadas corretamente.

## Tarefas

- [x] Confirmar que `startDate`/`endDate` (`LocalDate`) estão presentes em `BookCreateRequest` e `BookUpdateRequest`.
- [x] Criar exceção customizada `InvalidDateRangeException` (`common/InvalidDateRangeException.java`).
- [x] Implementar validação cruzada em `BookService` (nos métodos `create` e `update`): se `startDate` e `endDate` estiverem ambos presentes, `endDate` deve ser `>= startDate`; senão, lançar `InvalidDateRangeException`.
- [x] Mapear `InvalidDateRangeException` → `400` no `GlobalExceptionHandler`, com mensagem explicando a violação.
- [x] Teste de integração: PATCH com `endDate` anterior a `startDate` retorna `400`.
- [x] Teste de integração: PATCH com `startDate` e `endDate` válidos (fim >= início) retorna `200` e persiste ambas as datas.
- [x] Teste de integração: PATCH informando somente `startDate` (sem `endDate`) é aceito normalmente.

## Observações / Pendências

- Verificado no código (`common/InvalidDateRangeException.java`,
  `book/BookService.validateDateRange` chamado em `create` e `update`,
  `common/GlobalExceptionHandler.handleInvalidDateRange`) e por
  `./gradlew test` (suíte completa verde), incluindo os 5 testes novos em
  `BookControllerTest`: teste de criação com `endDate` anterior a
  `startDate`, `patchWithEndDateBeforeStartDateReturns400`,
  `patchWithValidStartDateAndEndDateReturns200AndPersistsBothDates`,
  `patchWithOnlyStartDateIsAcceptedWhenNoEndDateIsPersisted` e
  `patchWithOnlyStartDateReturns400WhenItConflictsWithAlreadyPersistedEndDate`.
- Detalhe importante de implementação: no `update`, `validateDateRange` é
  chamado com o **estado final já mesclado** (`startDate`/`endDate` após
  aplicar o `BookUpdateRequest` sobre o registro existente), não com os
  campos isolados do corpo do PATCH. Isso cobre o caso em que o cliente
  envia só `startDate` (ou só `endDate`) e o valor já persistido no outro
  campo passa a formar um intervalo inválido — ver nota equivalente em
  `CLAUDE.md` e na seção 6.3 do plano técnico, que generaliza essa
  convenção para qualquer validação cruzada futura sobre merge parcial.
