# US-02 — Fazer login

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#61-autenticação-apiv1auth)
**Depende de:** [US-01 — Criar conta](01-criar-conta.md)

## História de usuário

> Como usuário cadastrado, quero fazer login para acessar meu catálogo pessoal.

## Critérios de aceite

- [ ] `POST /api/v1/auth/login` com credenciais corretas retorna `200` com `{ accessToken, expiresInSeconds }`.
- [ ] Login com e-mail inexistente ou senha incorreta retorna `401`, sem indicar qual dos dois está errado.
- [ ] O token emitido contém o id do usuário como claim (`sub`) e expiração configurável.

## Tarefas

- [ ] Criar `JwtTokenProvider` (`auth/JwtTokenProvider.java`): gera JWT HS256 com claim `sub = user.id`, `iat`, `exp`.
- [ ] Adicionar configuração `app.jwt.secret` (via env var `JWT_SECRET`) e `app.jwt.expiration-minutes` (default `1440`) em `application.yml`.
- [ ] Criar DTO `LoginRequest` (`auth/dto/LoginRequest.java`) com `email`/`password`.
- [ ] Criar DTO `TokenResponse` (`auth/dto/TokenResponse.java`) com `accessToken`/`expiresInSeconds`.
- [ ] Implementar `AuthService.login(...)`: busca `User` por e-mail, valida senha com `PasswordEncoder.matches`, gera token via `JwtTokenProvider`.
- [ ] Lançar `BadCredentialsException` em caso de e-mail não encontrado ou senha incorreta (mesma exceção para os dois casos).
- [ ] Implementar `AuthController.login` → `POST /api/v1/auth/login`, retorna `200`.
- [ ] Mapear `BadCredentialsException` → `401` no `GlobalExceptionHandler`.
- [ ] Teste de integração: login com credenciais válidas retorna token.
- [ ] Teste de integração: login com e-mail inexistente retorna `401`.
- [ ] Teste de integração: login com senha incorreta retorna `401`.
