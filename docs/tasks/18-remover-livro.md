# US-18 — Remover livro

**Grupo da spec:** Remoção
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#62-livros-apiv1books)
**Depende de:** [US-04](04-cadastrar-livro-dados-basicos.md), [US-03](03-isolamento-por-usuario.md)

## História de usuário

> Como usuário, quero remover um livro cadastrado por engano ou que não quero mais acompanhar, para manter meu catálogo limpo.

## Critérios de aceite

- [x] `DELETE /api/v1/books/{id}` de um livro próprio retorna `204 No Content`.
- [x] O livro removido não aparece mais em listagens/buscas subsequentes.
- [x] `DELETE` em livro de outro usuário ou id inexistente retorna `404`.

## Tarefas

- [x] Implementar `BookService.delete(...)`: busca o livro garantindo `userId` igual ao do contexto autenticado (senão lança `NotFoundException`), remove via `BookRepository`.
- [x] Implementar `BookController.delete` → `DELETE /api/v1/books/{id}`, retorna `204`.
- [x] Teste de integração: DELETE de livro próprio retorna `204`.
- [x] Teste de integração: GET subsequente ao mesmo id retorna `404` após a remoção.
- [x] Teste de integração: DELETE de livro de outro usuário retorna `404` (sem remover o recurso).
- [x] Teste de integração: DELETE de id inexistente retorna `404`.

## Observações

- **Gap de backlog identificado e resolvido nesta história:** o endpoint
  `GET /api/v1/books/{id}` (busca de um livro único por id) já estava
  previsto na tabela de rotas da seção 6.2 do plano técnico, mas não
  constava como tarefa explícita em nenhuma história do backlog (US-01 a
  US-18). Ele foi implementado como parte da US-18 porque o próprio
  critério de aceite "GET subsequente ao mesmo id retorna 404 após a
  remoção" exige que esse endpoint exista para ser testável. Implementado
  em `BookService.findById(UUID)` (mesmo padrão de
  `findByIdAndUserId` + `NotFoundException` usado em `update`/`delete`) e
  `BookController.get` → `GET /api/v1/books/{id}` → `200` com
  `BookResponse`. Nenhuma história cobriu esse endpoint isoladamente;
  está coberto por 3 testes de integração nesta mesma história (livro
  próprio, livro de outro usuário, id inexistente), além de ser exercitado
  indiretamente pelos testes de `DELETE`. Sinalizado ao usuário — não é
  invenção de escopo (a rota já estava na spec/plano), apenas uma lacuna
  de rastreabilidade entre o plano técnico e a lista de tasks.
- Verificado no código: `BookService.java` e `BookController.java`
  (`src/main/java/dev/dfsantos/myreadings/book/`), testes em
  `BookControllerTest.java`
  (`src/test/java/dev/dfsantos/myreadings/book/`), 8 testes novos (3 de
  GET por id, 5 de DELETE). `./gradlew test` com suíte completa: 66
  testes, 0 falhas. **Esta história encerra o backlog completo (US-01 a
  US-18, todas concluídas).**
