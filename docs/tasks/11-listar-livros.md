# US-11 — Listar todos os livros do catálogo

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero listar todos os livros do meu catálogo, para ter uma visão geral da minha coleção.

## Critérios de aceite

- [x] `GET /api/v1/books` sem parâmetros retorna todos os livros do usuário autenticado, paginados.
- [x] Nenhum livro de outro usuário aparece na listagem.
- [x] Resposta usa o envelope de paginação (`content`, `totalElements`, `totalPages`, `number`, `size`).
- [x] Paginação default: `page=0`, `size=20`, ordenado por `createdAt,desc`; tamanho máximo de página `100`.

## Tarefas

- [x] Criar `BookSearchCriteria` (`book/BookSearchCriteria.java`) — inicialmente vazio, será preenchido pelas próximas histórias (US-12 a US-16).
- [x] Implementar `BookService.search(...)` usando `BookRepository.findAll(Specification, Pageable)`, sempre filtrando por `userId` do contexto de segurança.
- [x] Implementar `BookController.list` → `GET /api/v1/books`, aceitando `Pageable` (via `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`).
- [x] Limitar `size` máximo a `100` (validação no controller ou configuração global do Spring Data).
- [x] Teste de integração: listagem sem parâmetros retorna todos os livros do usuário autenticado.
- [x] Teste de integração: listagem não retorna livros cadastrados por outro usuário.
- [x] Teste de integração: paginação respeita `page`/`size` informados.

## Observações

- Além das três tarefas de teste listadas, o agente implementador
  acrescentou um quarto teste (`listWithoutAuthorizationHeaderReturns401`)
  não previsto explicitamente nesta checklist, cobrindo o caso de ausência
  de token — consistente com o padrão já estabelecido para os demais
  endpoints de `Book`. Não é um critério de aceite desta história, mas
  reforça o isolamento por usuário (US-03).
- `size` máximo foi limitado via propriedade global do Spring Data Web
  (`spring.data.web.pageable.max-page-size: 100` em `application.yaml`),
  não por validação manual no Controller — ver nota de implementação na
  seção 6.2 do plano técnico.
