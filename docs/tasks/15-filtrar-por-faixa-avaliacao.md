# US-15 — Filtrar por faixa de avaliação

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero filtrar por faixa de avaliação (ex: nota >= 4), para relembrar meus livros favoritos.

## Critérios de aceite

- [ ] `GET /api/v1/books?minRating=4` retorna apenas livros com nota ≥ 4.
- [ ] `GET /api/v1/books?maxRating=3` retorna apenas livros com nota ≤ 3.
- [ ] `minRating`/`maxRating` combinados filtram a faixa correta.
- [ ] Valores fora de `1`–`5` retornam `400`.

## Tarefas

- [ ] Adicionar campos `minRating`/`maxRating` em `BookSearchCriteria`.
- [ ] Implementar `Specification<Book>` adicional: `rating >= :minRating` e/ou `rating <= :maxRating`, aplicados apenas quando informados.
- [ ] Expor query params `minRating`/`maxRating` em `BookController.list`, com `@Min(1) @Max(5)` na validação do parâmetro.
- [ ] Teste de integração: filtro `minRating=4` retorna apenas livros com nota ≥ 4.
- [ ] Teste de integração: filtro `minRating=4&maxRating=5` retorna a faixa correta.
- [ ] Teste de integração: `minRating=0` ou `maxRating=6` retorna `400`.
