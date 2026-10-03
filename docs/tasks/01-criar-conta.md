# US-01 — Criar conta

**Grupo da spec:** Autenticação e conta
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#61-autenticação-apiv1auth)
**Depende de:** Nenhuma

## História de usuário

> Como usuário novo, quero criar uma conta para ter minha própria lista de leituras isolada das de outros usuários.

## Critérios de aceite

- [ ] `POST /api/v1/auth/register` com e-mail e senha válidos retorna `201` com `{ id, email }`.
- [ ] Registrar com e-mail já existente retorna `409`.
- [ ] Registrar com senha menor que 8 caracteres retorna `400`.
- [ ] Senha nunca é armazenada em texto plano.

## Tarefas

- [ ] Adicionar dependências `spring-boot-starter-security`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` ao `build.gradle`.
- [ ] Criar migração Flyway `src/main/resources/db/migration/V1__create_users_table.sql` (tabela `users`: `id`, `email` UNIQUE, `password_hash`, `created_at`).
- [ ] Criar entidade `User` (`user/User.java`) com `id` (UUID), `email`, `passwordHash`, `createdAt`.
- [ ] Criar `UserRepository` (`user/UserRepository.java`) com `findByEmail(String email)`.
- [ ] Criar `SecurityConfig` (`config/SecurityConfig.java`) com bean `PasswordEncoder` (`BCryptPasswordEncoder`).
- [ ] Criar DTO `RegisterRequest` (`auth/dto/RegisterRequest.java`) com `email` (`@Email @NotBlank`) e `password` (`@NotBlank @Size(min = 8)`).
- [ ] Criar DTO de resposta `UserResponse`/`RegisterResponse` (`id`, `email`).
- [ ] Implementar `AuthService.register(...)`: verifica unicidade do e-mail, gera hash da senha, persiste o `User`.
- [ ] Implementar `AuthController.register` → `POST /api/v1/auth/register`, retorna `201`.
- [ ] Criar `common/GlobalExceptionHandler.java` e mapear `DataIntegrityViolationException` → `409` (`ProblemDetail`).
- [ ] Teste de integração: registro válido retorna `201` com `id` gerado.
- [ ] Teste de integração: e-mail duplicado retorna `409`.
- [ ] Teste de integração: senha curta retorna `400`.
