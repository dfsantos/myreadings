# US-16 — Combinar múltiplos filtros

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#10-rastreabilidade-com-a-spec)
**Depende de:** [US-12](12-buscar-por-titulo-autor.md), [US-13](13-filtrar-por-status.md), [US-14](14-filtrar-por-genero.md), [US-15](15-filtrar-por-faixa-avaliacao.md)

## História de usuário

> Como usuário, quero combinar múltiplos filtros (ex: gênero + status), para refinar a busca.

## Critérios de aceite

- [ ] `GET /api/v1/books?genre=Ficção científica&status=LIDO` aplica AND entre os dois critérios.
- [ ] Combinação de 3 ou mais filtros simultâneos (`q`, `status`, `genre`, `minRating`/`maxRating`) retorna apenas o subconjunto que satisfaz todos.
- [ ] Filtros não informados são simplesmente ignorados (não restringem o resultado).

## Tarefas

- [ ] Compor todas as `Specification`s parciais (texto, status, gênero, rating) em `BookService.search(...)` usando `Specification.allOf(...)`/`.and(...)`, ignorando as que não foram informadas no `BookSearchCriteria`.
- [ ] Garantir que `BookController.list` constrói um único `BookSearchCriteria` a partir de todos os query params recebidos antes de chamar `BookService.search`.
- [ ] Teste de integração: combinação `genre=Ficção científica&status=LIDO` retorna apenas livros que satisfazem ambos.
- [ ] Teste de integração: combinação de 4 filtros (`q`, `status`, `genre`, `minRating`) retorna o subconjunto correto.
- [ ] Teste de integração: nenhum filtro informado se comporta como a listagem simples (US-11).
