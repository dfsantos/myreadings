# US-11 — Listar todos os livros do catálogo

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero listar todos os livros do meu catálogo, para ter uma visão geral da minha coleção.

## Critérios de aceite

- [ ] `GET /api/v1/books` sem parâmetros retorna todos os livros do usuário autenticado, paginados.
- [ ] Nenhum livro de outro usuário aparece na listagem.
- [ ] Resposta usa o envelope de paginação (`content`, `totalElements`, `totalPages`, `number`, `size`).
- [ ] Paginação default: `page=0`, `size=20`, ordenado por `createdAt,desc`; tamanho máximo de página `100`.

## Tarefas

- [ ] Criar `BookSearchCriteria` (`book/BookSearchCriteria.java`) — inicialmente vazio, será preenchido pelas próximas histórias (US-12 a US-16).
- [ ] Implementar `BookService.search(...)` usando `BookRepository.findAll(Specification, Pageable)`, sempre filtrando por `userId` do contexto de segurança.
- [ ] Implementar `BookController.list` → `GET /api/v1/books`, aceitando `Pageable` (via `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`).
- [ ] Limitar `size` máximo a `100` (validação no controller ou configuração global do Spring Data).
- [ ] Teste de integração: listagem sem parâmetros retorna todos os livros do usuário autenticado.
- [ ] Teste de integração: listagem não retorna livros cadastrados por outro usuário.
- [ ] Teste de integração: paginação respeita `page`/`size` informados.
