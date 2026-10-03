# myreadings

Backend do **Catálogo de Leituras** — uma API REST que permite a um usuário
autenticado manter sua própria lista de livros (status de leitura, datas de
início/fim, avaliação, busca e filtros), isolada da lista de outros usuários.

Todo o backlog de histórias de usuário (US-01 a US-18) está implementado.

## Stack

- **Java 25** (toolchain Gradle) + **Spring Boot 4.1.1**
- **Spring Web MVC** (REST) sob o prefixo `/api/v1`
- **Spring Security** + **JWT** stateless (`io.jsonwebtoken:jjwt` 0.12.6) —
  sem sessão/cookie, sem OAuth de terceiro nesta versão
- **Spring Data JPA** + **Hibernate 7** sobre **SQLite**
  (`org.xerial:sqlite-jdbc` + `hibernate-community-dialects`)
- **Flyway** como única fonte da verdade do schema
  (`spring.jpa.hibernate.ddl-auto: validate`)
- **Bean Validation** (`spring-boot-starter-validation`) nos DTOs de request
- **Spring Boot Actuator** (health check)
- **Gradle** (wrapper incluso)

## Executando o projeto

Requer Java 25 instalado (ou deixe o Gradle toolchain baixá-lo
automaticamente).

```bash
# variável obrigatória: segredo usado para assinar os tokens JWT
export JWT_SECRET="um-segredo-bem-grande-e-aleatorio"

./gradlew bootRun
```

A API fica disponível em `http://localhost:8080/api/v1`. O Flyway aplica as
migrações automaticamente na inicialização e, por padrão, o banco SQLite é
criado em `./data/myreadings.db`.

### Variáveis de ambiente

| Variável | Obrigatória | Default | Descrição |
|---|---|---|---|
| `JWT_SECRET` | sim | — | Chave usada para assinar/validar os JWTs (HS256) |
| `JWT_EXPIRATION_MINUTES` | não | `1440` | Tempo de expiração do token, em minutos |
| `DB_PATH` | não | `./data/myreadings.db` | Caminho do arquivo SQLite |

### Rodando os testes

```bash
./gradlew test
```

Os testes de integração usam SQLite real (arquivo temporário), migrado via
Flyway — não usam H2.

### Hook de commit

O repositório valida a primeira linha de cada commit contra
[Conventional Commits](https://www.conventionalcommits.org/) via um hook
`commit-msg` instalado automaticamente ao rodar qualquer comando Gradle
(`core.hooksPath` em `settings.gradle`). Detalhes em
[`.claude/rules/commit-conventions.md`](.claude/rules/commit-conventions.md).

## Autenticação

1. `POST /api/v1/auth/register` cria a conta.
2. `POST /api/v1/auth/login` retorna um `accessToken` JWT.
3. Esse token deve ser enviado em toda requisição aos endpoints de livros via
   header `Authorization: Bearer <accessToken>`.

Todo acesso a um livro de outro usuário (ou que não existe) retorna `404` —
nunca `403` — para não revelar a existência do recurso.

## Endpoints

### Auth (`/api/v1/auth`) — públicos

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/register` | Cria uma conta (`email`, `password`) |
| `POST` | `/login` | Autentica e retorna `{ accessToken, expiresInSeconds }` |

### Livros (`/api/v1/books`) — autenticados

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | Cadastra um livro (default `status = QUERO_LER`) |
| `GET` | `/{id}` | Busca um livro pelo id |
| `PATCH` | `/{id}` | Atualiza parcialmente um livro (status, datas, nota, etc.) |
| `DELETE` | `/{id}` | Remove um livro |
| `GET` | `/` | Lista/busca livros do usuário autenticado, paginado |

Campos de um livro: `title`, `author` (obrigatórios), `publisher`,
`publicationYear`, `pageCount`, `genre` (texto livre), `coverUrl`, `status`
(`QUERO_LER`, `LENDO`, `LIDO`, `ABANDONADO`), `startDate`, `endDate`, `rating`
(1 a 5).

#### Filtros e paginação de `GET /api/v1/books`

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `q` | string | Busca livre em título ou autor (case-insensitive) |
| `status` | enum | Filtra por status de leitura |
| `genre` | string | Filtra por gênero (igualdade exata, case-insensitive) |
| `minRating` / `maxRating` | 1–5 | Filtra por faixa de avaliação |
| `page`, `size`, `sort` | — | Paginação padrão do Spring Data (`size` máximo 100, default 20, ordenado por `createdAt` desc) |

Filtros combinados são aplicados em `AND`. Nenhum filtro resulta na listagem
simples; nenhum resultado retorna `200` com página vazia (nunca `404`).

### Formato de erro

Erros de validação são respondidos como `ProblemDetail` (RFC 7807), `400`,
com a extension property `errors` (`[{field, message}]`) além do `detail`
textual. Mensagens de erro são em português.

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

## Collection do Insomnia

Uma collection pronta com todos os endpoints (auth, livros e health check)
está em [`myreadings-wrk_01024a6a7cee4616bb802d44448cbc53.yaml`](myreadings-wrk_01024a6a7cee4616bb802d44448cbc53.yaml).
Para usar:

1. No Insomnia: `Application` → `Preferences` → `Data` → `Import Data` (ou
   `Create` → `Import` dentro do workspace), selecionando o arquivo.
2. Rode `Login` (pasta `Auth`) e copie o `accessToken` da resposta para a
   variável de ambiente `access_token` — a pasta `Livros` já está configurada
   para enviar `Authorization: Bearer {{ _.access_token }}` em todas as
   requisições.
3. Após criar um livro, copie o `id` da resposta para a variável `book_id`
   para usar nas requisições de busca/atualização/remoção por id.

## Documentação

- [Spec/PRD](docs/specs/catalogo-de-leituras-backend.md)
- [Plano técnico](docs/plans/catalogo-de-leituras-backend.md)
- [Tasks por história de usuário](docs/tasks/)
- [Dashboard de acompanhamento](docs/dashboard.html)
- [Convenção de commits](.claude/rules/commit-conventions.md)
- [Convenção de código](.claude/rules/code-conventions.md)
