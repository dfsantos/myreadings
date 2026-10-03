# US-12 — Buscar por título ou autor (texto livre)

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero buscar por título ou autor usando texto livre, para encontrar rapidamente um livro específico.

## Critérios de aceite

- [x] `GET /api/v1/books?q=termo` retorna livros cujo `title` OU `author` contenha o termo.
- [x] A busca é case-insensitive.
- [x] A busca é por substring (não exige correspondência exata).

## Tarefas

- [x] Adicionar campo `q` em `BookSearchCriteria`.
- [x] Implementar `Specification<Book>` que aplica `LOWER(title) LIKE %termo% OR LOWER(author) LIKE %termo%` quando `q` estiver presente.
- [x] Expor query param `q` em `BookController.list`, repassando para `BookSearchCriteria`.
- [x] Teste de integração: busca por termo presente no título retorna o livro esperado.
- [x] Teste de integração: busca por termo presente apenas no autor também retorna o livro esperado.
- [x] Teste de integração: busca case-insensitive (`q=duna` encontra livro cadastrado como `"Duna"`).

## Observações

- A implementação seguiu a arquitetura `Specification`/`BookSpecifications` (padrão
  estabelecido pela US-11), em vez do `@Query` que a seção 10 do plano técnico
  mencionava originalmente como mecanismo esperado. O plano técnico foi atualizado
  para refletir o mecanismo real.
