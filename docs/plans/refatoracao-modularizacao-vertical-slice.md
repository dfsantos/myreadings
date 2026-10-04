# Plano Técnico — Modularização em Vertical Slices (myreadings)

**Status:** Rascunho v1
**Data:** 2026-10-04
**Spec de origem:** [`docs/specs/refatoracao-modularizacao-vertical-slice.md`](../specs/refatoracao-modularizacao-vertical-slice.md)

Este documento traduz a spec de refatoração em decisões técnicas concretas
e suficientes para implementar — arquivo por arquivo, classe por classe.
Onde a spec deixou um "ponto de decisão" em aberto (seção 5 da spec), este
documento toma a decisão (a opção recomendada pela spec) e justifica.
Nenhum passo deste plano muda comportamento observável: é reposicionamento
de classe, extração mecânica de método e reorganização de arquivo — nunca
reescrita de lógica.

## 1. Decisões que resolvem os pontos em aberto da spec

| Ponto em aberto da spec | Decisão | Justificativa |
|---|---|---|
| Criar pacote `security` vs. deixar JWT/`CurrentUser` em `common`/`user` | **Criar `security`** | Validação de token é infraestrutura replicável por qualquer serviço futuro, não lógica de negócio do módulo `user` — ver seção 3 da spec, princípio 4. |
| Dividir `GlobalExceptionHandler` em 4 classes vs. manter um handler único | **Dividir** (`common`, `user`, `book`, `security`) | É o ponto que hoje inverte a direção de dependência (`common` → `auth`); sem dividir, a modularização dos demais pacotes não elimina o acoplamento mais grave identificado no diagnóstico (spec, seção 2.2). |
| Namespacing de migrações Flyway por módulo vs. manter pasta única | **Namespacing** (`db/migration/user/`, `db/migration/book/`) | Custo baixo (Flyway não depende de estrutura de pastas — ver seção 5 deste documento) e antecipa exatamente a estrutura que cada serviço levaria consigo numa decomposição futura. |
| Renomear classes (`AuthController`→`UserController` etc.) vs. manter nomes | **Manter nomes** | Nenhuma classe muda de nome nesta refatoração, só de pacote — a rota pública continua `/api/v1/auth/**`, e renomear sem necessidade ampliaria o diff sem ganho de modularização. |

## 2. Escopo

Refatoração pura de reorganização de pacotes dentro do módulo Gradle único
já existente. Não há mudança de stack, de dependências em `build.gradle`,
de schema de banco (além da localização do arquivo `.sql`, não do
conteúdo) ou de contrato HTTP. As únicas classes **novas** são três
`@RestControllerAdvice` enxutos, cada um extraindo métodos que já existem
hoje em `GlobalExceptionHandler` — nenhuma linha de regra de negócio nova.

## 3. Estrutura de pacotes — de/para

### 3.1 Produção (`src/main/java/dev/dfsantos/myreadings`)

| De | Para | Mudança |
|---|---|---|
| `common/CurrentUser.java` | `security/CurrentUser.java` | só pacote |
| `auth/JwtTokenProvider.java` | `security/JwtTokenProvider.java` | só pacote |
| `auth/JwtAuthenticationFilter.java` | `security/JwtAuthenticationFilter.java` | só pacote (import de `JwtTokenProvider` passa a ser mesmo pacote, sem `import`) |
| `auth/JwtAuthenticationEntryPoint.java` | `security/JwtAuthenticationEntryPoint.java` | só pacote |
| *(extraído de `common/GlobalExceptionHandler.java`)* | `security/SecurityExceptionHandler.java` | novo arquivo — ver seção 4.1 |
| `auth/AuthController.java` | `user/AuthController.java` | pacote + import de `JwtTokenProvider`/`CurrentUser` não se aplica aqui (controller não os usa diretamente) |
| `auth/AuthService.java` | `user/AuthService.java` | pacote; import de `JwtTokenProvider` passa a ser `dev.dfsantos.myreadings.security.JwtTokenProvider` (cross-module, permitido pela regra de dependência da seção 3 da spec); import de `User`/`UserRepository` passa a ser mesmo pacote (sem `import`) |
| `auth/EmailAlreadyInUseException.java` | `user/EmailAlreadyInUseException.java` | só pacote |
| `auth/dto/LoginRequest.java` | `user/dto/LoginRequest.java` | só pacote |
| `auth/dto/RegisterRequest.java` | `user/dto/RegisterRequest.java` | só pacote |
| `auth/dto/RegisterResponse.java` | `user/dto/RegisterResponse.java` | só pacote |
| `auth/dto/TokenResponse.java` | `user/dto/TokenResponse.java` | só pacote |
| *(extraído de `common/GlobalExceptionHandler.java`)* | `user/UserExceptionHandler.java` | novo arquivo — ver seção 4.2 |
| `user/User.java` | *(inalterado)* | — |
| `user/UserRepository.java` | *(inalterado)* | — |
| `common/InvalidDateRangeException.java` | `book/InvalidDateRangeException.java` | só pacote |
| *(extraído de `common/GlobalExceptionHandler.java`)* | `book/BookExceptionHandler.java` | novo arquivo — ver seção 4.3 |
| `book/*.java` (demais 8 arquivos + `dto/`) | *(inalterado)* | — |
| `config/SecurityConfig.java` | *(mesmo pacote)* | imports de `JwtAuthenticationFilter`/`JwtAuthenticationEntryPoint` passam a apontar para `dev.dfsantos.myreadings.security` |
| `common/GlobalExceptionHandler.java` | *(mesmo pacote)* | reduzido — ver seção 4.4 |
| `common/NotFoundException.java` | *(inalterado)* | — |
| `common/InstantStringConverter.java` | *(inalterado)* | — |
| `common/LocalDateStringConverter.java` | *(inalterado)* | — |

Pacote `auth/` deixa de existir ao final (fica vazio e é removido).

### 3.2 Testes (`src/test/java/dev/dfsantos/myreadings`)

| De | Para | Observação |
|---|---|---|
| `auth/JwtAuthenticationFilterTest.java` | `security/JwtAuthenticationFilterTest.java` | só `package`; já referencia `JwtTokenProvider`/`JwtAuthenticationFilter` sem import explícito (mesmo pacote hoje) — continua assim no pacote novo |
| `auth/JwtAuthenticationIntegrationTest.java` | `security/JwtAuthenticationIntegrationTest.java` | só `package`; nenhum import de classe de produção por FQN (confirmado) |
| `auth/AuthControllerTest.java` | `user/AuthControllerTest.java` | só `package`; nenhum import de classe de produção por FQN (confirmado) |
| `book/BookControllerTest.java` | *(inalterado)* | confirmado: nenhum import de `dev.dfsantos.myreadings.*` por FQN (interage só via `MockMvc`/HTTP) — não precisa de nenhuma edição nesta refatoração |

Nenhum teste tem asserção alterada em nenhuma fase — só a linha `package` e,
quando aplicável, a pasta do arquivo.

### 3.3 Árvore final

```
dev.dfsantos.myreadings
├── config/
│   └── SecurityConfig.java
├── security/
│   ├── CurrentUser.java
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtAuthenticationEntryPoint.java
│   └── SecurityExceptionHandler.java
├── user/
│   ├── User.java
│   ├── UserRepository.java
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── EmailAlreadyInUseException.java
│   ├── UserExceptionHandler.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       ├── RegisterResponse.java
│       └── TokenResponse.java
├── book/
│   ├── Book.java
│   ├── BookController.java
│   ├── BookRepository.java
│   ├── BookSearchCriteria.java
│   ├── BookService.java
│   ├── BookSpecifications.java
│   ├── ReadingStatus.java
│   ├── InvalidDateRangeException.java
│   ├── BookExceptionHandler.java
│   └── dto/
│       ├── BookCreateRequest.java
│       ├── BookResponse.java
│       └── BookUpdateRequest.java
└── common/
    ├── GlobalExceptionHandler.java
    ├── NotFoundException.java
    ├── InstantStringConverter.java
    └── LocalDateStringConverter.java
```

## 4. Os quatro `@RestControllerAdvice`

### 4.1 `security/SecurityExceptionHandler.java` (novo)

```java
package dev.dfsantos.myreadings.security;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Backstop: a validação do token normalmente falha dentro do
// JwtAuthenticationFilter (antes do DispatcherServlet), onde este advice
// não atua — tratado lá via JwtAuthenticationEntryPoint. Cobre o caso de
// algum controller/service futuro, de qualquer módulo, parsear um token
// diretamente (ex: endpoint de refresh) e deixar a exceção vazar — por
// isso este advice NÃO é escopado por basePackages (é infraestrutura
// cross-cutting, não lógica de um módulo de negócio específico).
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("unauthorized");
        problemDetail.setDetail("Token de autenticação ausente, inválido ou expirado");
        return problemDetail;
    }
}
```

Corpo idêntico ao método `handleJwtException` hoje em
`common/GlobalExceptionHandler.java` — só muda de classe/pacote.

### 4.2 `user/UserExceptionHandler.java` (novo)

```java
package dev.dfsantos.myreadings.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.user")
public class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ProblemDetail handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("conflict");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("unauthorized");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
}
```

Corpo idêntico aos métodos `handleEmailAlreadyInUse` e
`handleBadCredentials` hoje em `GlobalExceptionHandler`.
`basePackages = "dev.dfsantos.myreadings.user"` é seguro porque as duas
exceções só são lançadas por `AuthService` (pacote `user`), invocado
sempre a partir de `AuthController` (mesmo pacote) — o Spring resolve o
advice aplicável pelo pacote do **controller** que atende a requisição,
não pelo pacote de onde a exceção foi lançada, e os dois sempre coincidem
aqui.

### 4.3 `book/BookExceptionHandler.java` (novo)

```java
package dev.dfsantos.myreadings.book;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.book")
public class BookExceptionHandler {

    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("validation-error");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
}
```

Corpo idêntico ao método `handleInvalidDateRange` hoje em
`GlobalExceptionHandler`. Mesmo raciocínio de `basePackages` da seção 4.2:
`InvalidDateRangeException` só é lançada por `BookService`, invocado só
por `BookController` (mesmo pacote `book`).

### 4.4 `common/GlobalExceptionHandler.java` (reduzido)

Permanece **sem** `basePackages` (aplica-se globalmente) e com apenas os
handlers que já eram agnósticos de domínio:

```java
package dev.dfsantos.myreadings.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handleDataIntegrityViolation(DataIntegrityViolationException) — inalterado
    // handleNotFound(NotFoundException) — inalterado
    // handleMessageNotReadable(HttpMessageNotReadableException) — inalterado,
    //   inclui buildInvalidFormatDetail(...)
    // handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException) — inalterado
    // handleConstraintViolation(ConstraintViolationException) — inalterado,
    //   inclui lastPathNode(...)
    // handleValidation(MethodArgumentNotValidException) — inalterado
    // FieldErrorDetail (record privado) — inalterado, continua compartilhado
    //   pelos três handlers de validação acima
}
```

Saem: `handleEmailAlreadyInUse`, `handleBadCredentials`,
`handleJwtException`, `handleInvalidDateRange` — e os imports que só
existiam por causa deles (`EmailAlreadyInUseException`, `JwtException`,
`BadCredentialsException`, `InvalidDateRangeException`). O import de
`auth.EmailAlreadyInUseException` — a violação de fronteira apontada no
diagnóstico da spec (seção 2.2) — desaparece neste passo.

## 5. Migrações de banco (Flyway)

```
src/main/resources/db/migration/
├── user/
│   └── V1__create_users_table.sql   # conteúdo idêntico, só o caminho muda
└── book/
    └── V2__create_books_table.sql   # conteúdo idêntico, só o caminho muda
```

`application.yaml` — adicionar `locations` explícito à chave `flyway` já
existente:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration/user,classpath:db/migration/book
```

O Flyway resolve `V1`/`V2` pela numeração declarada no nome do arquivo,
não pelo diretório em que está — uma única tabela `flyway_schema_history`
por banco continua sendo usada independente de quantos `locations` estão
listados, e o histórico de migrações já aplicadas em qualquer ambiente
existente (dev local, CI) não é afetado por mover o arquivo de pasta.

## 6. Ordem de execução e commits

Mesma ordem de fases da spec (seção 6), detalhada por arquivo:

| # | Fase | Arquivos tocados | Commit |
|---|---|---|---|
| 1 | Criar `security/`: mover os 4 arquivos da seção 3.1, criar `SecurityExceptionHandler`, atualizar imports em `SecurityConfig` e `AuthService` (ainda em `auth/` nesta fase), mover os 2 testes de `auth/` para `security/` | `security/*.java` (4 movidos + 1 novo), `config/SecurityConfig.java`, `auth/AuthService.java`, `common/GlobalExceptionHandler.java` (remove `handleJwtException`), `src/test/.../security/*.java` (2 movidos) | `refactor(security): extrai infraestrutura de JWT para pacote compartilhado` |
| 2 | Mover `AuthController`, `AuthService`, `EmailAlreadyInUseException`, `dto/*` de `auth/` para `user/`; criar `UserExceptionHandler`; remover pacote `auth/` (vazio); mover `AuthControllerTest` para `user/` | `user/*.java` (7 movidos + 1 novo), `common/GlobalExceptionHandler.java` (remove `handleEmailAlreadyInUse`/`handleBadCredentials`), `src/test/.../user/AuthControllerTest.java` (movido) | `refactor(user): funde pacote auth no módulo user` |
| 3 | Mover `InvalidDateRangeException` de `common/` para `book/`; criar `BookExceptionHandler` | `book/InvalidDateRangeException.java` (movido), `book/BookExceptionHandler.java` (novo), `common/GlobalExceptionHandler.java` (remove `handleInvalidDateRange`) | `refactor(book): internaliza exceção de validação de datas no módulo` |
| 4 | Segmentar migrações Flyway | `src/main/resources/db/migration/user/V1__create_users_table.sql` (movido), `src/main/resources/db/migration/book/V2__create_books_table.sql` (movido), `application.yaml` | `refactor(build): segmenta migrações Flyway por módulo` |
| 5 | Atualizar `CLAUDE.md` e `.claude/rules/code-conventions.md` com a árvore de pacotes da seção 3.3 e a convenção de exception handler por módulo (seção 4) — **a cargo do agente `docs-plan-keeper`** | `CLAUDE.md`, `.claude/rules/code-conventions.md` | `docs: atualiza convenção de pacotes para refletir a modularização por vertical slice` |

Critério de "pronto" ao final de **cada** fase (não só ao final de tudo):

```
./gradlew build
```

verde (compilação + suíte de testes completa), e `git diff` da fase
mostrando só `rename`, `package`/import e o corpo mecânico dos métodos de
`@ExceptionHandler` movidos — nenhuma linha de regra de negócio reescrita.

## 7. Rastreabilidade com a spec

| Decisão da spec | Componente técnico |
|---|---|
| Princípio 2 — direção de dependência de mão única | Tabela de imports da seção 3.1 (`user`/`book` → `security`/`common`; nunca o inverso; `user` nunca importa `book` nem vice-versa) |
| Princípio 3 — cada módulo trata seus próprios erros | Seções 4.1–4.4 (quatro `@RestControllerAdvice`) |
| Princípio 4 — autenticação é infraestrutura compartilhada | Pacote `security/` (seção 3.3), incluindo `JwtTokenProvider` completo (emissão *e* validação seguem na mesma classe — ver nota abaixo) |
| Princípio 5 — `common` reduzido ao genérico | Seção 4.4 |
| Seção 4.2 da spec — migrações por módulo | Seção 5 deste documento |
| Seção 5, ponto 4 da spec — manter nomes de classe | Seção 3.1 (nenhuma classe renomeada) |

Nota de decisão (não estava explícita na spec): `JwtTokenProvider`
permanece uma única classe em `security/`, emitindo *e* validando token,
em vez de dividida em "emissor" (que seria lógica exclusiva de `user`) e
"validador" (infraestrutura compartilhada). As duas operações compartilham
o mesmo `SecretKey`/`expirationMinutes` injetados via `@Value`, e separá-las
exigiria duas classes consumindo a mesma configuração só para refletir uma
distinção que hoje não tem nenhum consumidor prático (não há, por exemplo,
um segundo serviço só-validador no código atual). Manter uma classe só
evita abstração especulativa; se uma decomposição real em microsserviços
vier a acontecer, extrair o validador nesse momento — com um motivo
concreto — é mais barato do que manter a divisão hoje sem uso.

## 8. Riscos técnicos e verificação

- **Ordem de resolução de `@RestControllerAdvice` com `basePackages`:** o
  Spring Boot escolhe o advice pelo pacote do **controller** que atende a
  requisição (não pelo pacote onde a exceção foi lançada). Confirmado por
  inspeção de código (seção 4.2/4.3) que, para toda exceção migrada, o
  controller que a desencadeia está sempre no mesmo pacote do seu novo
  handler — não há caso de uma exceção de `book` ser lançada a partir de
  uma requisição atendida por `AuthController`, ou vice-versa. Nenhum teste
  de integração deve mudar de resultado por causa disso; a suíte de
  `BookControllerTest`/`AuthControllerTest` já cobre o status/corpo de erro
  de cada exceção migrada e serve como verificação.
- **Testes com dependência de pacote:** confirmado por inspeção (seção 3.2)
  que nenhum teste afetado usa visibilidade package-private de uma classe
  de produção nem importa uma classe movida por FQN — mover só a linha
  `package` é suficiente, sem precisar adicionar nenhum `import` novo em
  teste.
- **Flyway com `locations` múltiplos:** validado pelos próprios testes de
  integração existentes, que já sobem o schema via Flyway num SQLite
  temporário (`@TempDir`) a cada execução — se uma tabela não existir por
  causa de um arquivo de migração no caminho errado, o primeiro teste que
  tocar a tabela falha imediatamente.
- **Import morto em `common/GlobalExceptionHandler.java`:** ao remover os
  quatro métodos da seção 4.4, os imports de `EmailAlreadyInUseException`,
  `JwtException`, `BadCredentialsException` e `InvalidDateRangeException`
  ficam sem uso — devem ser removidos no mesmo commit (o compilador não
  falha por import não usado, mas deixá-los reintroduziria a mesma
  violação de fronteira que esta refatoração elimina).
