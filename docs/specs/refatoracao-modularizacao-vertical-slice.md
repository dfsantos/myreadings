# Plano de refatoração: modularização em vertical slices

> Status: **proposta — aguardando avaliação**. Nenhum código em `src/` foi
> alterado para produzir este documento. Este plano só deve ser executado
> após aprovação explícita, e preferencialmente em PRs pequenos, um por fase.

## 1. Objetivo

Reorganizar o código de produção em torno das duas entidades de negócio do
domínio — **usuário** e **livro** — seguindo princípios de *vertical slice*,
de forma que cada módulo concentre tudo que é específico dele (entidade,
repositório, regra de negócio, DTOs, controller e tratamento de erro próprio)
e dependa do mínimo possível de infraestrutura compartilhada.

O critério de sucesso declarado pelo usuário é duplo:

1. **Zero mudança de comportamento.** Toda rota, status HTTP, formato de
   `ProblemDetail`, schema de banco e regra de negócio deve permanecer
   byte-a-byte idêntica. Esta é uma refatoração pura: mover/renomear/dividir
   classes, nunca mudar o que elas fazem.
2. **Preparar o terreno para uma futura decomposição em microsserviços** —
   sem de fato decompor agora (ver seção 7, Fora de escopo).

## 2. Diagnóstico do estado atual

### 2.1. O que já está bem encaminhado

- `Book` **não** tem relacionamento JPA com `User` — guarda só um `userId`
  (`UUID`) solto. Isso significa que o módulo de livro já não depende do
  agregado `User` em tempo de execução, só de um identificador opaco. É
  exatamente a forma de acoplamento que sobrevive a uma separação em
  serviços distintos.
- `BookSpecifications` já é package-private (`final class` sem `public`),
  um bom precedente de "detalhe de implementação não vaza para fora do
  pacote" que vale estender a outras classes internas.
- A autenticação já é JWT stateless sem sessão — o padrão que permite cada
  serviço validar o token de forma independente, sem chamar um serviço de
  autenticação a cada requisição.

### 2.2. Violações de fronteira que a modularização atual não evita

- **`common` depende de `auth`.** `GlobalExceptionHandler` (pacote `common`)
  importa `auth.EmailAlreadyInUseException` diretamente. Isso inverte a
  direção de dependência esperada: um pacote "genérico"/compartilhado nunca
  deveria conhecer uma exceção de um módulo de negócio específico. Se
  `auth`/`user` fosse extraído amanhã, `common` iria junto por obrigação.
- **`InvalidDateRangeException` mora em `common`, mas é regra de `book`.**
  Validação de `endDate >= startDate` é uma regra de negócio exclusiva do
  catálogo de livros; não há motivo para estar ao lado de utilitários
  genéricos como os `AttributeConverter`.
- **`auth` e `user` são, na prática, o mesmo módulo de negócio** (identidade
  do usuário: cadastro, login, emissão de token), mas hoje estão em dois
  pacotes Java separados sem um limite claro entre eles — `auth` depende de
  `user.User`/`user.UserRepository`, mas a entidade continua isolada em seu
  próprio pacote sem nenhuma outra razão de ser.
- **Migração de schema (Flyway) não é segmentada por módulo** — um único
  diretório `db/migration/` concentra `V1` (usuários) e `V2` (livros). Numa
  decomposição futura, cada serviço precisaria herdar só a migração da sua
  própria tabela.
- **`GlobalExceptionHandler` é uma classe única de ~190 linhas** misturando
  erros genéricos de framework (bean validation, bind de query param,
  corpo malformado) com erros de negócio específicos de um módulo
  (`EmailAlreadyInUseException`, `BadCredentialsException`,
  `InvalidDateRangeException`, `JwtException`). Isso é o oposto de vertical
  slice: a lógica de erro de um módulo não está encapsulada nele.

## 3. Princípios orientadores da arquitetura alvo

1. **Dois módulos de negócio, só isso:** `user` e `book`. Nenhuma classe de
   regra de negócio fora deles (mantém a regra já escrita em
   `code-conventions.md`).
2. **Direção de dependência de mão única:** `user` e `book` podem depender
   de `security` e `common` (infraestrutura compartilhada); `security` e
   `common` nunca dependem de `user` ou `book`; `user` e `book` nunca
   dependem um do outro. `config` é a raiz de composição e pode depender de
   qualquer pacote (é o único lugar que "conhece" todos os módulos).
3. **Cada módulo trata seus próprios erros.** Exceção de negócio específica
   de um módulo é mapeada para `ProblemDetail` por um `@RestControllerAdvice`
   que vive dentro do próprio módulo. O handler compartilhado em `common`
   só cobre o que é genuinamente genérico (erros de bind/validação do
   Spring, aplicáveis a qualquer DTO de qualquer módulo presente ou
   futuro).
4. **Autenticação é infraestrutura compartilhada, não lógica de negócio do
   módulo `user`.** Validar um JWT (extrair o `userId`, popular o
   `SecurityContext`, responder 401 quando ausente/inválido) é algo que,
   numa decomposição futura, **todo** serviço replicaria de forma
   independente — é a essência do JWT stateless (nenhum serviço chama um
   "auth service" a cada requisição para validar o token). Por isso essa
   parte vai para um pacote novo, `security`, separado do pacote `user`.
   Só a **emissão** do token no login/registro é, de fato, lógica exclusiva
   do módulo `user` (só quem possui a identidade emite a credencial).
5. **`common` fica reduzido ao que é realmente genérico e sem estado de
   domínio:** conversores JPA (`Instant`/`LocalDate` ↔ `TEXT`) e o conceito
   abstrato de "recurso não encontrado" (`NotFoundException`), que qualquer
   módulo futuro pode reutilizar sem acoplamento a um domínio específico.

## 4. Árvore de pacotes alvo

```
dev.dfsantos.myreadings
├── config/                         # raiz de composição — único pacote que conhece todos os módulos
│   └── SecurityConfig.java         # inalterado em comportamento; passa a importar de `security`
│
├── security/                       # infraestrutura de autenticação compartilhada (não é "negócio de user")
│   ├── CurrentUser.java            # movido de common — leitura do userId autenticado
│   ├── JwtTokenProvider.java       # movido de auth — emissão (usado por user) e validação (usada aqui)
│   ├── JwtAuthenticationFilter.java   # movido de auth
│   ├── JwtAuthenticationEntryPoint.java  # movido de auth
│   └── SecurityExceptionHandler.java     # novo — extrai o handler de JwtException do GlobalExceptionHandler
│
├── user/                           # módulo de negócio: identidade do usuário
│   ├── User.java                   # já estava aqui
│   ├── UserRepository.java         # já estava aqui
│   ├── AuthController.java         # movido de auth (nome mantido — rota /api/v1/auth)
│   ├── AuthService.java            # movido de auth
│   ├── EmailAlreadyInUseException.java   # movido de auth
│   ├── UserExceptionHandler.java   # novo — extrai EmailAlreadyInUseException/BadCredentialsException do GlobalExceptionHandler
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       ├── RegisterResponse.java
│       └── TokenResponse.java
│
├── book/                           # módulo de negócio: catálogo de livros (estrutura já correta, só ganha o que faltava)
│   ├── Book.java
│   ├── BookController.java
│   ├── BookRepository.java
│   ├── BookSearchCriteria.java
│   ├── BookService.java
│   ├── BookSpecifications.java
│   ├── ReadingStatus.java
│   ├── InvalidDateRangeException.java    # movido de common — é regra exclusiva do catálogo
│   ├── BookExceptionHandler.java         # novo — extrai InvalidDateRangeException do GlobalExceptionHandler
│   └── dto/
│       ├── BookCreateRequest.java
│       ├── BookResponse.java
│       └── BookUpdateRequest.java
│
└── common/                         # só o que é genérico e sem conhecimento de domínio
    ├── GlobalExceptionHandler.java # reduzido: só erros de framework/bind, genéricos a qualquer DTO
    ├── NotFoundException.java      # conceito genérico, reaproveitável por qualquer módulo futuro
    ├── InstantStringConverter.java
    └── LocalDateStringConverter.java
```

Nenhuma classe muda de nome nesta proposta — só de pacote — exceto a
criação de três classes novas, pequenas e sem lógica própria além de
`@ExceptionHandler`s hoje existentes no `GlobalExceptionHandler`
(`SecurityExceptionHandler`, `UserExceptionHandler`, `BookExceptionHandler`).

### 4.1. `GlobalExceptionHandler` depois da divisão

Fica só com o que é genuinamente agnóstico de domínio:

| Exceção | Por que continua genérica |
|---|---|
| `MethodArgumentNotValidException` | Bean Validation em `@Valid` de qualquer DTO |
| `ConstraintViolationException` | Bean Validation em `@RequestParam` de qualquer controller |
| `HttpMessageNotReadableException` | corpo malformado/enum inválido em qualquer DTO de request |
| `MethodArgumentTypeMismatchException` | conversão de query/path param inválida em qualquer controller |
| `DataIntegrityViolationException` | defesa em profundidade contra violação de `UNIQUE`, não específica de uma tabela |
| `NotFoundException` | conceito de "404 por isolamento de usuário", reaproveitável por qualquer módulo |

Migram para handlers dedicados: `EmailAlreadyInUseException` e
`BadCredentialsException` → `user.UserExceptionHandler`;
`InvalidDateRangeException` → `book.BookExceptionHandler`; `JwtException` →
`security.SecurityExceptionHandler`. Cada um usa
`@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.<modulo>")`
(exceto o de `security`, que permanece global por ser infraestrutura, não
negócio de um módulo) — o texto e o status HTTP de cada resposta de erro
continuam exatamente os mesmos.

### 4.2. Migrações Flyway por módulo

```
src/main/resources/db/migration/
├── user/
│   └── V1__create_users_table.sql   # conteúdo inalterado, só o caminho muda
└── book/
    └── V2__create_books_table.sql   # conteúdo inalterado, só o caminho muda
```

`application.yaml` passa a declarar as duas localizações explicitamente:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration/user,classpath:db/migration/book
```

O Flyway mantém uma única tabela `flyway_schema_history` por banco,
independente de quantos diretórios estão listados em `locations` — a
numeração de versão (`V1`, `V2`) e o histórico já aplicado em ambientes
existentes não são afetados. Isso só antecipa a estrutura de pastas que
cada serviço levaria consigo numa decomposição futura.

## 5. Pontos de decisão que pedem aval explícito

Estes são os pontos em que havia mais de um caminho razoável — decidi por
uma opção e documento a alternativa, mas vale confirmar antes de
implementar:

1. **Criar o pacote `security` (recomendado) vs. deixar `CurrentUser`/JWT
   dentro de `common`/`user`.** Optei por `security` porque tratar
   validação de token como "detalhe compartilhado por todos os serviços"
   (em vez de "lógica do módulo user") é o que de fato se replicaria numa
   decomposição real. A alternativa mais simples seria só fundir `auth` em
   `user` e deixar `CurrentUser` em `common` — funciona, mas mistura
   infraestrutura de autenticação com utilitário genérico sem domínio.
2. **Dividir `GlobalExceptionHandler` em quatro classes (recomendado) vs.
   manter um único handler central.** Um handler único é mais fácil de
   varrer com os olhos hoje, mas é exatamente o tipo de acoplamento que
   impede extrair um módulo sem levar erro de outro módulo junto. Com ~18
   histórias de usuário já implementadas e potencial de crescer, a divisão
   paga o custo de uma classe a mais por módulo.
3. **Namespacing das migrações Flyway (recomendado) vs. manter uma pasta
   única.** Baixo risco (Flyway não se importa com a estrutura de pastas),
   mas é churn em arquivo versionado — se o time não vir valor imediato,
   dá para adiar para quando a separação em serviços for de fato cogitada.
4. **Manter nomes de classes (`AuthController`, `AuthService`) mesmo após
   mover para o pacote `user`.** Evita renomear por renomear; a rota
   pública continua `/api/v1/auth/**`, e o nome da classe reflete a rota,
   não o pacote.

## 6. Plano de execução em fases

Cada fase é um commit (ou PR) isolado, compila e passa 100% dos testes
existentes antes de seguir para a próxima — nenhuma fase depende de
alterar uma asserção de teste, só pacote/import. Ordem pensada para minimizar
retrabalho entre fases.

| # | Fase | Commit sugerido (Conventional Commits) |
|---|---|---|
| 1 | Criar pacote `security`; mover `CurrentUser`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`; extrair `SecurityExceptionHandler` (handler de `JwtException`) do `GlobalExceptionHandler`; atualizar `SecurityConfig` e `AuthService`; mover os testes correspondentes (`JwtAuthenticationFilterTest`, `JwtAuthenticationIntegrationTest`) para `src/test/.../security/` | `refactor(security): extrai infraestrutura de JWT para pacote compartilhado` |
| 2 | Mover `AuthController`, `AuthService`, `EmailAlreadyInUseException`, `dto/*` de `auth` para `user`; extrair `UserExceptionHandler` (handlers de `EmailAlreadyInUseException` e `BadCredentialsException`) do `GlobalExceptionHandler`; apagar o pacote `auth` vazio; mover `AuthControllerTest` para `src/test/.../user/` | `refactor(user): funde pacote auth no módulo user` |
| 3 | Mover `InvalidDateRangeException` de `common` para `book`; extrair `BookExceptionHandler` (handler de `InvalidDateRangeException`) do `GlobalExceptionHandler` | `refactor(book): internaliza exceção de validação de datas no módulo` |
| 4 | Confirmar `GlobalExceptionHandler` reduzido ao conjunto genérico da seção 4.1 (nenhuma lógica nova, só o que sobrou depois das fases 1–3) | incluído no commit da fase 3, ou `refactor(common): reduz GlobalExceptionHandler aos erros genéricos de framework` se feito à parte |
| 5 | Mover `V1__create_users_table.sql` para `db/migration/user/`, `V2__create_books_table.sql` para `db/migration/book/`; adicionar `spring.flyway.locations` em `application.yaml` | `refactor(build): segmenta migrações Flyway por módulo` |
| 6 | Atualizar a árvore de pacotes documentada em `CLAUDE.md` e em `.claude/rules/code-conventions.md` para refletir `user`/`book`/`security`/`common`/`config`, e registrar a convenção de exception handler por módulo — **a cargo do agente `docs-plan-keeper`**, não desta refatoração de código | `docs: atualiza convenção de pacotes para refletir a modularização por vertical slice` |

Critério de "pronto" ao final de cada fase: `./gradlew build` verde, sem
nenhuma asserção de teste alterada (`git diff` só deve mostrar
rename/import/pacote/anotação de escopo do `@RestControllerAdvice`), e
nenhuma mudança observável via HTTP (mesmas rotas, status, corpo de
resposta e `ProblemDetail`).

## 7. Fora de escopo (não-metas desta refatoração)

- **Separação física em módulos Gradle** (multi-module build, um `build.gradle`
  por módulo). Essa seria a próxima etapa natural depois desta, mas é bem
  mais invasiva (build, classpath, testes de contrato entre módulos) e não
  foi pedida agora.
- **Decomposição real em microsserviços** (bancos separados, comunicação
  via rede/mensageria entre `user` e `book`). Este plano só organiza o
  monólito para que essa decisão, se vier a ser tomada, custe menos.
- **Qualquer mudança de contrato HTTP**, de schema de banco ou de regra de
  negócio.
- **Adicionar `OpenApiConfig`** (mencionado como planejado em `CLAUDE.md`
  mas inexistente hoje) — fora do escopo desta refatoração.
- **Endurecer visibilidade (package-private) de forma extensiva** ou
  adicionar `package-info.java` por módulo documentando fronteiras. É uma
  melhoria incremental saudável, mas não é necessária para o objetivo
  declarado e pode ser proposta separadamente depois que a nova árvore de
  pacotes estiver estável.

## 8. Riscos e mitigação

- **Risco:** mover classes de pacote quebra testes que dependem de
  visibilidade package-private (ex.: um teste unitário no mesmo pacote de
  uma classe não-pública). **Mitigação:** mover o teste para o mesmo pacote
  novo da classe testada, na mesma fase.
- **Risco:** o `@RestControllerAdvice(basePackages = ...)` tem uma
  semântica de precedência diferente de um advice global sem escopo
  (Spring resolve por "mais específico primeiro", mas em caso de exceções
  que nunca se sobrepõem entre módulos — como é o caso aqui — isso não
  deve se manifestar). **Mitigação:** cobrir explicitamente nos testes de
  integração existentes (já cobrem todos os status/erros hoje) e rodar a
  suíte completa a cada fase antes de prosseguir.
- **Risco:** segmentar `locations` do Flyway e esquecer uma migração.
  **Mitigação:** validar com um teste de integração existente que já sobe
  o schema via Flyway num SQLite temporário (`@TempDir`) — se a tabela não
  existir, o teste falha na primeira query.

## 9. Critérios de aceite da refatoração

- `./gradlew build` (compilação + suíte de testes completa) verde, sem
  nenhuma asserção de teste modificada além de pacote/import.
- `git diff` entre o estado atual e o final mostra apenas movimentação de
  arquivo, ajuste de `package`/import, e a extração mecânica de métodos de
  `@ExceptionHandler` para as novas classes — nenhuma linha de regra de
  negócio reescrita.
- Nenhum arquivo de migração SQL tem conteúdo alterado, só caminho.
- `CLAUDE.md` e `.claude/rules/code-conventions.md` atualizados para
  descrever a árvore de pacotes final (fase 6), para que a documentação do
  projeto não fique divergente do código — como manda a convenção já
  vigente no projeto.
