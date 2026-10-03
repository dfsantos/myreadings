# myreadings

Backend do Catálogo de Leituras pessoal. API REST que permite a um usuário
autenticado manter sua própria lista de livros (status de leitura, datas,
avaliação, busca/filtros), isolada da lista de outros usuários.

Documentação completa:
- Spec/PRD: [`docs/specs/catalogo-de-leituras-backend.md`](docs/specs/catalogo-de-leituras-backend.md)
- Plano técnico: [`docs/plans/catalogo-de-leituras-backend.md`](docs/plans/catalogo-de-leituras-backend.md)
- Tasks por história de usuário: [`docs/tasks/`](docs/tasks/)
- Dashboard de acompanhamento: [`docs/dashboard.html`](docs/dashboard.html)
- Convenção de commits: [`.claude/rules/commit-conventions.md`](.claude/rules/commit-conventions.md)
- Convenção de código: [`.claude/rules/code-conventions.md`](.claude/rules/code-conventions.md)

## Stack confirmada

- **Linguagem/runtime:** Java 25 (toolchain Gradle)
- **Framework:** Spring Boot 4.1.1
- **Persistência:** Spring Data JPA + Hibernate 7, SQLite via
  `org.xerial:sqlite-jdbc` + `org.hibernate.orm:hibernate-community-dialects`
  (`SQLiteDialect`, não incluso no Hibernate core)
- **Migração de schema:** Flyway (`flyway-core` + `spring-boot-starter-flyway`),
  `spring.jpa.hibernate.ddl-auto: validate` — Flyway é a fonte da verdade,
  nunca `ddl-auto` fora de `validate`
- **Web:** Spring Web MVC (REST), prefixo `/api/v1`
- **Autenticação:** Spring Security + JWT stateless (`io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson`,
  versão fixada `0.12.6`), sem sessão/cookie, sem OAuth de terceiro na v1
- **Validação:** Bean Validation (`spring-boot-starter-validation`) nos DTOs de request
- **Observabilidade:** Spring Boot Actuator (health check mínimo)
- **Build:** Gradle (wrapper já presente); hook de commit-msg instalado
  automaticamente via `core.hooksPath` em `settings.gradle`
- **Jackson:** Spring Boot 4.1.1 usa Jackson 3 (`tools.jackson.databind`),
  não o pacote clássico `com.fasterxml.jackson.databind` — vale tanto para
  teste (que inspeciona `JsonNode` via `asString()`/`asLong()`, não
  `asText()` deprecated) quanto para código de produção: o
  `GlobalExceptionHandler` precisa capturar
  `tools.jackson.databind.exc.InvalidFormatException` (não a classe
  homônima de `com.fasterxml.jackson`) para tratar erro de deserialização
  de enum (ex.: `status` fora de `ReadingStatus` no `PATCH /books/{id}`,
  US-07) — ver seção 6.4 do plano técnico.
- **Spring Data Web (Boot 4.1.1):** a classe que declara
  `spring.data.web.pageable.max-page-size` foi relocada de
  `org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties`
  para `org.springframework.boot.data.autoconfigure.web.DataWebProperties`
  (módulo `spring-boot-data-commons`, parte da modularização do Boot 4) —
  o prefixo YAML `spring.data.web.pageable.*` não muda. Mesmo padrão das
  notas de Jackson 3/Hibernate 7 acima: vale atenção equivalente para
  qualquer outra propriedade `spring.data.web.*` configurada no futuro.

## Convenções de pacote/estrutura

```
dev.dfsantos.myreadings
├── config/   # SecurityConfig, OpenApiConfig
├── auth/     # AuthController, AuthService, JwtTokenProvider, JwtAuthenticationFilter, dto/
├── user/     # User (entidade), UserRepository
├── book/     # Book (entidade), ReadingStatus, BookRepository, BookService, BookController,
             # BookSearchCriteria, dto/
└── common/   # GlobalExceptionHandler, NotFoundException e demais exceções de domínio
```

Detalhes de camadas, nomenclatura, modelagem e testes em
[`.claude/rules/code-conventions.md`](.claude/rules/code-conventions.md) —
resumo das decisões mais relevantes:

- Controller só faz bind de request/response; regra de negócio fica no
  Service; `userId` usado em qualquer query/gravação sempre vem do
  contexto de segurança (usuário autenticado), nunca de campo recebido na
  requisição.
- IDs sempre `UUID` gerados na aplicação, nunca auto-increment. Enums
  persistidos como `STRING`. Toda tabela tem `created_at`; tabelas
  mutáveis (`books`) têm também `updated_at`.
- Acesso a recurso de outro usuário retorna `404`, nunca `403`.
- `Instant` (Java) ↔ `TEXT` ISO-8601 (coluna SQLite) mapeado via
  `common/InstantStringConverter` (`AttributeConverter<Instant, String>`),
  usado em `User.createdAt` e reutilizável em `Book.createdAt`/`updatedAt`.
- Testes de integração usam SQLite real (arquivo temporário via
  `@TempDir`, migrado via Flyway), nunca H2.
- `LocalDate` (Java) ↔ `TEXT` ISO-8601 (coluna SQLite) mapeado via
  `common/LocalDateStringConverter` (`AttributeConverter<LocalDate, String>`),
  usado em `Book.startDate`/`Book.endDate` — análogo ao
  `InstantStringConverter`: o `LocalDateJavaType` do Hibernate 7 não faz
  unwrap direto para `String` ao persistir em coluna `TEXT`, mesmo com
  `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- `Book` é uma entidade sem setters (só construtor completo + getters):
  `BookService.update` (US-07) faz merge parcial do `BookUpdateRequest`
  montando uma **nova instância** via construtor, copiando do registro
  existente todo campo não enviado no PATCH, em vez de mutar a entidade
  gerenciada. Como o `id` é um `UUID` atribuído pela aplicação (não
  `@GeneratedValue`), `JpaRepository.save` sempre executa o merge do
  Hibernate (insert ou update) normalmente. A US-08 seguiu o mesmo padrão
  ao acrescentar a validação cruzada de datas; próximas histórias que
  também atualizem `Book` (US-09+) devem seguir o mesmo padrão.

## Decisões técnicas vigentes (espelhando docs/plans/)

- Mecanismo de autenticação: JWT stateless, claim `sub = user.id`, `exp`
  configurável (`app.jwt.expiration-minutes`, default `1440`).
- Gênero de livro: texto livre (não enum fixo).
- Avaliação: inteiro de 1 a 5, nulo até o livro ser avaliado.
- SQLite é definitivo para a v1 (não é só banco de dev).
- No dialeto `SQLiteDialect`, violação de `UNIQUE` **não** é traduzida
  para `DataIntegrityViolationException` pelo Spring/Hibernate — checagem
  de duplicidade (ex.: e-mail) precisa ser feita explicitamente no Service
  antes do `INSERT`; o handler de `DataIntegrityViolationException`
  permanece como defesa em profundidade contra condição de corrida.
- `SecurityFilterChain` construído incrementalmente: a US-01 criou a
  versão mínima (rotas públicas de auth + health liberadas, resto exige
  autenticação); a US-02 emitiu o token sem alterar o filter chain; a
  US-03 completou o fluxo: `SessionCreationPolicy.STATELESS`,
  `JwtAuthenticationFilter` (popula `SecurityContext` com `userId`) e
  `JwtAuthenticationEntryPoint` (401 único para token ausente/inválido).
- `JwtTokenProvider` força `Jwts.SIG.HS256` explicitamente no
  `signWith` (em vez de deixar o jjwt inferir o algoritmo pelo tamanho da
  chave) — ver nota de implementação na seção 6.1 do plano técnico.
- Erro de autenticação (token ausente/inválido/expirado) é respondido por
  um `AuthenticationEntryPoint` dedicado (`JwtAuthenticationEntryPoint`),
  não pelo `GlobalExceptionHandler` — esse ponto de extensão do Spring
  Security roda antes do `DispatcherServlet`, onde um
  `@RestControllerAdvice` não teria efeito. O handler de `JwtException`
  no `GlobalExceptionHandler` continua existindo como defesa em
  profundidade para um parse de token fora do filtro (ex.: um futuro
  endpoint de refresh), não é o caminho usado hoje.

## Estado atual

Histórias concluídas (todas as tarefas e critérios de aceite verificados
no código e cobertos por teste):

- [US-01 — Criar conta](docs/tasks/01-criar-conta.md): `POST /api/v1/auth/register`,
  entidade `User`, `UserRepository`, `SecurityConfig` (bean `PasswordEncoder`
  + filter chain mínimo), `AuthService`/`AuthController`,
  `GlobalExceptionHandler`, migração `V1__create_users_table.sql`.
- [US-02 — Fazer login](docs/tasks/02-fazer-login.md): `POST /api/v1/auth/login`,
  `JwtTokenProvider` (JWT HS256, claim `sub = user.id`, `exp` configurável
  via `app.jwt.expiration-minutes`), `AuthService.login`,
  `BadCredentialsException` → `401` no `GlobalExceptionHandler`.
- [US-03 — Isolamento automático por usuário](docs/tasks/03-isolamento-por-usuario.md):
  `JwtAuthenticationFilter` (popula `SecurityContext` com o `userId` do
  token), `JwtAuthenticationEntryPoint` (401 único para "sem token" e
  "token inválido/expirado"), `common/CurrentUser.id()` (helper estático
  de leitura do usuário autenticado), `SecurityFilterChain` agora
  `STATELESS` com o filtro registrado. O terceiro critério de aceite desta
  história (404 em livro de outro usuário) só se tornou observável na
  US-07 (primeiro endpoint de edição por id) — ver "Observações/Pendências"
  em [`docs/tasks/03-isolamento-por-usuario.md`](docs/tasks/03-isolamento-por-usuario.md).
- [US-04 — Cadastrar livro com dados básicos e capa](docs/tasks/04-cadastrar-livro-dados-basicos.md):
  migração `V2__create_books_table.sql`, enum `ReadingStatus`, entidade
  `Book`, `BookRepository`, `BookCreateRequest`/`BookResponse`,
  `BookService.create` (default `status = QUERO_LER`, `userId` via
  `CurrentUser.id()`), `BookController` (`POST /api/v1/books` → `201`).
- [US-05 — Cadastrar livro sem preencher campos opcionais](docs/tasks/05-cadastrar-livro-campos-opcionais.md):
  nenhuma mudança de código — confirmação de que `BookCreateRequest` só
  tem `@NotBlank` em `title`/`author` e de que `BookService.create` já
  aplicava o default `status = QUERO_LER` (implementado na US-04),
  complementada por teste de integração cobrindo omissão de todos os
  opcionais e envio apenas de `coverUrl` entre os opcionais.
- [US-06 — Rejeitar cadastro com campo obrigatório ausente](docs/tasks/06-validacao-campo-obrigatorio-ausente.md):
  `GlobalExceptionHandler.handleValidation` (`MethodArgumentNotValidException`
  → `400`) populando `ProblemDetail` com extension property `errors`
  (array de `{field, message}`, um por campo inválido) além do `detail`
  textual de resumo — ver "Formato do erro de validação" abaixo.
  `@NotBlank` em `title`/`author` já existia desde a US-04.
- [US-07 — Marcar status de leitura](docs/tasks/07-marcar-status-leitura.md):
  DTO `BookUpdateRequest` (todos os campos opcionais, incluindo `status`),
  `BookRepository.findByIdAndUserId`, `BookService.update` (merge parcial
  via nova instância imutável de `Book`, `404` via `NotFoundException` se
  o livro não existir ou não pertencer ao usuário autenticado),
  `BookController.update` (`PATCH /api/v1/books/{id}` → `200`),
  `GlobalExceptionHandler.handleMessageNotReadable` (`400` para `status`
  fora do enum `ReadingStatus`, via
  `tools.jackson.databind.exc.InvalidFormatException`). Esta história
  também torna observável — e fecha — o terceiro critério de aceite da
  US-03 (404 em livro de outro usuário), via o teste
  `patchBookOfAnotherUserReturns404`.
- [US-08 — Registrar datas de início e término da leitura](docs/tasks/08-registrar-datas-inicio-fim.md):
  `common/InvalidDateRangeException`, `BookService.validateDateRange`
  chamado em `create` e em `update` com `endDate >= startDate`, mapeado
  para `400` em `GlobalExceptionHandler.handleInvalidDateRange`.
  Convenção importante: em `update`, a validação usa o **estado final já
  mesclado** (`startDate`/`endDate` após aplicar o `BookUpdateRequest`
  sobre o registro existente), não os campos isolados do corpo do PATCH —
  necessário para capturar o caso em que o cliente envia só uma das datas
  e ela conflita com o valor já persistido na outra. Vale para qualquer
  validação cruzada futura sobre merge parcial (ver seção 6.3 do plano
  técnico).
- [US-09 — Dar nota (avaliação) a um livro](docs/tasks/09-avaliar-livro.md):
  nenhuma mudança de código — `rating` (`Integer`, `@Min(1)` `@Max(5)`) já
  existia em `BookCreateRequest`/`BookUpdateRequest` e a coluna `rating` em
  `V2__create_books_table.sql` já tinha o `CHECK` correspondente desde a
  US-04, complementada por testes de integração cobrindo `rating = 5`
  (persistido), `rating = 6` e `rating = 0` (ambos `400`).
- [US-10 — Atualizar status/datas/nota sem recriar o livro](docs/tasks/10-atualizar-livro-sem-recriar.md):
  nenhuma mudança em `src/main` — confirmação de que `BookService.update`
  (US-07) já fazia merge genuinamente parcial campo a campo e já avançava
  `updatedAt` incondicionalmente a cada chamada, mesmo sem nenhuma
  diferença de negócio (inclusive `PATCH` com corpo vazio `{}` — decisão
  explicitada na seção 11 do plano técnico). Complementada por 3 testes de
  integração novos em `BookControllerTest`: PATCHs sequenciais acumulando
  estado (status → rating → datas), PATCH só com `rating` preservando os
  demais campos, PATCH vazio retornando `200` sem mudança de negócio mas
  com `updatedAt` avançado. **Esta história encerra o grupo "Acompanhamento
  da leitura" (US-07 a US-10, todas concluídas).**

**Formato do erro de validação (vigente desde a US-06):** erros de Bean
Validation em qualquer DTO de request são respondidos como `ProblemDetail`
`400` com `problemDetail.setProperty("errors", List<FieldErrorDetail>)`,
cada item `{field, message}`, serializado pelo Jackson 3 em
`$.errors[].field`/`$.errors[].message`. Não criar um DTO de erro
customizado substituindo o `ProblemDetail` — este é o padrão a seguir em
validações futuras (ex.: US-15, `minRating`/`maxRating`). Detalhe completo
na seção 6.4 do plano técnico.

**Convenção obrigatória para US-04 em diante:** `BookService` (e qualquer
Service futuro que precise do usuário autenticado) deve chamar
`CurrentUser.id()` (`common/CurrentUser.java`) diretamente — nunca receber
`userId` como parâmetro de controller, DTO de request ou query string.
Controllers continuam sem qualquer lógica de obtenção de usuário; essa é a
decisão arquitetural tomada na US-03 e vale para todo o restante do
catálogo de livros.

**Convenção obrigatória para US-11 em diante (grupo "Consulta e busca"):**
qualquer predicado novo de filtro de `Book` entra em
`book/BookSpecifications.java` como método estático nomeado (ex.:
`hasStatus`, `matchesGenre`, `hasRatingGreaterThanOrEqualTo`), nunca
inline em `BookService.search`. Os predicados são combinados via
`Specification.and(...)`; a `Specification` base `hasUserId(userId)` é
sempre aplicada, independente dos demais filtros presentes em
`BookSearchCriteria`.

**Padrão obrigatório para validar `@RequestParam` individual com Bean
Validation (vigente desde a US-15):** Bean Validation (`@Min`/`@Max` etc.)
não funciona em um campo de um record bindado implicitamente como
parâmetro de método de controller (ex.: um campo de `BookSearchCriteria`)
— só funciona em um parâmetro de método anotado diretamente
(`@RequestParam(...) @Min(1) Integer x`), e isso exige a classe do
controller anotada com `@Validated`
(`org.springframework.validation.annotation.Validated`). Quando um campo
de `BookSearchCriteria` precisa de validação Bean Validation própria (não
apenas regra de negócio no Service), ele deve ser recebido como
`@RequestParam` individual no controller — não como parte do record
bindado implicitamente — e composto manualmente num `BookSearchCriteria`
completo dentro do método. A violação chega como
`ConstraintViolationException` (`jakarta.validation`), não como
`MethodArgumentNotValidException` (que só cobre `@Valid` em corpo de
request) — exige handler próprio
(`GlobalExceptionHandler.handleConstraintViolation`). Ver seção 6.3/6.4 do
plano técnico para o detalhe completo; esta é a convenção a seguir pela
US-16 (combinar filtros) e qualquer validação futura de query param.

- [US-11 — Listar todos os livros do catálogo](docs/tasks/11-listar-livros.md):
  `BookSearchCriteria` (record vazio, estendido por US-12 a US-16),
  `BookSpecifications` (classe de predicados nomeados; hoje só
  `hasUserId`), `BookService.search` (`BookRepository.findAll(Specification, Pageable)`,
  sempre filtrando por `userId` do contexto de segurança),
  `BookController.list` (`GET /api/v1/books` retornando `Page<BookResponse>`,
  via `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`),
  `spring.data.web.pageable.max-page-size: 100` em `application.yaml`.
  **Esta história abre o grupo "Consulta e busca" (US-11 a US-16).**
- [US-12 — Buscar por título ou autor (texto livre)](docs/tasks/12-buscar-por-titulo-autor.md):
  campo `q` em `BookSearchCriteria`, `BookSpecifications.matchesSearchTerm`
  (`LOWER(title) LIKE %termo% OR LOWER(author) LIKE %termo%`, termo
  normalizado em minúsculas no lado Java antes de montar o `LIKE`),
  combinado em `BookService.search` via `.and(...)` apenas quando
  `criteria.q()` não é nulo/blank. Nenhuma mudança em `BookController`
  (bind do novo campo do record já funciona automaticamente via
  `BookSearchCriteria`). O plano técnico previa `@Query` no
  `BookRepository` como mecanismo esperado (seção 10); a implementação
  seguiu o padrão `Specification`/`BookSpecifications` já estabelecido
  pela US-11, e a seção 10 do plano foi corrigida para refletir isso.

- [US-13 — Filtrar por status de leitura](docs/tasks/13-filtrar-por-status.md):
  campo `status` (`ReadingStatus`) em `BookSearchCriteria`,
  `BookSpecifications.hasStatus`, combinado em `BookService.search` via
  `.and(...)` apenas quando `criteria.status()` não é nulo. Nenhuma mudança
  em `BookController` (bind do novo campo do record, incluindo conversão de
  enum, já funciona automaticamente). Novo handler
  `GlobalExceptionHandler.handleMethodArgumentTypeMismatch`
  (`MethodArgumentTypeMismatchException` → `400`) para valor de `status`
  fora do enum na query string — mecanismo diferente do usado para o mesmo
  tipo de erro no corpo do `PATCH` (`HttpMessageNotReadableException`/
  `InvalidFormatException`, US-07): erro de conversão em query param e erro
  de conversão em corpo de requisição chegam ao Spring por caminhos
  distintos, cada um com seu próprio handler reaproveitando o mesmo formato
  de mensagem (valor inválido + valores aceitos do enum).

- [US-14 — Filtrar por gênero/categoria](docs/tasks/14-filtrar-por-genero.md):
  campo `genre` (`String`) em `BookSearchCriteria`,
  `BookSpecifications.matchesGenre` (`LOWER(genre) = LOWER(:genre)` —
  igualdade exata, diferente de `matchesSearchTerm`, que é substring),
  combinado em `BookService.search` via `.and(...)` apenas quando
  `criteria.genre()` não é nulo/blank. Nenhuma mudança em `BookController`
  (bind do novo campo do record já funciona automaticamente, mesmo padrão
  da US-12/US-13).

- [US-15 — Filtrar por faixa de avaliação](docs/tasks/15-filtrar-por-faixa-avaliacao.md):
  campos `minRating`/`maxRating` (`Integer`) em `BookSearchCriteria`,
  `BookSpecifications.hasRatingGreaterThanOrEqualTo`/`hasRatingLessThanOrEqualTo`
  (dois predicados separados, não um `hasRatingBetween` único — mantém a
  simetria "um campo, combinado condicionalmente" dos demais predicados),
  combinados em `BookService.search` via `.and(...)` independentemente
  quando presentes. **Primeira validação de `@RequestParam` individual do
  projeto** (ver padrão destacado acima): `BookController` agora é
  `@Validated`, e `minRating`/`maxRating` chegam como
  `@RequestParam(required = false) @Min(1) @Max(5) Integer`, separados do
  bind implícito de `BookSearchCriteria` (que continua cuidando de
  `q`/`status`/`genre`); o controller recompõe um `BookSearchCriteria`
  completo antes de chamar `BookService.search`. Novo handler
  `GlobalExceptionHandler.handleConstraintViolation`
  (`jakarta.validation.ConstraintViolationException` → `400`, mesmo
  formato `errors: [{field, message}]`).

- [US-16 — Combinar múltiplos filtros](docs/tasks/16-combinar-filtros.md):
  nenhuma mudança de código — confirmação de que `BookService.search` já
  compunha `hasUserId` (base) com `.and(...)` condicional para cada filtro
  presente em `BookSearchCriteria` (texto, status, gênero, rating), sem
  nenhum `.or()` ou `.and()` incondicional entre eles, e de que
  `BookController.list` já montava um `BookSearchCriteria` completo
  combinando o bind implícito (`q`/`status`/`genre`) com `minRating`/
  `maxRating` validados (US-15). Complementada por 3 testes de integração
  novos em `BookControllerTest`: combinação `genre`+`status` distinguindo
  AND de OR, combinação de 4 filtros simultâneos, e ausência de filtro
  comportando-se como a listagem simples (US-11). **Esta história encerra
  o grupo "Consulta e busca" (US-11 a US-16, todas as 6 concluídas).**

- [US-17 — Lista vazia quando nenhum livro corresponde à busca](docs/tasks/17-busca-sem-resultados.md):
  nenhuma mudança de código — confirmação de que `BookService.search`
  (via `bookRepository.findAll(specification, pageable)`) já retorna uma
  `Page` vazia nativamente (comportamento padrão do Spring Data) quando a
  `Specification` não encontra correspondência, sem produzir `404`/`500`.
  Mesmo padrão de "nenhuma mudança de código de produção necessária" já
  observado em US-05/US-09/US-10/US-16. Complementada por 2 testes de
  integração novos em `BookControllerTest`: `q` sem correspondência e
  combinação de filtros válidos (`status`+`minRating`) sem nenhum livro
  correspondente, ambos retornando `200` com `content: []`/
  `totalElements: 0`.

- [US-18 — Remover livro](docs/tasks/18-remover-livro.md):
  `BookService.delete` (busca via `findByIdAndUserId`, `404` via
  `NotFoundException` se não encontrado/não é do usuário, remove via
  `BookRepository`), `BookController.delete` (`DELETE /api/v1/books/{id}` →
  `204`). Esta história também implementou `GET /api/v1/books/{id}`
  (`BookService.findById` + `BookController.get` → `200`), endpoint já
  previsto na tabela de rotas da seção 6.2 do plano técnico mas que não
  tinha tarefa própria em nenhuma história do backlog — foi necessário
  porque o critério de aceite "GET subsequente ao id removido retorna
  404" depende dele; detalhe em "Observações" de
  [`docs/tasks/18-remover-livro.md`](docs/tasks/18-remover-livro.md).
  Coberta por 8 testes de integração novos em `BookControllerTest`. **Esta
  história encerra o backlog completo (US-01 a US-18, todas as 18
  histórias concluídas).**

Todo o backlog de histórias de usuário (US-01 a US-18) está implementado —
ver [`docs/dashboard.html`](docs/dashboard.html) para o progresso agregado
e [`docs/tasks/`](docs/tasks/) para o detalhe de cada uma.
