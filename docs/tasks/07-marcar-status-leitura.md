# US-07 — Marcar status de leitura

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero marcar o status de um livro (quero ler / lendo / lido / abandonado), para saber em que ponto estou com cada leitura.

## Critérios de aceite

- [ ] `PATCH /api/v1/books/{id}` alterando apenas `status` retorna `200` com o novo status aplicado e os demais campos inalterados.
- [ ] Enviar um valor de `status` fora do enum `ReadingStatus` retorna `400`.
- [ ] `PATCH` em livro de outro usuário ou inexistente retorna `404`.

## Tarefas

- [ ] Criar DTO `BookUpdateRequest` (`book/dto/BookUpdateRequest.java`) com todos os campos opcionais, incluindo `status`.
- [ ] Criar exceção `NotFoundException` (`common/NotFoundException.java`) e mapear para `404` no `GlobalExceptionHandler`.
- [ ] Implementar `BookService.update(...)`: busca o livro garantindo `userId` igual ao do contexto autenticado (senão lança `NotFoundException`), aplica somente os campos não nulos do `BookUpdateRequest`, persiste e atualiza `updatedAt`.
- [ ] Implementar `BookController.update` → `PATCH /api/v1/books/{id}`, retorna `200` com `BookResponse` atualizado.
- [ ] Garantir que valor de `status` fora do enum é rejeitado com `400` (mapear `HttpMessageNotReadableException`/erro de deserialização do enum no `GlobalExceptionHandler`, se necessário).
- [ ] Teste de integração: PATCH alterando apenas `status` retorna `200` e preserva os demais campos.
- [ ] Teste de integração: PATCH com `status` inválido (ex: `"LENDO_DEMAIS"`) retorna `400`.
- [ ] Teste de integração: PATCH em livro de outro usuário retorna `404`.
