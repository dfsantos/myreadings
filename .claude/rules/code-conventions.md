# Convenções de Código

Aplica-se a todo código em `src/`. Reflete as decisões já tomadas em
`docs/plans/catalogo-de-leituras-backend.md` — em caso de divergência entre
este documento e o plano técnico, o plano técnico descreve o *o quê*, este
documento descreve *como escrever* o código que o implementa.

## Idioma

- Identificadores de código (pacotes, classes, métodos, variáveis) em
  **inglês**.
- Comentários no código, quando necessários, em português — só escrever
  comentário quando o *porquê* não for óbvio pelo código; nunca descrever o
  *o quê* (nome bem escolhido já faz isso).
- Documentação (`docs/`), commits e PRs em português.
- Mensagens de erro retornadas pela API (`ProblemDetail`) em português — é
  o idioma do único público-alvo do backend nesta fase.

## Estrutura de pacotes

```
dev.dfsantos.myreadings
├── config/   # SecurityConfig, OpenApiConfig
├── auth/     # AuthController, AuthService, JwtTokenProvider, JwtAuthenticationFilter, dto/
├── user/     # User (entidade), UserRepository
├── book/     # Book (entidade), ReadingStatus, BookRepository, BookService, BookController,
             # BookSearchCriteria, dto/
└── common/   # GlobalExceptionHandler, NotFoundException e demais exceções de domínio
```

Nenhuma classe de regra de negócio fora desses pacotes. Novo pacote só se
justificar um novo agregado (ex.: se o catálogo crescer para múltiplos
agregados além de `book`).

## Camadas e responsabilidades

- **Controller**: faz bind de request/response e nada além disso. Não
  contém regra de negócio, não acessa `Repository` diretamente, não decide
  defaults de campo.
- **Service**: contém toda a regra de negócio — validação cruzada (ex.:
  `endDate >= startDate`), defaults (ex.: `status = QUERO_LER` na
  criação), e a aplicação do isolamento por usuário. O `userId` usado em
  qualquer consulta/gravação **sempre** vem do contexto de segurança
  (usuário autenticado do token), nunca de um campo recebido no corpo ou
  query da requisição.
- **Repository**: `JpaRepository` + `JpaSpecificationExecutor`, sem lógica
  de negócio além de queries.
- **DTOs** de request/response nunca expõem a entidade JPA diretamente;
  sempre há um mapeamento explícito entidade ↔ DTO.

## Modelagem

- IDs sempre `UUID`, gerados na aplicação (`UUID.randomUUID()`), nunca
  auto-increment.
- Enums persistidos como `STRING` (`@Enumerated(EnumType.STRING)`), nunca
  `ORDINAL`.
- Toda tabela tem `created_at`; tabelas mutáveis (ex.: `books`) têm também
  `updated_at`, atualizado em toda operação de `update`.
- Migração de schema sempre via Flyway
  (`src/main/resources/db/migration/V{n}__descricao.sql`); nunca usar
  `spring.jpa.hibernate.ddl-auto` além de `validate`.

## Validação e tratamento de erros

- Validação de entrada via Bean Validation (`jakarta.validation`) nos
  DTOs de request.
- Toda exceção de negócio é mapeada para `ProblemDetail` (RFC 7807) através
  do `GlobalExceptionHandler` central — nunca deixar uma exceção não
  tratada vazar stack trace para o cliente.
- Acesso a um recurso de outro usuário retorna `404`, nunca `403` — não
  revelar a existência do recurso para quem não é o dono.

## Testes

- Todo endpoint novo exige pelo menos um teste de integração cobrindo cada
  critério de aceite listado no arquivo correspondente em `docs/tasks/`.
- Testes de integração usam SQLite real (arquivo temporário via
  `@TempDir`, migrado via Flyway), não H2 — o dialeto SQLite tem
  particularidades (ex.: ausência de `ILIKE`) que um banco diferente não
  reproduz.
- Teste unitário (Mockito, sem contexto Spring) para regra de negócio que
  não depende de I/O (ex.: validação cruzada de datas).

## Nomenclatura

- Classes: `PascalCase`. Métodos e variáveis: `camelCase`. Colunas de
  banco: `snake_case`.
- `Specification` helpers nomeados como predicado (`hasStatus`,
  `matchesGenre`, `hasRatingBetween`), agrupados em uma classe
  `BookSpecifications`.
- DTOs de request terminam em `Request` (`BookCreateRequest`,
  `BookUpdateRequest`); DTOs de resposta terminam em `Response`
  (`BookResponse`, `TokenResponse`).
