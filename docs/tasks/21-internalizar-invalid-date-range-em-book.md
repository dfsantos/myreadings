# RF-03 — Internalizar `InvalidDateRangeException` em `book`

**Tipo:** Refatoração técnica (sem mudança de comportamento) — não faz
parte do backlog de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seções 3.1, 4.3 e 6, fase 3)
**Depende de:** [RF-02 — Fundir auth em user](20-fundir-auth-em-user.md) (ordem de execução do plano; não há dependência técnica real entre as duas)

## Objetivo da refatoração

> Mover `InvalidDateRangeException` de `common` para `book` — é uma regra
> de negócio exclusiva do catálogo de livros (`endDate >= startDate`, ver
> US-08), sem motivo para estar ao lado de utilitários genéricos como os
> `AttributeConverter`.

## Critérios de aceite

- [ ] `./gradlew build` passa sem nenhuma asserção de teste alterada.
- [ ] Nenhum status HTTP ou corpo de resposta muda para `POST`/`PATCH
      /api/v1/books` com datas inválidas — continua `400`/`validation-error`.
- [ ] `common/GlobalExceptionHandler.java` não contém mais
      `handleInvalidDateRange`.

## Tarefas

- [ ] Mover `common/InvalidDateRangeException.java` →
      `book/InvalidDateRangeException.java` (só `package`).
- [ ] Criar `book/BookExceptionHandler.java`
      (`@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.book")`)
      com o método `handleInvalidDateRange` extraído de
      `common/GlobalExceptionHandler.java` (corpo idêntico ao plano
      técnico, seção 4.3).
- [ ] Remover `handleInvalidDateRange` e o import de
      `InvalidDateRangeException` de `common/GlobalExceptionHandler.java`.
- [ ] Confirmar que `book/BookService.java` não precisa de nenhuma mudança
      de import (a exceção passa a estar no mesmo pacote `book`).
- [ ] Rodar `./gradlew build` e confirmar suíte completa verde.

## Observações

- Esta é a fase menor do conjunto — um único arquivo movido e um handler
  extraído. Pode ser feita isoladamente ou no mesmo commit da
  [RF-04](22-reduzir-global-exception-handler.md), a critério de quem
  implementar.
