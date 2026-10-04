# RF-07 — Regras ArchUnit para as convenções de código

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
ImportOption.DoNotIncludeTests.class)`) com 10 campos
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

- [x] 1. Controller sem lógica de negócio / sem acesso a `Repository` —
      nenhuma classe `*Controller` depende de classe `*Repository`.
- [x] 2. Único chamador de `CurrentUser.id()` é a camada de Service —
      nenhuma classe `*Controller` depende de `security.CurrentUser`.
- [x] 3. Exceção de módulo de negócio nasce no próprio módulo — nenhuma
      classe `*Exception` em `common` além das genéricas já conhecidas
      (hoje só `NotFoundException`).
- [x] 4. Exception handler por módulo — `UserExceptionHandler`/
      `BookExceptionHandler` anotados com `@RestControllerAdvice` com
      `basePackages` apontando para o próprio módulo;
      `GlobalExceptionHandler`/`SecurityExceptionHandler` anotados com
      `@RestControllerAdvice` **sem** `basePackages`.
- [x] 5. Estrutura de pacotes fechada — toda classe de produção reside em
      `config`, `security`, `user`(`.dto`), `book`(`.dto`), `common` ou no
      pacote raiz (só `MyreadingsApplication`); nenhum pacote `auth`
      remanescente.
- [x] 6. Direção de dependência entre módulos de negócio — `user` não
      depende de `book`, e `book` não depende de `user` (dois campos
      `@ArchTest` simétricos).
- [x] 7. `common` e `security` não dependem de módulo de negócio —
      nenhuma classe em `common`/`security` depende de classe em
      `user`/`book`.
- [x] 8. Repository é `JpaRepository` — toda classe `*Repository` é uma
      `interface` que estende `JpaRepository`.
- [x] 9. Nomenclatura de DTO — toda classe em `user.dto`/`book.dto`
      termina em `Request` ou `Response`.

Critérios adicionais de verificação:

- [x] `./gradlew test --tests "dev.dfsantos.myreadings.ArchitectureTest"`
      passa com os 10 campos `@ArchTest` verdes (um por regra, lembrando
      que a convenção 6 soma dois campos).
- [x] `./gradlew build` completo passa em seguida, sem nenhuma asserção de
      teste existente alterada (confirma que a adição não teve efeito
      colateral no restante da suíte).
- [x] Nenhuma mudança em `src/main`, `build.gradle` ou `application.yaml`
      — tarefa puramente aditiva em `src/test/java`.

## Tarefas

- [x] Criar `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`
      com `@AnalyzeClasses(packages = "dev.dfsantos.myreadings",
      importOptions = ImportOption.DoNotIncludeTests.class)`,
      classe package-private, campos `static final ArchRule` package-private
      (não `public`) — mesmo padrão de visibilidade de qualquer outra
      classe de teste do projeto.
- [x] Implementar o campo `controllersMustNotAccessRepositoriesDirectly`
      (convenção 1), conforme plano técnico seção 4.1.
- [x] Implementar o campo `controllersMustNotDependOnCurrentUser`
      (convenção 2), conforme plano técnico seção 4.2.
- [x] Implementar o campo `onlyKnownGenericExceptionsMayLiveInCommon`
      (convenção 3), conforme plano técnico seção 4.3.
- [x] Implementar o campo
      `exceptionHandlersMustBeScopedToTheirOwnModule` (convenção 4) junto
      do `ArchCondition<JavaClass>` customizado
      `beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule()`
      (método auxiliar `private static`, sem `@ArchTest` — não é um campo),
      conforme plano técnico seção 4.4.
- [x] Implementar o campo
      `productionClassesMustResideInTheDefinedPackageStructure`
      (convenção 5), conforme plano técnico seção 4.5.
- [x] Implementar os dois campos simétricos
      `userModuleMustNotDependOnBookModule` e
      `bookModuleMustNotDependOnUserModule` (convenção 6), conforme plano
      técnico seção 4.6.
- [x] Implementar o campo
      `commonAndSecurityMustNotDependOnBusinessModules` (convenção 7),
      conforme plano técnico seção 4.7.
- [x] Implementar o campo `repositoriesMustBeJpaRepositoryInterfaces`
      (convenção 8), conforme plano técnico seção 4.8.
- [x] Implementar o campo `dtosMustBeNamedRequestOrResponse` (convenção
      9), conforme plano técnico seção 4.9.
- [x] Rodar
      `./gradlew test --tests "dev.dfsantos.myreadings.ArchitectureTest"`
      e confirmar os 10 campos verdes.
- [x] Rodar `./gradlew build` completo e confirmar suíte inteira verde,
      sem nenhuma asserção de teste existente alterada.
- [x] Commit único: `test: adiciona regras ArchUnit para as convenções de
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

**Pós-implementação (verificado pelo `docs-plan-keeper`):**

- Implementação concluída pelo `spring-boot-dev` no commit `08b0f85`
  (`test: adiciona regras ArchUnit para as convenções de código
  documentadas`). Confirmado por leitura direta de
  `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`: os 10
  campos `@ArchTest` existem com exatamente os nomes e a formulação
  descritos no plano técnico (seções 4.1–4.9), cobrindo as 9 convenções.
  O relatório do `spring-boot-dev` (`./gradlew test --tests
  "dev.dfsantos.myreadings.ArchitectureTest"` com 10/10 verde, `./gradlew
  build` completo com 76 testcases e 0 falhas) não pôde ser re-executado
  nesta verificação porque o sandbox usado pelo `docs-plan-keeper` só tem
  JDK 21 disponível (o projeto exige toolchain Java 25 via Gradle, sem
  provisionamento automático configurado) — a verificação ficou restrita
  à leitura do código-fonte e à árvore de trabalho limpa (`git status`
  sem alterações pendentes sobre o commit já criado), não à re-execução
  do build.
- Divergência real encontrada durante a implementação: a referência
  `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.class`, usada nas seções 1
  e 6 do plano técnico e também citada na primeira tarefa desta checklist,
  não compila na API real do ArchUnit 1.5.0 (`DO_NOT_INCLUDE_TESTS` é uma
  constante de enum, não uma classe). A implementação usou corretamente
  `ImportOption.DoNotIncludeTests.class`, e o plano técnico e esta task
  foram corrigidos para refletir essa API (mesmo padrão de "documentar
  divergência revelada pela implementação" já usado em US-18 do backlog
  original).
- **Numeração:** esta task foi rotulada "RF-07" (em vez de "RF-06") para
  não colidir com [`docs/tasks/24-atualizar-documentacao-modularizacao.md`](24-atualizar-documentacao-modularizacao.md),
  que já é "RF-06" dentro da numeração RF-01–RF-06 da refatoração de
  modularização em vertical slices (spec/plano
  `refatoracao-modularizacao-vertical-slice.md`) — são duas iniciativas
  diferentes, só o prefixo "RF-" é compartilhado.
