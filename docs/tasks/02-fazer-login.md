# US-02 — Fazer login

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#61-autenticação-apiv1auth)
**Depende de:** [US-01 — Criar conta](01-criar-conta.md)

## História de usuário

> Como usuário cadastrado, quero fazer login para acessar meu catálogo pessoal.

## Critérios de aceite

- [x] `POST /api/v1/auth/login` com credenciais corretas retorna `200` com `{ accessToken, expiresInSeconds }`.
- [x] Login com e-mail inexistente ou senha incorreta retorna `401`, sem indicar qual dos dois está errado.
- [x] O token emitido contém o id do usuário como claim (`sub`) e expiração configurável.

## Tarefas

- [x] Criar `JwtTokenProvider` (`auth/JwtTokenProvider.java`): gera JWT HS256 com claim `sub = user.id`, `iat`, `exp`.
- [x] Adicionar configuração `app.jwt.secret` (via env var `JWT_SECRET`) e `app.jwt.expiration-minutes` (default `1440`) em `application.yml`.
- [x] Criar DTO `LoginRequest` (`auth/dto/LoginRequest.java`) com `email`/`password`.
- [x] Criar DTO `TokenResponse` (`auth/dto/TokenResponse.java`) com `accessToken`/`expiresInSeconds`.
- [x] Implementar `AuthService.login(...)`: busca `User` por e-mail, valida senha com `PasswordEncoder.matches`, gera token via `JwtTokenProvider`.
- [x] Lançar `BadCredentialsException` em caso de e-mail não encontrado ou senha incorreta (mesma exceção para os dois casos).
- [x] Implementar `AuthController.login` → `POST /api/v1/auth/login`, retorna `200`.
- [x] Mapear `BadCredentialsException` → `401` no `GlobalExceptionHandler`.
- [x] Teste de integração: login com credenciais válidas retorna token.
- [x] Teste de integração: login com e-mail inexistente retorna `401`.
- [x] Teste de integração: login com senha incorreta retorna `401`.

## Observações

- `app.jwt.expiration-minutes` ficou configurável também via variável de
  ambiente opcional `JWT_EXPIRATION_MINUTES` (default `1440`), seguindo o
  mesmo padrão já usado para `JWT_SECRET`/`DB_PATH`; a tarefa original só
  pedia o default fixo. Documentado em
  [plano técnico §8](../plans/catalogo-de-leituras-backend.md#8-configuração-applicationyml).
- `JwtTokenProvider` fixa `Jwts.SIG.HS256` explicitamente no `signWith`
  para garantir o algoritmo pedido independentemente do tamanho do
  `app.jwt.secret` configurado — detalhe de implementação não pedido
  literalmente na tarefa, documentado em
  [plano técnico §6.1](../plans/catalogo-de-leituras-backend.md#61-autenticação-apiv1auth).
