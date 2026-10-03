# US-03 — Isolamento automático por usuário

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#7-segurança)
**Depende de:** [US-02 — Fazer login](02-fazer-login.md)

## História de usuário

> Como usuário autenticado, quero que toda operação sobre livros seja automaticamente restrita aos meus próprios registros, sem precisar informar um "meu usuário" manualmente.

## Critérios de aceite

- [ ] Requisição sem token válido a qualquer rota de livros retorna `401`.
- [ ] O `userId` usado para filtrar/gravar livros vem sempre do token autenticado, nunca de um parâmetro enviado pelo cliente.
- [ ] Tentar acessar/editar/remover um livro de outro usuário retorna `404` (não `403`), implementado nas histórias de CRUD (US-04+), mas usando o mecanismo criado aqui.

## Tarefas

- [ ] Implementar `JwtAuthenticationFilter` (`auth/JwtAuthenticationFilter.java`): extrai `Authorization: Bearer <token>`, valida via `JwtTokenProvider`, popula o `SecurityContext` com o `userId` autenticado.
- [ ] Configurar `SecurityFilterChain` em `SecurityConfig`: `SessionCreationPolicy.STATELESS`, CSRF desabilitado, `JwtAuthenticationFilter` registrado antes do filtro padrão.
- [ ] Liberar rotas públicas (`/api/v1/auth/**`, `/actuator/health`); exigir autenticação em todo o restante.
- [ ] Criar um helper para obter o usuário autenticado atual em controllers/services (ex: `CurrentUser.id()` lendo do `SecurityContextHolder`, ou `@AuthenticationPrincipal` customizado).
- [ ] Mapear falha de validação do token (`JwtException`) → `401` no `GlobalExceptionHandler`.
- [ ] Teste de integração: requisição sem header `Authorization` a uma rota protegida retorna `401`.
- [ ] Teste de integração: requisição com token inválido/expirado retorna `401`.
- [ ] Teste de integração: requisição com token válido é autenticada e o `userId` fica disponível no contexto da aplicação.
