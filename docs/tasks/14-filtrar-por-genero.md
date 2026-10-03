# US-14 — Filtrar por gênero/categoria

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#1-decisões-que-resolvem-as-open-questions-da-spec)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero filtrar por gênero/categoria, para navegar minha coleção por tipo de livro.

## Critérios de aceite

- [ ] `GET /api/v1/books?genre=Ficção científica` retorna apenas livros daquele gênero.
- [ ] O filtro é case-insensitive (gênero é texto livre, não enum — decisão registrada no plano técnico).

## Tarefas

- [ ] Adicionar campo `genre` em `BookSearchCriteria`.
- [ ] Implementar `Specification<Book>` adicional: `LOWER(genre) = LOWER(:genre)` quando informado.
- [ ] Expor query param `genre` em `BookController.list`.
- [ ] Teste de integração: filtro `genre=Ficção científica` retorna apenas os livros daquele gênero.
- [ ] Teste de integração: filtro case-insensitive (`genre=ficção científica` encontra livro cadastrado como `"Ficção científica"`).
