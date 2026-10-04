# RF-01 — Extrair pacote `security`

**Tipo:** Refatoração técnica (sem mudança de comportamento) — não faz
parte do backlog de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seções 3.1, 4.1 e 6, fase 1)
**Depende de:** — (primeira fase da refatoração)

## Objetivo da refatoração

> Mover a infraestrutura de validação de JWT (`CurrentUser`,
> `JwtTokenProvider`, `JwtAuthenticationFilter`,
> `JwtAuthenticationEntryPoint`) para um pacote novo e compartilhado,
> `security`, hoje espalhada entre `common` e `auth`. O objetivo é deixar
> de tratar autenticação como lógica exclusiva do módulo `user` — validar
> um token é algo que qualquer módulo futuro replicaria de forma
> independente, não uma regra de negócio de identidade.

## Critérios de aceite

- [ ] `./gradlew build` passa (compilação + suíte de testes completa) sem
      nenhuma asserção de teste alterada.
- [ ] Nenhuma rota, status HTTP ou corpo de resposta muda — confirmado
      pelos testes de integração já existentes (`AuthControllerTest`,
      `JwtAuthenticationFilterTest`, `JwtAuthenticationIntegrationTest`).
- [ ] Pacote `dev.dfsantos.myreadings.security` existe com exatamente:
      `CurrentUser`, `JwtTokenProvider`, `JwtAuthenticationFilter`,
      `JwtAuthenticationEntryPoint`, `SecurityExceptionHandler`.
- [ ] `common/GlobalExceptionHandler.java` não importa mais `JwtException`
      nem contém `handleJwtException`.

## Tarefas

- [ ] Criar o pacote `security/` e mover `common/CurrentUser.java` para lá
      (só `package`, nenhuma outra mudança).
- [ ] Mover `auth/JwtTokenProvider.java` → `security/JwtTokenProvider.java`
      (só `package`).
- [ ] Mover `auth/JwtAuthenticationFilter.java` →
      `security/JwtAuthenticationFilter.java` (só `package`; o import de
      `JwtTokenProvider` deixa de ser necessário, mesmo pacote agora).
- [ ] Mover `auth/JwtAuthenticationEntryPoint.java` →
      `security/JwtAuthenticationEntryPoint.java` (só `package`).
- [ ] Criar `security/SecurityExceptionHandler.java`
      (`@RestControllerAdvice`, **sem** `basePackages` — é infraestrutura
      cross-cutting, não lógica de um módulo) com o método
      `handleJwtException` extraído de `common/GlobalExceptionHandler.java`
      (corpo idêntico ao plano técnico, seção 4.1).
- [ ] Remover `handleJwtException` e o import de
      `io.jsonwebtoken.JwtException` de `common/GlobalExceptionHandler.java`.
- [ ] Atualizar os imports em `config/SecurityConfig.java` para
      `dev.dfsantos.myreadings.security.JwtAuthenticationFilter` e
      `dev.dfsantos.myreadings.security.JwtAuthenticationEntryPoint`.
- [ ] Atualizar o import de `JwtTokenProvider` em `auth/AuthService.java`
      para `dev.dfsantos.myreadings.security.JwtTokenProvider`.
- [ ] Atualizar todo uso de `common.CurrentUser` (hoje em
      `book/BookService.java`) para `dev.dfsantos.myreadings.security.CurrentUser`.
- [ ] Mover `src/test/.../auth/JwtAuthenticationFilterTest.java` →
      `src/test/.../security/JwtAuthenticationFilterTest.java` (só `package`).
- [ ] Mover `src/test/.../auth/JwtAuthenticationIntegrationTest.java` →
      `src/test/.../security/JwtAuthenticationIntegrationTest.java` (só `package`).
- [ ] Rodar `./gradlew build` e confirmar suíte completa verde.

## Observações

- `auth/AuthController.java`, `auth/AuthService.java`,
  `auth/EmailAlreadyInUseException.java` e `auth/dto/*` permanecem em
  `auth/` até a [RF-02](20-fundir-auth-em-user.md) — esta tarefa só extrai
  a parte de infraestrutura de segurança, não consolida o módulo `user`
  ainda.
- Ver plano técnico, seção 8, para os riscos já mapeados (resolução de
  `@RestControllerAdvice`, dependência de pacote em teste) — nenhum deles
  se aplica a esta fase especificamente, mas valem para o conjunto.
