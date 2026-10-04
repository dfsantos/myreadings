# RF-06 — Regras ArchUnit para as convenções de código

**Tipo:** Governança de arquitetura/qualidade interna (sem mudança de
comportamento da API) — não faz parte do backlog de histórias de usuário
(US-01 a US-18, já concluído) nem da refatoração de modularização em
vertical slices (RF-01 a RF-05, já concluída), mas é a tarefa que paga a
dívida deliberadamente deixada em aberto por ela (ver `CLAUDE.md` →
"Stack confirmada" → "Governança de arquitetura").
**Spec de origem:** [`docs/specs/archunit-regras-convencoes.md`](../specs/archunit-regras-convencoes.md)
**Plano técnico:** [`docs/plans/archunit-regras-convencoes.md`](../plans/archunit-regras-convencoes.md)
**Depende de:** — nenhuma. É puramente aditiva em `src/test/java`, não
altera `src/main`, `build.gradle` nem `application.yaml` (a dependência
`archunit-junit5` já foi adicionada numa tarefa anterior, sem regra
escrita).

## Objetivo

Criar `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`
(`@AnalyzeClasses(packages = "dev.dfsantos.myreadings", importOptions =
ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.class)`) com 10 campos
`@ArchTest` cobrindo as 9 convenções de arquitetura já documentadas em
`.claude/rules/code-conventions.md` (a convenção 6 — direção de
dependência entre `user` e `book` — usa 2 campos simétricos em vez de um
array, porque o `ArchUnitTestEngine` 1.5.0 só reconhece campo do tipo
`ArchRule` único ou `ArchTests`, não `ArchRule[]` — ver plano técnico,
seção 3). Traduz em verificação automatizada o que hoje depende só de
revisão humana/`pr-reviewer`.

## Critérios de aceite

As 9 convenções abaixo (numeração e formulação exata na seção 3.1 da spec
e seção 4 do plano técnico) devem estar cobertas, cada uma por regra(s)
ArchUnit que passam sem nenhuma mudança em `src/main`:

- [ ] 1. Controller sem lógica de negócio / sem acesso a `Repository` —
      nenhuma classe `*Controller` depende de classe `*Repository`.
- [ ] 2. Único chamador de `CurrentUser.id()` é a camada de Service —
      nenhuma classe `*Controller` depende de `security.CurrentUser`.
- [ ] 3. Exceção de módulo de negócio nasce no próprio módulo — nenhuma
      classe `*Exception` em `common` além das genéricas já conhecidas
      (hoje só `NotFoundException`).
- [ ] 4. Exception handler por módulo — `UserExceptionHandler`/
      `BookExceptionHandler` anotados com `@RestControllerAdvice` com
      `basePackages` apontando para o próprio módulo;
      `GlobalExceptionHandler`/`SecurityExceptionHandler` anotados com
      `@RestControllerAdvice` **sem** `basePackages`.
- [ ] 5. Estrutura de pacotes fechada — toda classe de produção reside em
      `config`, `security`, `user`(`.dto`), `book`(`.dto`), `common` ou no
      pacote raiz (só `MyreadingsApplication`); nenhum pacote `auth`
      remanescente.
- [ ] 6. Direção de dependência entre módulos de negócio — `user` não
      depende de `book`, e `book` não depende de `user` (dois campos
      `@ArchTest` simétricos).
- [ ] 7. `common` e `security` não dependem de módulo de negócio —
      nenhuma classe em `common`/`security` depende de classe em
      `user`/`book`.
- [ ] 8. Repository é `JpaRepository` — toda classe `*Repository` é uma
      `interface` que estende `JpaRepository`.
- [ ] 9. Nomenclatura de DTO — toda classe em `user.dto`/`book.dto`
      termina em `Request` ou `Response`.

Critérios adicionais de verificação:

- [ ] `./gradlew test --tests "dev.dfsantos.myreadings.ArchitectureTest"`
      passa com os 10 campos `@ArchTest` verdes (um por regra, lembrando
      que a convenção 6 soma dois campos).
- [ ] `./gradlew build` completo passa em seguida, sem nenhuma asserção de
      teste existente alterada (confirma que a adição não teve efeito
      colateral no restante da suíte).
- [ ] Nenhuma mudança em `src/main`, `build.gradle` ou `application.yaml`
      — tarefa puramente aditiva em `src/test/java`.

## Tarefas

- [ ] Criar `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`
      com `@AnalyzeClasses(packages = "dev.dfsantos.myreadings",
      importOptions = ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.class)`,
      classe package-private, campos `static final ArchRule` package-private
      (não `public`) — mesmo padrão de visibilidade de qualquer outra
      classe de teste do projeto.
- [ ] Implementar o campo `controllersMustNotAccessRepositoriesDirectly`
      (convenção 1), conforme plano técnico seção 4.1.
- [ ] Implementar o campo `controllersMustNotDependOnCurrentUser`
      (convenção 2), conforme plano técnico seção 4.2.
- [ ] Implementar o campo `onlyKnownGenericExceptionsMayLiveInCommon`
      (convenção 3), conforme plano técnico seção 4.3.
- [ ] Implementar o campo
      `exceptionHandlersMustBeScopedToTheirOwnModule` (convenção 4) junto
      do `ArchCondition<JavaClass>` customizado
      `beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule()`
      (método auxiliar `private static`, sem `@ArchTest` — não é um campo),
      conforme plano técnico seção 4.4.
- [ ] Implementar o campo
      `productionClassesMustResideInTheDefinedPackageStructure`
      (convenção 5), conforme plano técnico seção 4.5.
- [ ] Implementar os dois campos simétricos
      `userModuleMustNotDependOnBookModule` e
      `bookModuleMustNotDependOnUserModule` (convenção 6), conforme plano
      técnico seção 4.6.
- [ ] Implementar o campo
      `commonAndSecurityMustNotDependOnBusinessModules` (convenção 7),
      conforme plano técnico seção 4.7.
- [ ] Implementar o campo `repositoriesMustBeJpaRepositoryInterfaces`
      (convenção 8), conforme plano técnico seção 4.8.
- [ ] Implementar o campo `dtosMustBeNamedRequestOrResponse` (convenção
      9), conforme plano técnico seção 4.9.
- [ ] Rodar
      `./gradlew test --tests "dev.dfsantos.myreadings.ArchitectureTest"`
      e confirmar os 10 campos verdes.
- [ ] Rodar `./gradlew build` completo e confirmar suíte inteira verde,
      sem nenhuma asserção de teste existente alterada.
- [ ] Commit único: `test: adiciona regras ArchUnit para as convenções de
      código documentadas` (tipo `test`, sem escopo entre parênteses — a
      mudança é transversal ao projeto, não pertence a um módulo de
      negócio nem à infraestrutura — ver plano técnico, seção 7, para a
      justificativa completa da escolha de tipo/escopo).

## Observações

- A spec (seção 5) e o plano técnico (seção 8) declaram, por inspeção
  manual prévia do código, que **nenhuma das 9 convenções deveria
  encontrar violação real** — esta tarefa é puramente aditiva. Se, ao
  implementar, qualquer uma das 10 regras falhar de fato, a decisão já
  tomada pela spec (seção 6, ponto 5) é: não afrouxar a regra ArchUnit
  para acomodar o código, e sim corrigir `src/main` para se adequar à
  convenção já documentada — e registrar o achado nesta seção
  "Observações", sinalizando ao usuário antes de seguir.
- Esta tarefa não tem dependência de nenhuma task anterior porque não
  toca `src/main`: pode ser implementada e verificada de forma totalmente
  independente das histórias US-01 a US-18 e das refatorações RF-01 a
  RF-05 — embora, na prática, só faça sentido ser feita depois delas, já
  que as convenções que ela verifica foram estabelecidas por elas
  (especialmente a estrutura de pacotes da RF-01/RF-02 e o exception
  handler por módulo da RF-01/RF-03/RF-04).
- `docs/dashboard.html` e a seção "Estado atual" de `CLAUDE.md` serão
  atualizados pelo `docs-plan-keeper` depois que o `spring-boot-dev`
  concluir a implementação e `./gradlew build` estiver verde — não fazem
  parte desta rodada de criação da task.
