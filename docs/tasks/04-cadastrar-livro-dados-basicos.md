# US-04 — Cadastrar livro com dados básicos e capa

**Grupo da spec:** Cadastro de livros/leituras
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-03 — Isolamento automático por usuário](03-isolamento-por-usuario.md)

## História de usuário

> Como usuário, quero cadastrar um livro informando dados básicos (título, autor, editora, ano, número de páginas, gênero) e uma URL de capa, para registrar que estou lendo ou já li esse livro.

## Critérios de aceite

- [x] `POST /api/v1/books` com todos os campos preenchidos retorna `201` com o recurso criado, incluindo `id` gerado.
- [x] O livro criado é automaticamente associado ao usuário autenticado (via token), sem campo `userId` no corpo da requisição.
- [x] `coverUrl` é persistida como string.

## Tarefas

- [x] Criar migração Flyway `src/main/resources/db/migration/V2__create_books_table.sql` (tabela `books` conforme seção 4.2 do plano técnico, com índices `idx_books_user_id`, `idx_books_user_status`, `idx_books_user_genre`).
- [x] Criar enum `ReadingStatus` (`book/ReadingStatus.java`): `QUERO_LER`, `LENDO`, `LIDO`, `ABANDONADO`.
- [x] Criar entidade `Book` (`book/Book.java`) com todos os campos (`title`, `author`, `publisher`, `publicationYear`, `pageCount`, `genre`, `coverUrl`, `status` com `@Enumerated(EnumType.STRING)`, `startDate`, `endDate`, `rating`, `createdAt`, `updatedAt`, `userId`).
- [x] Criar `BookRepository` (`book/BookRepository.java`) estendendo `JpaRepository<Book, UUID>` e `JpaSpecificationExecutor<Book>`.
- [x] Criar DTO `BookCreateRequest` (`book/dto/BookCreateRequest.java`) com `title`/`author` obrigatórios e demais campos opcionais.
- [x] Criar DTO `BookResponse` (`book/dto/BookResponse.java`) e mapper entidade ↔ DTO.
- [x] Implementar `BookService.create(...)`: associa `userId` do contexto de segurança (ver US-03), persiste via `BookRepository`.
- [x] Implementar `BookController.create` → `POST /api/v1/books`, retorna `201` com `BookResponse`.
- [x] Teste de integração: criação com todos os campos preenchidos retorna `201` com `id` gerado e todos os valores persistidos corretamente, incluindo `coverUrl`.

## Observações / Pendências

- Verificado no código (`book/Book.java`, `book/ReadingStatus.java`,
  `book/BookRepository.java`, `book/dto/BookCreateRequest.java`,
  `book/dto/BookResponse.java`, `book/BookService.java`,
  `book/BookController.java`,
  `src/main/resources/db/migration/V2__create_books_table.sql`) e por
  `./gradlew test` (suíte completa verde, 20/20, incluindo os 5 testes de
  `BookControllerTest` que cobrem os 3 critérios de aceite acima).
- `BookCreateRequest` já inclui `status`, `startDate`, `endDate` e
  `rating` como campos opcionais, além dos citados na história, com
  `@Min(1)`/`@Max(5)` em `rating` antecipando a validação de avaliação da
  US-09. Isso é coerente com o exemplo de `BookCreateRequest` já descrito
  na seção 6.2 do plano técnico (que já mostrava `status`/`startDate` no
  payload de criação) — não é escopo novo, só adianta a implementação de
  um campo que o plano já previa para o create.
- Novo `common/LocalDateStringConverter` (não listado nas tarefas desta
  história, mas necessário para persistir `startDate`/`endDate`) — ver
  nota correspondente no plano técnico (seção 4.1) e em `CLAUDE.md`.
- O terceiro critério de aceite da US-03 (404 em livro de outro usuário)
  não era observável nesta história (só existia `POST /books`, sem
  `GET/PATCH/DELETE /books/{id}`). A US-07 (`PATCH /api/v1/books/{id}`)
  passou a expor esse caminho e o critério foi marcado em
  `docs/tasks/03-isolamento-por-usuario.md`, com o teste
  `patchBookOfAnotherUserReturns404` (`BookControllerTest`) como
  evidência.
