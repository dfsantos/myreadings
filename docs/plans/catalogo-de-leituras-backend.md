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

### 6.4 Tratamento de erros

Usar `ProblemDetail` (RFC 7807, nativo do Spring 6+) via `@RestControllerAdvice` central (`GlobalExceptionHandler`):

| Exceção | Status | `type`/`title` |
|---|---|---|
| `MethodArgumentNotValidException` (Bean Validation) | 400 | `validation-error`, detalha campo(s) inválido(s) |
| `NotFoundException` (livro não existe ou não pertence ao usuário) | 404 | `not-found` |
| `DataIntegrityViolationException` (e-mail duplicado) | 409 | `conflict` |
| `BadCredentialsException` / falha de JWT | 401 | `unauthorized` |
| Exceção não mapeada | 500 | `internal-error` (sem detalhes internos no corpo) |

Importante: `GET/PATCH/DELETE /books/{id}` de um livro de **outro** usuário retorna **404**, nunca 403 — não revela a existência do recurso para quem não é o dono (mesma regra já definida na spec).

## 7. Segurança

- `SecurityConfig`: `SecurityFilterChain` stateless (`SessionCreationPolicy.STATELESS`), CSRF desabilitado (API pura, sem cookies de sessão), `JwtAuthenticationFilter` registrado antes do filtro padrão de autenticação.
- Rotas públicas: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `/actuator/health`. Todo o resto exige autenticação.
- CORS: não configurado na v1 (sem frontend definido ainda); ao introduzir um frontend, adicionar `CorsConfigurationSource` restrito às origens conhecidas — **não** usar `*` em produção.
- Segredo do JWT (`app.jwt.secret`) vem de variável de ambiente, nunca commitado; valor de desenvolvimento em `application-dev.yml` apenas para rodar localmente.

## 8. Configuração (`application.yml`)

Profiles sugeridos: `dev` (SQLite em arquivo local, logs verbosos) e `prod` (SQLite em caminho configurável via env var, logs em nível `INFO`).

Chaves principais:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-minutes: 1440
spring:
  datasource:
    url: jdbc:sqlite:${DB_PATH:./data/myreadings.db}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## 9. Estratégia de testes

- **Unit:** `BookService`/`AuthService` com Mockito, cobrindo regras de validação cruzada (datas, isolamento por usuário) sem subir contexto Spring.
- **Integração (`@SpringBootTest` + `@AutoConfigureMockMvc`):** cada endpoint P0, incluindo os critérios de aceite da spec (401 sem token, 404 em recurso de outro usuário, 400 em campo obrigatório ausente, lista vazia com 200 em busca sem match).
- **Banco de testes:** arquivo SQLite temporário por classe de teste (`@TempDir`), migrado via Flyway no `@BeforeAll` — evita divergência de comportamento entre ambiente de teste e produção (não usar H2 em memória, já que o dialeto SQLite tem particularidades, ex: ausência de `ILIKE`).
- Cobertura mínima esperada: todos os critérios de aceite "Must-Have (P0)" listados na spec têm pelo menos um teste de integração correspondente.

## 10. Rastreabilidade com a spec

| Requisito da spec (P0) | Componente técnico |
|---|---|
| Registro/login de usuário | `AuthController`, `AuthService`, `User`, `UserRepository` |
| Isolamento por usuário (404 em recurso de terceiro) | `BookService#findOwnedOrThrow`, filtro `user_id` em toda query do `BookRepository` |
| CRUD de livro com campos opcionais exceto título/autor | `Book`, `BookCreateRequest`/`BookUpdateRequest`, Bean Validation |
| Status fechado em enum | `ReadingStatus`, `@Enumerated(STRING)` |
| Listagem paginada | `BookController#list` + `Pageable` do Spring Data |
| Busca por texto livre em título/autor | `BookRepository` com `@Query` `LOWER(title) LIKE ... OR LOWER(author) LIKE ...` |
| Filtros por status/gênero/avaliação combináveis | `BookSearchCriteria` + `Specification<Book>` (Spring Data JPA Specifications) |

## 11. Riscos e trade-offs assumidos

- **PATCH com semântica "omitido = não altera":** não há forma nativa de distinguir "campo enviado como `null`" de "campo omitido" em JSON sem usar wrapper (`Optional<T>`/`JsonNullable`). Decisão v1: tratar `null` recebido como "não alterar" — ou seja, **não é possível limpar um campo opcional via PATCH na v1** (ex: remover uma `coverUrl` já cadastrada exige recriar o registro ou uma decisão futura de usar `JsonNullable`). Documentado aqui para não ser descoberto como bug depois.
- **Busca textual com `LIKE`:** sem índice full-text, `LIKE '%termo%'` força varredura completa da tabela por usuário. Aceitável no volume esperado (uso pessoal); revisitar com FTS5 se a base crescer muito (nenhuma evidência disso hoje).
- **SQLite em produção:** um único arquivo, sem replicação. Aceitável para instância single-tenant/self-hosted; não serve para múltiplas instâncias da aplicação escrevendo concorrentemente no mesmo arquivo.
