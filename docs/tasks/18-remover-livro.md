# US-18 — Remover livro

**Grupo da spec:** Remoção
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04](04-cadastrar-livro-dados-basicos.md), [US-03](03-isolamento-por-usuario.md)

## História de usuário

> Como usuário, quero remover um livro cadastrado por engano ou que não quero mais acompanhar, para manter meu catálogo limpo.

## Critérios de aceite

- [ ] `DELETE /api/v1/books/{id}` de um livro próprio retorna `204 No Content`.
- [ ] O livro removido não aparece mais em listagens/buscas subsequentes.
- [ ] `DELETE` em livro de outro usuário ou id inexistente retorna `404`.

## Tarefas

- [ ] Implementar `BookService.delete(...)`: busca o livro garantindo `userId` igual ao do contexto autenticado (senão lança `NotFoundException`), remove via `BookRepository`.
- [ ] Implementar `BookController.delete` → `DELETE /api/v1/books/{id}`, retorna `204`.
- [ ] Teste de integração: DELETE de livro próprio retorna `204`.
- [ ] Teste de integração: GET subsequente ao mesmo id retorna `404` após a remoção.
- [ ] Teste de integração: DELETE de livro de outro usuário retorna `404` (sem remover o recurso).
- [ ] Teste de integração: DELETE de id inexistente retorna `404`.
