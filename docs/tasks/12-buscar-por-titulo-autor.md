# US-12 — Buscar por título ou autor (texto livre)

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero buscar por título ou autor usando texto livre, para encontrar rapidamente um livro específico.

## Critérios de aceite

- [ ] `GET /api/v1/books?q=termo` retorna livros cujo `title` OU `author` contenha o termo.
- [ ] A busca é case-insensitive.
- [ ] A busca é por substring (não exige correspondência exata).

## Tarefas

- [ ] Adicionar campo `q` em `BookSearchCriteria`.
- [ ] Implementar `Specification<Book>` que aplica `LOWER(title) LIKE %termo% OR LOWER(author) LIKE %termo%` quando `q` estiver presente.
- [ ] Expor query param `q` em `BookController.list`, repassando para `BookSearchCriteria`.
- [ ] Teste de integração: busca por termo presente no título retorna o livro esperado.
- [ ] Teste de integração: busca por termo presente apenas no autor também retorna o livro esperado.
- [ ] Teste de integração: busca case-insensitive (`q=duna` encontra livro cadastrado como `"Duna"`).
