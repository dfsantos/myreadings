# US-07 — Marcar status de leitura

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero marcar o status de um livro (quero ler / lendo / lido / abandonado), para saber em que ponto estou com cada leitura.

## Critérios de aceite

- [x] `PATCH /api/v1/books/{id}` alterando apenas `status` retorna `200` com o novo status aplicado e os demais campos inalterados.
- [x] Enviar um valor de `status` fora do enum `ReadingStatus` retorna `400`.
- [x] `PATCH` em livro de outro usuário ou inexistente retorna `404`.

## Tarefas

- [x] Criar DTO `BookUpdateRequest` (`book/dto/BookUpdateRequest.java`) com todos os campos opcionais, incluindo `status`.
- [x] Criar exceção `NotFoundException` (`common/NotFoundException.java`) e mapear para `404` no `GlobalExceptionHandler`.
- [x] Implementar `BookService.update(...)`: busca o livro garantindo `userId` igual ao do contexto autenticado (senão lança `NotFoundException`), aplica somente os campos não nulos do `BookUpdateRequest`, persiste e atualiza `updatedAt`.
- [x] Implementar `BookController.update` → `PATCH /api/v1/books/{id}`, retorna `200` com `BookResponse` atualizado.
- [x] Garantir que valor de `status` fora do enum é rejeitado com `400` (mapear `HttpMessageNotReadableException`/erro de deserialização do enum no `GlobalExceptionHandler`, se necessário).
- [x] Teste de integração: PATCH alterando apenas `status` retorna `200` e preserva os demais campos.
- [x] Teste de integração: PATCH com `status` inválido (ex: `"LENDO_DEMAIS"`) retorna `400`.
- [x] Teste de integração: PATCH em livro de outro usuário retorna `404`.

## Observações / Pendências

- Verificado no código (`book/dto/BookUpdateRequest.java`,
  `common/NotFoundException.java`, `book/BookRepository.findByIdAndUserId`,
  `book/BookService.update`, `book/BookController.update`,
  `common/GlobalExceptionHandler.handleMessageNotReadable`) e por
  `./gradlew test` (suíte completa verde: 27 testes, 0 falhas), incluindo
  os 4 testes novos em `BookControllerTest`:
  `patchChangingOnlyStatusReturns200AndPreservesRemainingFields`,
  `patchWithInvalidStatusValueReturns400`,
  `patchBookOfAnotherUserReturns404`, `patchNonExistentBookReturns404`.
- `BookService.update` reaproveita a convenção de `Book` como entidade
  imutável: monta uma nova instância via construtor completo em vez de
  usar setters, copiando os campos não enviados no PATCH a partir do
  registro existente — ver nota em `CLAUDE.md`/plano técnico (seção 6.2).
- O terceiro critério de aceite desta história também fecha o terceiro
  critério de aceite pendente da US-03 (404 em livro de outro usuário) —
  ver `docs/tasks/03-isolamento-por-usuario.md`.
