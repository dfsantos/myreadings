# RF-02 — Fundir pacote `auth` em `user`

**Tipo:** Refatoração técnica (sem mudança de comportamento) — não faz
parte do backlog de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seções 3.1, 4.2 e 6, fase 2)
**Depende de:** [RF-01 — Extrair pacote security](19-extrair-pacote-security.md)

## Objetivo da refatoração

> Consolidar `auth` e `user` em um único módulo de negócio — identidade do
> usuário (cadastro, login, emissão de token) — já que hoje são o mesmo
> domínio espalhado em dois pacotes Java sem fronteira real entre eles.
> Nenhuma classe muda de nome: só de pacote.

## Critérios de aceite

- [ ] `./gradlew build` passa sem nenhuma asserção de teste alterada.
- [ ] Nenhuma rota, status HTTP ou corpo de resposta muda — `/api/v1/auth/**`
      continua respondendo exatamente igual.
- [ ] O pacote `auth/` deixa de existir (vazio e removido).
- [ ] `common/GlobalExceptionHandler.java` não contém mais
      `handleEmailAlreadyInUse` nem `handleBadCredentials`.

## Tarefas

- [ ] Mover `auth/AuthController.java` → `user/AuthController.java` (só
      `package`; nome da classe e da rota `/api/v1/auth/**` inalterados).
- [ ] Mover `auth/AuthService.java` → `user/AuthService.java` (`package`;
      os imports de `User`/`UserRepository` deixam de ser necessários,
      mesmo pacote agora; o import de `JwtTokenProvider` já deve apontar
      para `dev.dfsantos.myreadings.security`, feito na RF-01).
- [ ] Mover `auth/EmailAlreadyInUseException.java` →
      `user/EmailAlreadyInUseException.java` (só `package`).
- [ ] Mover `auth/dto/LoginRequest.java`, `RegisterRequest.java`,
      `RegisterResponse.java`, `TokenResponse.java` → `user/dto/` (só
      `package` em cada um).
- [ ] Criar `user/UserExceptionHandler.java`
      (`@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.user")`)
      com os métodos `handleEmailAlreadyInUse` e `handleBadCredentials`
      extraídos de `common/GlobalExceptionHandler.java` (corpo idêntico ao
      plano técnico, seção 4.2).
- [ ] Remover `handleEmailAlreadyInUse`, `handleBadCredentials` e os
      imports de `EmailAlreadyInUseException`/`BadCredentialsException` de
      `common/GlobalExceptionHandler.java`.
- [ ] Remover o pacote `auth/` (deve estar vazio após os passos acima).
- [ ] Mover `src/test/.../auth/AuthControllerTest.java` →
      `src/test/.../user/AuthControllerTest.java` (só `package`).
- [ ] Rodar `./gradlew build` e confirmar suíte completa verde.

## Observações

- Confirmado por inspeção (plano técnico, seção 3.2) que
  `AuthControllerTest.java` não importa nenhuma classe de produção por
  nome totalmente qualificado — mover só a linha `package` é suficiente.
- O raciocínio de por que `basePackages = "dev.dfsantos.myreadings.user"`
  é seguro (o Spring resolve o advice pelo pacote do controller que atende
  a requisição, que aqui sempre coincide com onde a exceção é lançada) está
  detalhado no plano técnico, seção 4.2.
