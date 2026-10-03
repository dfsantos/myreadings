# US-03 — Isolamento automático por usuário

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#7-segurança)
**Depende de:** [US-02 — Fazer login](02-fazer-login.md)

## História de usuário

> Como usuário autenticado, quero que toda operação sobre livros seja automaticamente restrita aos meus próprios registros, sem precisar informar um "meu usuário" manualmente.

## Critérios de aceite

- [x] Requisição sem token válido a qualquer rota de livros retorna `401`.
- [x] O `userId` usado para filtrar/gravar livros vem sempre do token autenticado, nunca de um parâmetro enviado pelo cliente.
- [x] Tentar acessar/editar/remover um livro de outro usuário retorna `404` (não `403`), implementado nas histórias de CRUD (US-04+), mas usando o mecanismo criado aqui.

## Tarefas

- [x] Implementar `JwtAuthenticationFilter` (`auth/JwtAuthenticationFilter.java`): extrai `Authorization: Bearer <token>`, valida via `JwtTokenProvider`, popula o `SecurityContext` com o `userId` autenticado.
- [x] Configurar `SecurityFilterChain` em `SecurityConfig`: `SessionCreationPolicy.STATELESS`, CSRF desabilitado, `JwtAuthenticationFilter` registrado antes do filtro padrão.
- [x] Liberar rotas públicas (`/api/v1/auth/**`, `/actuator/health`); exigir autenticação em todo o restante.
- [x] Criar um helper para obter o usuário autenticado atual em controllers/services (ex: `CurrentUser.id()` lendo do `SecurityContextHolder`, ou `@AuthenticationPrincipal` customizado).
- [x] Mapear falha de validação do token (`JwtException`) → `401` no `GlobalExceptionHandler`.
- [x] Teste de integração: requisição sem header `Authorization` a uma rota protegida retorna `401`.
- [x] Teste de integração: requisição com token inválido/expirado retorna `401`.
- [x] Teste de integração: requisição com token válido é autenticada e o `userId` fica disponível no contexto da aplicação.

## Observações / Pendências

- O terceiro critério de aceite (404 em livro de outro usuário) ficou
  **pendente** até existir um endpoint de CRUD de livro que o tornasse
  observável — não havia `GET/PATCH/DELETE /books/{id}` nas histórias
  anteriores (US-04 criou só o `POST`). A US-07 (`PATCH /api/v1/books/{id}`)
  foi a primeira a expor esse caminho, e o teste de integração
  `patchBookOfAnotherUserReturns404` em `BookControllerTest` (ver
  `docs/tasks/07-marcar-status-leitura.md`) cobre exatamente este critério:
  PATCH em livro de outro usuário retorna `404`. Critério marcado como
  concluído com essa evidência.
- As 8 tarefas técnicas e os 2 primeiros critérios de aceite foram
  verificados no código (`auth/JwtAuthenticationFilter.java`,
  `auth/JwtAuthenticationEntryPoint.java`, `common/CurrentUser.java`,
  `config/SecurityConfig.java`, `common/GlobalExceptionHandler.java`) e
  por `./gradlew test` (suíte completa verde, incluindo
  `JwtAuthenticationFilterTest` e `JwtAuthenticationIntegrationTest`).
