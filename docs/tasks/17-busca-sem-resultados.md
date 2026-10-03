# US-17 — Lista vazia quando nenhum livro corresponde à busca

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#9-estratégia-de-testes)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero receber uma lista vazia (não um erro) quando nenhum livro corresponder à busca, para distinguir "sem resultados" de "algo quebrou".

## Critérios de aceite

- [ ] Busca sem nenhuma correspondência retorna `200` com `content: []` e `totalElements: 0`.
- [ ] Nenhuma combinação de filtros válidos sem resultado produz `404` ou `500`.

## Tarefas

- [ ] Confirmar que `BookService.search` retorna uma `Page` vazia (comportamento nativo do Spring Data) quando a `Specification` não encontra correspondência — não é necessário tratamento especial de código.
- [ ] Teste de integração: `GET /api/v1/books?q=termo-inexistente` retorna `200` com `content: []` e `totalElements: 0`.
- [ ] Teste de integração: combinação de filtros válidos sem nenhum livro correspondente (ex: `status=ABANDONADO&minRating=5`) retorna `200` vazio, não erro.
