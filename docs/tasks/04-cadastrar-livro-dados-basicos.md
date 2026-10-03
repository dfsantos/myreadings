# US-04 — Cadastrar livro com dados básicos e capa

**Grupo da spec:** Cadastro de livros/leituras
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#42-books)
**Depende de:** [US-03 — Isolamento automático por usuário](03-isolamento-por-usuario.md)

## História de usuário

> Como usuário, quero cadastrar um livro informando dados básicos (título, autor, editora, ano, número de páginas, gênero) e uma URL de capa, para registrar que estou lendo ou já li esse livro.

## Critérios de aceite

- [ ] `POST /api/v1/books` com todos os campos preenchidos retorna `201` com o recurso criado, incluindo `id` gerado.
- [ ] O livro criado é automaticamente associado ao usuário autenticado (via token), sem campo `userId` no corpo da requisição.
- [ ] `coverUrl` é persistida como string.

## Tarefas

- [ ] Criar migração Flyway `src/main/resources/db/migration/V2__create_books_table.sql` (tabela `books` conforme seção 4.2 do plano técnico, com índices `idx_books_user_id`, `idx_books_user_status`, `idx_books_user_genre`).
- [ ] Criar enum `ReadingStatus` (`book/ReadingStatus.java`): `QUERO_LER`, `LENDO`, `LIDO`, `ABANDONADO`.
- [ ] Criar entidade `Book` (`book/Book.java`) com todos os campos (`title`, `author`, `publisher`, `publicationYear`, `pageCount`, `genre`, `coverUrl`, `status` com `@Enumerated(EnumType.STRING)`, `startDate`, `endDate`, `rating`, `createdAt`, `updatedAt`, `userId`).
- [ ] Criar `BookRepository` (`book/BookRepository.java`) estendendo `JpaRepository<Book, UUID>` e `JpaSpecificationExecutor<Book>`.
- [ ] Criar DTO `BookCreateRequest` (`book/dto/BookCreateRequest.java`) com `title`/`author` obrigatórios e demais campos opcionais.
- [ ] Criar DTO `BookResponse` (`book/dto/BookResponse.java`) e mapper entidade ↔ DTO.
- [ ] Implementar `BookService.create(...)`: associa `userId` do contexto de segurança (ver US-03), persiste via `BookRepository`.
- [ ] Implementar `BookController.create` → `POST /api/v1/books`, retorna `201` com `BookResponse`.
- [ ] Teste de integração: criação com todos os campos preenchidos retorna `201` com `id` gerado e todos os valores persistidos corretamente, incluindo `coverUrl`.
