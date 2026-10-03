# Plano Técnico — Backend do Catálogo de Leituras (myreadings)

**Status:** Rascunho v1
**Data:** 2026-10-02
**Spec de origem:** [`docs/specs/catalogo-de-leituras-backend.md`](../specs/catalogo-de-leituras-backend.md)

Este documento traduz a spec de produto em decisões técnicas concretas e suficientes para implementar o backend. Onde a spec deixou uma "Open Question", este documento toma a decisão e justifica.

## 1. Decisões que resolvem as Open Questions da spec

| Questão aberta na spec | Decisão | Justificativa |
|---|---|---|
| Mecanismo de autenticação | **JWT stateless** (sem sessão/cookie, sem OAuth de terceiro na v1) | API sem frontend definido ainda; JWT evita estado de sessão no servidor e é trivial de testar via `curl`/Postman. OAuth de terceiro fica como P2 se um dia houver login social. |
| Gênero fixo vs texto livre | **Texto livre** (`String`, até 100 caracteres) | Evita manter uma taxonomia e migração de enum toda vez que surgir um gênero novo. Filtro por gênero compara string normalizada (case-insensitive, trim). Normalização em tabela própria fica como P2 caso se queira agregações mais ricas. |
| Faixa de avaliação | **Inteiro de 1 a 5**, nulo até o livro ser avaliado | Granularidade de "estrelas" é suficiente para uso pessoal; decimais adicionariam complexidade de UI sem ganho claro. |
| SQLite definitivo ou só dev | **Definitivo para v1** | Uso single-user por instância, baixa concorrência de escrita (um usuário escrevendo por vez já é mais que suficiente — SQLite lida bem com múltiplos usuários lendo e poucas escritas concorrentes). Revisitar se o projeto evoluir para multi-instância/deploy horizontal. |

## 2. Stack técnica

Já fixada em `build.gradle` — este plano assume e detalha o uso de cada peça:

- **Linguagem/runtime:** Java 25 (toolchain Gradle)
- **Framework:** Spring Boot 4.1.1
- **Persistência:** Spring Data JPA + Hibernate, SQLite via `org.xerial:sqlite-jdbc`
- **Web:** Spring Web MVC (REST)
- **Observabilidade:** Spring Boot Actuator (health check mínimo)
- **Build:** Gradle (wrapper já presente)

Dependências a **adicionar** (ainda não estão no `build.gradle`):

- `org.springframework.boot:spring-boot-starter-security` — autenticação/autorização
- `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` (ou `com.nimbusds:nimbus-jose-jwt`) — geração/validação de JWT
- `org.flywaydb:flyway-core` — versionamento de schema (ver seção 5)
- `org.springdoc:springdoc-openapi-starter-webmvc-ui` (opcional, P1) — documentação OpenAPI/Swagger automática

Na implementação da US-01, três dependências adicionais além das acima
listadas se mostraram necessárias para a combinação Spring Boot 4.1.1 +
Hibernate 7 + SQLite + Flyway + Bean Validation funcionar:
`spring-boot-starter-validation` (Bean Validation nos DTOs, seção 6.3),
`spring-boot-starter-flyway` (autoconfiguração do Flyway, complementa
`flyway-core`) e `org.hibernate.orm:hibernate-community-dialects` em
`runtimeOnly` (fornece o `SQLiteDialect`, não incluso no Hibernate core).

## 3. Estrutura de pacotes

```
dev.dfsantos.myreadings
├── MyreadingsApplication.java
├── config/
│   ├── SecurityConfig.java        # filter chain, CORS, política de senha
│   └── OpenApiConfig.java         # (opcional, P1)
├── auth/
│   ├── AuthController.java        # /api/v1/auth/**
│   ├── AuthService.java
│   ├── JwtTokenProvider.java      # emissão/validação de token
│   ├── JwtAuthenticationFilter.java
│   └── dto/
│       ├── RegisterRequest.java
│       ├── LoginRequest.java
│       └── TokenResponse.java
├── user/
│   ├── User.java                  # entidade JPA
│   └── UserRepository.java
├── book/
│   ├── Book.java                  # entidade JPA
│   ├── ReadingStatus.java         # enum
│   ├── BookRepository.java
│   ├── BookService.java
│   ├── BookController.java        # /api/v1/books/**
│   ├── BookSearchCriteria.java    # objeto de filtros (q, status, genre, minRating, maxRating)
│   └── dto/
│       ├── BookCreateRequest.java
│       ├── BookUpdateRequest.java
│       └── BookResponse.java
└── common/
    ├── GlobalExceptionHandler.java  # @RestControllerAdvice -> ProblemDetail
    ├── NotFoundException.java
    └── PageResponse.java            # (se não usar Page<T> do Spring direto)
```

## 4. Modelo de dados

### 4.1 `users`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | TEXT (UUID) | PK |
| `email` | TEXT | UNIQUE, NOT NULL |
| `password_hash` | TEXT | NOT NULL |
| `created_at` | TEXT (ISO-8601) | NOT NULL |

- `id` gerado como UUIDv4 na aplicação (`UUID.randomUUID()`), não auto-increment — evita expor contagem de usuários e facilita eventual migração/merge de bancos.
- Senha armazenada com `BCryptPasswordEncoder` (custo padrão 10).
- Mapeamento `Instant` (Java) ↔ `TEXT` ISO-8601 (coluna) feito via
  `common/InstantStringConverter` (`AttributeConverter<Instant, String>`),
  usado em `created_at` de `User` e reutilizável em `created_at`/`updated_at`
  de `Book` — o mapeamento padrão do Hibernate para `Instant` espera uma
  coluna `TIMESTAMP`, que o SQLite não tem.

Nota de implementação (US-04): pelo mesmo motivo acima, `LocalDate` (Java)
precisou de um conversor análogo, `common/LocalDateStringConverter`
(`AttributeConverter<LocalDate, String>`) — o `LocalDateJavaType` do
Hibernate 7 não faz unwrap direto para `String` ao persistir em coluna
`TEXT` do SQLite, mesmo com `@JdbcTypeCode(SqlTypes.VARCHAR)` anotado
diretamente no campo. Usado em `Book.startDate`/`Book.endDate` (seção
4.2).

### 4.2 `books`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | TEXT (UUID) | PK |
| `user_id` | TEXT (UUID) | FK → `users.id`, NOT NULL |
| `title` | TEXT | NOT NULL, max 255 |
| `author` | TEXT | NOT NULL, max 255 |
| `publisher` | TEXT | NULL, max 255 |
| `publication_year` | INTEGER | NULL |
| `page_count` | INTEGER | NULL, CHECK (`page_count IS NULL OR page_count > 0`) |
| `genre` | TEXT | NULL, max 100 |
| `cover_url` | TEXT | NULL, max 2048 |
| `status` | TEXT | NOT NULL, DEFAULT `'QUERO_LER'`, um de `ReadingStatus` |
| `start_date` | TEXT (ISO-8601 date) | NULL |
| `end_date` | TEXT (ISO-8601 date) | NULL |
| `rating` | INTEGER | NULL, CHECK (`rating IS NULL OR rating BETWEEN 1 AND 5`) |
| `created_at` | TEXT (ISO-8601) | NOT NULL |
| `updated_at` | TEXT (ISO-8601) | NOT NULL |

Índices:
- `idx_books_user_id` em `(user_id)` — toda consulta é escopada por usuário.
- `idx_books_user_status` em `(user_id, status)` — suporta filtro por status.
- `idx_books_user_genre` em `(user_id, genre)` — suporta filtro por gênero.

Busca textual (`título`/`autor`) usa `LIKE '%termo%'` case-insensitive (via `LOWER(...)` nas duas pontas, já que SQLite não tem `ILIKE` nativo) — volume esperado (uso pessoal, até alguns milhares de linhas) não justifica full-text search (FTS5) na v1; fica como nota para P2 se a base crescer muito.

### 4.3 `ReadingStatus` (enum)

```java
public enum ReadingStatus { QUERO_LER, LENDO, LIDO, ABANDONADO }
```

Persistido como `STRING` (`@Enumerated(EnumType.STRING)`), nunca `ORDINAL`, para não quebrar dados existentes se a ordem dos valores mudar.

Regra de validação cruzada: se `status` != `QUERO_LER`, `start_date` pode ser exigido; se `status` == `LIDO`, `end_date` é recomendado (não bloqueante em P0 — ver seção 6.3). `end_date`, quando presente, deve ser >= `start_date`.

## 5. Migrações de banco (Flyway)

Usar Flyway em vez de `ddl-auto` do Hibernate, para ter controle explícito e versionado do schema:

- `src/main/resources/db/migration/V1__create_users_table.sql`
- `src/main/resources/db/migration/V2__create_books_table.sql`

`application.yml`: `spring.jpa.hibernate.ddl-auto: validate` (Hibernate só valida que o schema bate com as entidades; Flyway é a fonte da verdade).

## 6. API REST

Prefixo de versão: `/api/v1`.

### 6.1 Autenticação (`/api/v1/auth`)

| Método | Rota | Request | Response | Status |
|---|---|---|---|---|
| POST | `/auth/register` | `{ "email": string, "password": string }` | `{ "id": uuid, "email": string }` | 201 / 409 (e-mail já existe) |
| POST | `/auth/login` | `{ "email": string, "password": string }` | `{ "accessToken": string, "expiresInSeconds": number }` | 200 / 401 |

- Senha mínima: 8 caracteres (validado via Bean Validation `@Size(min = 8)` no `RegisterRequest`).
- Token: JWT HS256, claim `sub` = `user.id`, `exp` configurável (`app.jwt.expiration-minutes`, default `1440` = 24h). Sem refresh token na v1 — usuário loga de novo ao expirar (P1: refresh token se o atrito for real).

Nota de implementação (US-02): `JwtTokenProvider` força `Jwts.SIG.HS256`
explicitamente em `signWith(key, Jwts.SIG.HS256)`, em vez de deixar o
jjwt 0.12.x inferir o algoritmo pelo tamanho da chave — sem isso, um
`app.jwt.secret` de produção com mais de 32 bytes faria a biblioteca
escolher HS384/HS512 silenciosamente.

### 6.2 Livros (`/api/v1/books`)

Todas as rotas abaixo exigem header `Authorization: Bearer <token>` e operam apenas sobre livros do usuário do token.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/books` | Cria um livro |
| GET | `/books/{id}` | Busca um livro por id |
| PATCH | `/books/{id}` | Atualiza parcialmente (campos omitidos/`null` no JSON não são alterados) |
| DELETE | `/books/{id}` | Remove um livro |
| GET | `/books` | Lista/busca com filtros e paginação |

**`GET /books` — query params:**

| Param | Tipo | Efeito |
|---|---|---|
| `q` | string | Busca em `title` OR `author`, case-insensitive, substring |
| `status` | `ReadingStatus` | Filtra por status exato |
| `genre` | string | Filtra por gênero exato (case-insensitive) |
| `minRating` / `maxRating` | int (1–5) | Filtra por faixa de avaliação (inclusive) |
| `page`, `size` | int | Paginação (default `page=0`, `size=20`, max `size=100`) |
| `sort` | string | Ex: `title,asc` / `endDate,desc` (default: `createdAt,desc`) |

Todos os filtros são combináveis com AND. Resposta usa o envelope padrão do Spring Data (`content`, `totalElements`, `totalPages`, `number`, `size`).

Nota de implementação (US-11) — `BookSpecifications` como ponto único de
predicados: `BookService.search` monta a `Specification<Book>` combinando
predicados nomeados definidos em `book/BookSpecifications.java` (ex.:
`hasUserId`), nunca inline no Service. A US-11 só implementa `hasUserId`
(aplicado sempre, independente dos demais filtros); as US-12
(`matchesSearchTerm`), US-13 (`hasStatus`) e US-14 (`matchesGenre`) já
seguiram esse padrão. A US-15 (faixa de avaliação) implementou **dois**
predicados separados — `hasRatingGreaterThanOrEqualTo(minRating)` e
`hasRatingLessThanOrEqualTo(maxRating)` — em vez de um único
`hasRatingBetween(min, max)` como este documento previa originalmente:
cada ponta da faixa é combinada independentemente em `BookService.search`
(`minRating`/`maxRating` podem chegar um sem o outro), mantendo a simetria
com os demais predicados desta classe ("um campo/condição, combinado
condicionalmente"). A US-16 (combinar múltiplos filtros) confirmou que
`BookService.search` já compunha todos os predicados existentes via
`.and(...)` condicional — nenhum ajuste de código foi necessário, só
testes de integração comprovando o comportamento AND; esse continua sendo
o único lugar para montar filtro de `Book`.

Nota de implementação (US-11) — `size` máximo via propriedade global, não
validação manual: o limite de `size=100` é configurado em
`application.yaml` (`spring.data.web.pageable.max-page-size: 100`), não
checado manualmente no Controller/Service. No Spring Boot 4.1.1 a classe
que declara essa propriedade foi relocada de
`org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties`
para `org.springframework.boot.data.autoconfigure.web.DataWebProperties`
(módulo `spring-boot-data-commons`, parte da modularização do Boot 4) — o
prefixo YAML `spring.data.web.pageable.*` não muda. Vale a mesma atenção
para qualquer outra propriedade `spring.data.web.*` configurada no futuro.

**Exemplo de `BookCreateRequest`:**

```json
{
  "title": "Duna",
  "author": "Frank Herbert",
  "publisher": "Aleph",
  "publicationYear": 1965,
  "pageCount": 688,
  "genre": "Ficção científica",
  "coverUrl": "https://exemplo.com/duna.jpg",
  "status": "LENDO",
  "startDate": "2026-09-01"
}
```

`status` é opcional no create — default `QUERO_LER` se omitido.

### 6.3 Validação

- Bean Validation (`jakarta.validation`) nos DTOs de request: `@NotBlank` em `title`/`author`, `@Size` em campos de texto, `@Min`/`@Max` em `rating` (1–5) e `pageCount` (>0).
- Validação cruzada `endDate >= startDate` feita manualmente no `BookService` (não é expressável de forma simples em Bean Validation sem um validador customizado) — lançar `400` com mensagem explícita se violada.
- `coverUrl`, quando presente, validado apenas como string não vazia em P0; checagem de formato de URL (`http`/`https`) fica como P1, conforme priorizado na spec.

Nota de implementação (US-15) — validação de `@RequestParam` individual
(primeira vez no projeto validando query param fora de um DTO de corpo):
Bean Validation (`@Min`/`@Max` etc.) não é aplicado a um campo de um
record bindado implicitamente como parâmetro de método de controller (ex.:
`BookSearchCriteria criteria` em `BookController.list`) — a anotação só
tem efeito quando colocada diretamente em um parâmetro de método anotado
individualmente (`@RequestParam`), e isso exige a classe do controller
anotada com `@Validated`
(`org.springframework.validation.annotation.Validated`). Por isso
`minRating`/`maxRating` chegam como `@RequestParam(required = false)
@Min(1) @Max(5) Integer` **separados** do bind implícito do record (que
continua cuidando de `q`/`status`/`genre`), e dentro do método um
`BookSearchCriteria` completo é reconstruído combinando os dois. A
violação cai como `ConstraintViolationException`
(`jakarta.validation`, lançada pelo `MethodValidationInterceptor` do
Spring para parâmetro de método), não como `MethodArgumentNotValidException`
(que só cobre `@Valid` em corpo de request) — exige handler próprio, ver
seção 6.4. **Convenção a seguir em histórias futuras:** quando um campo de
`BookSearchCriteria` precisar de validação Bean Validation própria (não
apenas regra de negócio no Service), ele deve ser recebido como
`@RequestParam` individual no controller — nunca como parte do record
bindado implicitamente — e depois composto manualmente no
`BookSearchCriteria` dentro do método.

Nota de implementação (US-08) — validação cruzada sobre merge parcial: em
`BookService.update`, `validateDateRange(startDate, endDate)` é chamado
com o **estado final já mesclado** (valor do `BookUpdateRequest` quando
presente, senão o valor já persistido em `existing`), nunca com os campos
isolados do corpo do PATCH. Um PATCH que envia só `startDate` (ou só
`endDate`) pode, combinado com o valor já persistido no outro campo,
formar um intervalo inválido — validar apenas os campos recebidos no
request deixaria esse caso passar. Esta é a convenção a seguir em
qualquer validação cruzada futura que dependa de mais de um campo em um
endpoint com atualização parcial (PATCH): sempre validar o objeto
resultante do merge, não o delta do request.

### 6.4 Tratamento de erros

Usar `ProblemDetail` (RFC 7807, nativo do Spring 6+) via `@RestControllerAdvice` central (`GlobalExceptionHandler`):

| Exceção | Status | `type`/`title` |
|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 400 | `validation-error`, detalha campo(s) inválido(s) |
| `NotFoundException` (livro não existe ou não pertence ao usuário) | 404 | `not-found` |
| `DataIntegrityViolationException` (backstop de condição de corrida no e-mail duplicado) | 409 | `conflict` |
| `EmailAlreadyInUseException` (e-mail duplicado, caminho comum) | 409 | `conflict` |
| `BadCredentialsException` / falha de JWT | 401 | `unauthorized` |
| `MethodArgumentTypeMismatchException` (ex.: `status` fora do enum `ReadingStatus` em query param, US-13) | 400 | `validation-error` |
| `ConstraintViolationException` (`@Min`/`@Max` em `@RequestParam` individual, ex.: `minRating`/`maxRating` fora de 1–5, US-15) | 400 | `validation-error` |
| Exceção não mapeada | 500 | `internal-error` (sem detalhes internos no corpo) |

Importante: `GET/PATCH/DELETE /books/{id}` de um livro de **outro** usuário retorna **404**, nunca 403 — não revela a existência do recurso para quem não é o dono (mesma regra já definida na spec).

Nota de implementação (US-06) — formato do erro de validação de campo: o
`ProblemDetail` retornado por `handleValidation`
(`MethodArgumentNotValidException`) carrega uma extension property
`errors`, um array de `{field, message}` (um item por campo inválido),
via `problemDetail.setProperty("errors", List<FieldErrorDetail>)`. O
Jackson 3 mescla extension properties no nível raiz do JSON, então o
array fica acessível em `$.errors[].field`/`$.errors[].message`. O campo
`detail` do `ProblemDetail` continua presente com um resumo textual
(`"title: <msg>; author: <msg>"`), para consumidores que só leem
`detail`. Este é o formato oficial para erros de validação de campo a
partir de agora — próximas validações de Bean Validation (ex.: US-15,
`minRating`/`maxRating`) devem seguir o mesmo padrão em vez de criar um
DTO de erro customizado.

Nota de implementação (US-07) — `status` fora do enum no `PATCH
/books/{id}`: a desserialização de um valor de `status` que não existe em
`ReadingStatus` falha antes do Bean Validation (não passa por
`MethodArgumentNotValidException`), chegando ao
`@RestControllerAdvice` como `HttpMessageNotReadableException` cuja causa
é `InvalidFormatException`. Como o Spring Boot 4.1.1 roda sobre Jackson 3,
essa classe é `tools.jackson.databind.exc.InvalidFormatException`, não a
homônima de `com.fasterxml.jackson.databind.exc` — importar o pacote
errado compila (ambas existem no classpath via outras libs) mas o
`catch`/`instanceof` nunca casa. `handleMessageNotReadable` reaproveita o
mesmo formato `errors: [{field, message}]` do handler de validação (seção
6.4), listando os valores aceitos do enum na mensagem.

Nota de implementação (US-13) — `status` fora do enum no `GET /books`
(query param), caminho diferente do PATCH acima: o bind de query string
para `BookSearchCriteria` passa pelo conversor de `Enum` do Spring MVC
(`WebDataBinder`), não pelo Jackson, então o erro chega ao
`@RestControllerAdvice` como `MethodArgumentTypeMismatchException`, nunca
como `HttpMessageNotReadableException`/`InvalidFormatException`.
`handleMethodArgumentTypeMismatch` é um handler separado em
`GlobalExceptionHandler` — reaproveita o mesmo formato de mensagem (valor
inválido + valores aceitos do enum) do handler do PATCH, mas não foi
extraído um helper comum entre os dois (decisão deliberada, para não
refatorar fora do escopo da US-13).

Nota de implementação (US-15) — `ConstraintViolationException` de
`@RequestParam` individual, caminho diferente do `MethodArgumentNotValidException`
(Bean Validation em corpo `@Valid`) e do `MethodArgumentTypeMismatchException`
(erro de conversão de tipo em query param, US-13): quando `@Min`/`@Max`
são colocados diretamente em um parâmetro de método de controller
anotado com `@RequestParam` (necessário para validar
`minRating`/`maxRating`, já que não é possível anotar um campo de record
bindado implicitamente — ver seção 6.3), uma violação é lançada pelo
`MethodValidationInterceptor` do Spring como
`jakarta.validation.ConstraintViolationException`, sem passar pelo
`BindingResult` usado pelos outros dois handlers.
`handleConstraintViolation` extrai o nome do campo do último nó de
`violation.getPropertyPath()` e reaproveita o mesmo formato de resposta
`errors: [{field, message}]` dos demais handlers de validação (seção
6.4). Convenção a seguir para qualquer `@RequestParam` individual que
precise de Bean Validation no futuro.

Nota de implementação (US-07) — `Book` imutável: `BookService.update` não
muta a entidade gerenciada; monta uma nova instância de `Book` via
construtor completo, copiando do registro existente (buscado por
`findByIdAndUserId`) todo campo não enviado no `BookUpdateRequest`, e
delega a `bookRepository.save(...)`. Como o `id` é atribuído pela
aplicação (`UUID.randomUUID()`, não `@GeneratedValue`), o Hibernate trata
esse `save` como merge (insert se o id não existir, update se existir),
então não é necessário buscar a entidade "anexada" à sessão para que a
atualização seja persistida. Próximas histórias que alterem `Book`
(US-08+) devem seguir o mesmo padrão em vez de adicionar setters.

Nota de implementação (US-01): no dialeto `SQLiteDialect`
(`hibernate-community-dialects`), a violação da constraint `UNIQUE` de
`email` **não** é traduzida pelo Spring/Hibernate para
`DataIntegrityViolationException` como ocorreria em outros dialetos — por
isso `AuthService` checa explicitamente a unicidade do e-mail antes do
`INSERT` e lança `EmailAlreadyInUseException`, mantendo o handler de
`DataIntegrityViolationException` apenas como defesa em profundidade.

## 7. Segurança

- `SecurityConfig`: `SecurityFilterChain` stateless (`SessionCreationPolicy.STATELESS`), CSRF desabilitado (API pura, sem cookies de sessão), `JwtAuthenticationFilter` registrado antes do filtro padrão de autenticação.
- Rotas públicas: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `/actuator/health`. Todo o resto exige autenticação.
- CORS: não configurado na v1 (sem frontend definido ainda); ao introduzir um frontend, adicionar `CorsConfigurationSource` restrito às origens conhecidas — **não** usar `*` em produção.
- Segredo do JWT (`app.jwt.secret`) vem de variável de ambiente, nunca commitado; valor de desenvolvimento em `application-dev.yml` apenas para rodar localmente.

Nota de implementação (US-01): a US-01 já criou um `SecurityFilterChain`
mínimo (CSRF desabilitado, `/api/v1/auth/**` e `/actuator/health`
liberados, resto exige autenticação) — sem ele o Spring Security padrão
bloquearia com `401` o próprio endpoint de registro. A US-02 passou a
emitir o token (`JwtTokenProvider`/`POST /auth/login`) sem alterar esse
filter chain.

Nota de implementação (US-03): `SessionCreationPolicy.STATELESS` e
`JwtAuthenticationFilter` (`addFilterBefore`, antes de
`UsernamePasswordAuthenticationFilter`) foram adicionados ao
`SecurityFilterChain`. O filtro extrai `Authorization: Bearer <token>`,
valida via `JwtTokenProvider.extractUserId` e, se válido, popula o
`SecurityContext` com o `userId` (UUID) como principal; se ausente ou
inválido/expirado, deixa o contexto vazio e segue a cadeia — quem decide a
resposta de erro é o Spring Security, não o filtro.

A resposta de erro para token ausente/inválido é escrita por um
`AuthenticationEntryPoint` dedicado (`JwtAuthenticationEntryPoint`,
registrado via `exceptionHandling().authenticationEntryPoint(...)`), não
pelo `GlobalExceptionHandler`: como esse ponto de extensão roda antes do
`DispatcherServlet`, um `@RestControllerAdvice` não teria chance de atuar.
O `JwtAuthenticationEntryPoint` escreve o corpo `ProblemDetail`
(Problem+JSON) manualmente para manter o mesmo formato usado pelas demais
respostas de erro da API. O handler de `JwtException` no
`GlobalExceptionHandler` (seção 6.4) permanece como defesa em profundidade
para um cenário futuro em que algum controller/service faça parse de
token diretamente (ex.: endpoint de refresh) — não é o caminho acionado
pelo filtro atual, já que ele captura a exceção internamente.

Para obter o `userId` autenticado em controllers/services foi criado
`common/CurrentUser.id()` — um helper estático que lê
`SecurityContextHolder`, em vez de um `@AuthenticationPrincipal`
customizado. Decisão relevante para US-04 em diante: `BookService` deve
chamar `CurrentUser.id()` diretamente; controllers não devem conter
qualquer lógica de obtenção do usuário autenticado.

O terceiro critério de aceite da US-03 (404 ao acessar/editar/remover
livro de outro usuário) depende de existir ao menos um endpoint de CRUD
de livro para ser observável em teste — fica registrado como pendência de
verificação nas histórias de CRUD (US-04+), que devem usar
`CurrentUser.id()` como fonte do `userId` em toda query/gravação do
`BookRepository`.

## 8. Configuração (`application.yml`)

Profiles sugeridos: `dev` (SQLite em arquivo local, logs verbosos) e `prod` (SQLite em caminho configurável via env var, logs em nível `INFO`).

Chaves principais:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:1440}
spring:
  datasource:
    url: jdbc:sqlite:${DB_PATH:./data/myreadings.db}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Nota de implementação (US-02): `app.jwt.expiration-minutes` segue o mesmo
padrão de override opcional por variável de ambiente já usado em
`app.jwt.secret`/`spring.datasource.url` (`JWT_EXPIRATION_MINUTES`,
default `1440`), em vez do valor fixo originalmente esboçado aqui.

## 9. Estratégia de testes

- **Unit:** `BookService`/`AuthService` com Mockito, cobrindo regras de validação cruzada (datas, isolamento por usuário) sem subir contexto Spring.
- **Integração (`@SpringBootTest` + `@AutoConfigureMockMvc`):** cada endpoint P0, incluindo os critérios de aceite da spec (401 sem token, 404 em recurso de outro usuário, 400 em campo obrigatório ausente, lista vazia com 200 em busca sem match).
- **Banco de testes:** arquivo SQLite temporário por classe de teste (`@TempDir`), migrado via Flyway no `@BeforeAll` — evita divergência de comportamento entre ambiente de teste e produção (não usar H2 em memória, já que o dialeto SQLite tem particularidades, ex: ausência de `ILIKE`).
- Cobertura mínima esperada: todos os critérios de aceite "Must-Have (P0)" listados na spec têm pelo menos um teste de integração correspondente.

## 10. Rastreabilidade com a spec

| Requisito da spec (P0) | Componente técnico |
|---|---|
| Registro/login de usuário | `AuthController`, `AuthService`, `User`, `UserRepository` |
| Isolamento por usuário (404 em recurso de terceiro) | `BookRepository#findByIdAndUserId(id, userId)` chamado inline em `BookService.update` (lança `NotFoundException` se vazio); na listagem, `BookSpecifications#hasUserId` aplicado sempre como base da `Specification` em `BookService#search` |
| CRUD de livro com campos opcionais exceto título/autor | `Book`, `BookCreateRequest`/`BookUpdateRequest`, Bean Validation |
| Status fechado em enum | `ReadingStatus`, `@Enumerated(STRING)` |
| Listagem paginada | `BookController#list` + `Pageable` do Spring Data |
| Busca por texto livre em título/autor | `BookSpecifications#matchesSearchTerm` (`LOWER(title) LIKE ... OR LOWER(author) LIKE ...`), combinado em `BookService#search` via `Specification<Book>` |
| Filtros por status/gênero/avaliação combináveis | `BookSearchCriteria` + `Specification<Book>` (Spring Data JPA Specifications) |
| Remover livro do catálogo | `BookService#delete` (busca via `findByIdAndUserId`, `404` via `NotFoundException` se não encontrado/não é do usuário, remove via `BookRepository#delete`), `BookController#delete` (`DELETE /books/{id}` → `204`) |

Nota de implementação (US-18): `GET /books/{id}` (busca de livro único por
id, `BookService#findById` + `BookController#get`) já estava previsto na
tabela de rotas da seção 6.2, mas não tinha história própria no backlog
(US-01 a US-18) nem linha nesta tabela de rastreabilidade — foi
implementado junto da US-18 porque o critério de aceite "GET subsequente
ao id removido retorna 404" depende dele. Está confirmado implementado e
testado (ver `docs/tasks/18-remover-livro.md`, seção Observações).

## 11. Riscos e trade-offs assumidos

- **PATCH com semântica "omitido = não altera":** não há forma nativa de distinguir "campo enviado como `null`" de "campo omitido" em JSON sem usar wrapper (`Optional<T>`/`JsonNullable`). Decisão v1: tratar `null` recebido como "não alterar" — ou seja, **não é possível limpar um campo opcional via PATCH na v1** (ex: remover uma `coverUrl` já cadastrada exige recriar o registro ou uma decisão futura de usar `JsonNullable`). Documentado aqui para não ser descoberto como bug depois.
- **`updatedAt` avança mesmo em PATCH sem nenhum campo de negócio alterado (US-10):** `BookService.update` chama `Instant.now()` incondicionalmente a cada execução, independente de o merge resultar em algum campo efetivamente diferente do valor já persistido. Isso inclui o caso-limite de um `PATCH /books/{id}` com corpo vazio `{}` (ou um corpo cujos campos coincidem com os já persistidos): a resposta é `200` sem nenhuma mudança de negócio, mas `updatedAt` ainda avança. Comportamento intencional (simplicidade: não há diff campo a campo antes de decidir se grava), não um bug — registrado aqui para não ser lido como regressão no futuro.
- **Busca textual com `LIKE`:** sem índice full-text, `LIKE '%termo%'` força varredura completa da tabela por usuário. Aceitável no volume esperado (uso pessoal); revisitar com FTS5 se a base crescer muito (nenhuma evidência disso hoje).
- **SQLite em produção:** um único arquivo, sem replicação. Aceitável para instância single-tenant/self-hosted; não serve para múltiplas instâncias da aplicação escrevendo concorrentemente no mesmo arquivo.
