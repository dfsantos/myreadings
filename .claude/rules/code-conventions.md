# Convenções de Código

Aplica-se a todo código em `src/`. Reflete as decisões já tomadas em
`docs/plans/catalogo-de-leituras-backend.md` e, para a estrutura de
pacotes e a convenção de exception handler por módulo, em
`docs/plans/refatoracao-modularizacao-vertical-slice.md` — em caso de
divergência entre este documento e um plano técnico, o plano técnico
descreve o *o quê*, este documento descreve *como escrever* o código que
o implementa.

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
├── config/     # SecurityConfig, OpenApiConfig
├── security/   # CurrentUser, JwtTokenProvider, JwtAuthenticationFilter,
              # JwtAuthenticationEntryPoint, SecurityExceptionHandler
├── user/       # User (entidade), UserRepository, AuthController, AuthService,
              # EmailAlreadyInUseException, UserExceptionHandler, dto/
├── book/       # Book (entidade), ReadingStatus, BookRepository, BookService,
              # BookController, BookSearchCriteria, BookSpecifications,
              # InvalidDateRangeException, BookExceptionHandler, dto/
└── common/     # GlobalExceptionHandler, NotFoundException, InstantStringConverter,
              # LocalDateStringConverter
```

Não existe mais um pacote `auth/` — foi fundido em `user/` (ver
`docs/plans/refatoracao-modularizacao-vertical-slice.md`). `security/` é
infraestrutura compartilhada de autenticação/autorização (não um módulo
de negócio); `user` e `book` são os dois agregados de negócio hoje
existentes. Nenhuma classe de regra de negócio fora desses pacotes. Novo
pacote de negócio só se justificar um novo agregado (ex.: se o catálogo
crescer para múltiplos agregados além de `book`).

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
- Migração de schema sempre via Flyway, segmentada por módulo em
  `src/main/resources/db/migration/<módulo>/V{n}__descricao.sql` (ex.:
  `db/migration/user/V1__create_users_table.sql`,
  `db/migration/book/V2__create_books_table.sql`), com os diretórios
  listados explicitamente em `spring.flyway.locations`; nunca usar
  `spring.jpa.hibernate.ddl-auto` além de `validate`. A numeração de
  versão (`V{n}`) é única no projeto como um todo, não por diretório — o
  Flyway resolve a ordem pelo número, não pela pasta.

## Validação e tratamento de erros

- Validação de entrada via Bean Validation (`jakarta.validation`) nos
  DTOs de request.
- Toda exceção de negócio é mapeada para `ProblemDetail` (RFC 7807) através
  de um `@RestControllerAdvice` — nunca deixar uma exceção não tratada
  vazar stack trace para o cliente.
- Acesso a um recurso de outro usuário retorna `404`, nunca `403` — não
  revelar a existência do recurso para quem não é o dono.

### Exception handler por módulo

Desde a refatoração de modularização
(`docs/plans/refatoracao-modularizacao-vertical-slice.md`), o tratamento
de exceções é dividido em três níveis, nunca concentrado num único
`@RestControllerAdvice`:

- **Módulo de negócio** (`user`, `book`): cada módulo trata suas próprias
  exceções de domínio no seu próprio advice —
  `user/UserExceptionHandler.java`
  (`@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.user")`)
  e `book/BookExceptionHandler.java`
  (`@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.book")`).
  O `basePackages` é seguro porque o Spring resolve o advice aplicável
  pelo pacote do **controller** que atende a requisição, não pelo pacote
  de onde a exceção foi lançada — e, em ambos os casos hoje, controller e
  exceção estão no mesmo módulo. Uma exceção nova de `user`/`book` entra
  no advice do próprio módulo, nunca em `common`.
- **`common/GlobalExceptionHandler`**: cobre só erros genéricos de
  framework, sem conhecimento de domínio e sem `basePackages` (erro de
  bind/validação de request, `DataIntegrityViolationException`,
  `NotFoundException` genérico) — é o único advice que deve continuar
  agnóstico de módulo.
- **`security/SecurityExceptionHandler`**: infraestrutura cross-cutting
  compartilhada (erro de parse de JWT fora do filtro de autenticação).
  Também **sem** `basePackages`, porque qualquer módulo futuro pode
  precisar validar um token — não é lógica de um módulo de negócio
  específico, mesmo estando fisicamente no pacote `security`.

Ao criar uma exceção nova de regra de negócio, ela nasce no pacote do
módulo que a lança (nunca em `common`) e seu handler correspondente
nasce no `@RestControllerAdvice` daquele mesmo módulo.

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
