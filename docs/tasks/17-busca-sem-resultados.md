# US-17 — Lista vazia quando nenhum livro corresponde à busca

**Grupo da spec:** Consulta e busca
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#9-estratégia-de-testes)
**Depende de:** [US-11 — Listar todos os livros do catálogo](11-listar-livros.md)

## História de usuário

> Como usuário, quero receber uma lista vazia (não um erro) quando nenhum livro corresponder à busca, para distinguir "sem resultados" de "algo quebrou".

## Critérios de aceite

- [x] Busca sem nenhuma correspondência retorna `200` com `content: []` e `totalElements: 0`.
- [x] Nenhuma combinação de filtros válidos sem resultado produz `404` ou `500`.

## Tarefas

- [x] Confirmar que `BookService.search` retorna uma `Page` vazia (comportamento nativo do Spring Data) quando a `Specification` não encontra correspondência — não é necessário tratamento especial de código.
- [x] Teste de integração: `GET /api/v1/books?q=termo-inexistente` retorna `200` com `content: []` e `totalElements: 0`.
- [x] Teste de integração: combinação de filtros válidos sem nenhum livro correspondente (ex: `status=ABANDONADO&minRating=5`) retorna `200` vazio, não erro.

## Observações

- Nenhuma alteração de produção foi necessária — `BookService.search`
  (`src/main/java/dev/dfsantos/myreadings/book/BookService.java`) usa
  `bookRepository.findAll(specification, pageable)`, e o Spring Data já
  retorna uma `Page` vazia nativamente quando a `Specification` não
  encontra correspondência, sem lançar erro. Mesmo padrão de "nenhuma
  mudança de código de produção necessária" já observado em US-05/US-09.
- Trabalho realizado: dois testes de integração novos em
  `src/test/java/dev/dfsantos/myreadings/book/BookControllerTest.java` —
  `listWithQNotMatchingAnyBookReturns200WithEmptyContentAndZeroTotalElements`
  e `listWithValidFilterCombinationMatchingNoBookReturns200WithEmptyContent`.
  Confirmado lendo o código-fonte (não apenas o relato do agente
  implementador) e com `./gradlew test --tests
  "dev.dfsantos.myreadings.book.BookControllerTest"` passando (BUILD
  SUCCESSFUL).
- `./gradlew test` rodando a suíte completa falhou, mas por um erro de
  infraestrutura do Gradle (`NoSuchFileException`/`EOFException` na
  escrita do relatório binário de resultados em
  `build/test-results/test/binary/`), não por falha de teste — o mesmo
  erro ocorre mesmo filtrando apenas classes de `auth` (sem nenhuma classe
  de `book`), e nenhum teste chega a ser reportado como falho nos poucos
  casos em que um XML parcial é gerado. Parece ser um problema de ambiente
  pré-existente e não relacionado a esta história; sinalizado ao usuário.
