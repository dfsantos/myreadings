# US-01 — Criar conta

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#61-autenticação-apiv1auth)
**Depende de:** Nenhuma

## História de usuário

> Como usuário novo, quero criar uma conta para ter minha própria lista de leituras isolada das de outros usuários.

## Critérios de aceite

- [x] `POST /api/v1/auth/register` com e-mail e senha válidos retorna `201` com `{ id, email }`.
- [x] Registrar com e-mail já existente retorna `409`.
- [x] Registrar com senha menor que 8 caracteres retorna `400`.
- [x] Senha nunca é armazenada em texto plano.

## Tarefas

- [x] Adicionar dependências `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` ao `build.gradle`.
- [x] Criar migração Flyway `src/main/resources/db/migration/V1__create_users_table.sql` (tabela `users`: `id`, `email` UNIQUE, `password_hash`, `created_at`).
- [x] Criar entidade `User` (`user/User.java`) com `id` (UUID), `email`, `passwordHash`, `createdAt`.
- [x] Criar `UserRepository` (`user/UserRepository.java`) com `findByEmail(String email)`.
- [x] Criar `SecurityConfig` (`config/SecurityConfig.java`) com bean `PasswordEncoder` (`BCryptPasswordEncoder`).
- [x] Criar DTO `RegisterRequest` (`auth/dto/RegisterRequest.java`) com `email` (`@Email @NotBlank`) e `password` (`@NotBlank @Size(min = 8)`).
- [x] Criar DTO de resposta `UserResponse`/`RegisterResponse` (`id`, `email`).
- [x] Implementar `AuthService.register(...)`: verifica unicidade do e-mail, gera hash da senha, persiste o `User`.
- [x] Implementar `AuthController.register` → `POST /api/v1/auth/register`, retorna `201`.
- [x] Criar `common/GlobalExceptionHandler.java` e mapear `DataIntegrityViolationException` → `409` (`ProblemDetail`).
- [x] Teste de integração: registro válido retorna `201` com `id` gerado.
- [x] Teste de integração: e-mail duplicado retorna `409`.
- [x] Teste de integração: senha curta retorna `400`.

## Observações

- A checagem de e-mail duplicado em `AuthService` é feita explicitamente via
  `UserRepository.findByEmail` antes do `INSERT`, não apenas via captura de
  `DataIntegrityViolationException`: o dialeto SQLite comunitário usado
  (`hibernate-community-dialects`) não traduz a violação da constraint
  `UNIQUE` dessa exceção, então depender só dela deixaria o `409` de e-mail
  duplicado sem cobertura. O handler de `DataIntegrityViolationException`
  no `GlobalExceptionHandler` foi mantido como defesa em profundidade contra
  condição de corrida entre a checagem e o `INSERT`.
- Dependências de build adicionais além das listadas na tarefa foram
  necessárias para a stack funcionar (`spring-boot-starter-validation`,
  `spring-boot-starter-flyway`, `hibernate-community-dialects`) — ver nota
  na seção 2 do plano técnico.
