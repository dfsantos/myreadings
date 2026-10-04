# Plano Técnico — Regras ArchUnit para as convenções de código (myreadings)

**Status:** Rascunho v1
**Data:** 2026-10-04
**Spec de origem:** [`docs/specs/archunit-regras-convencoes.md`](../specs/archunit-regras-convencoes.md)

Este documento traduz a spec em uma formulação concreta, na API do
ArchUnit 1.5.0, para cada uma das 9 convenções listadas na seção 3.1 da
spec — é o *como*, que a spec deliberadamente deixou para este documento
(seção 8 da spec). Todas as assinaturas de método citadas abaixo foram
confirmadas por inspeção direta do bytecode de
`com.tngtech.archunit:archunit:1.5.0` e
`com.tngtech.archunit:archunit-junit5-api:1.5.0` (via `javap`), não
assumidas de memória — ver seção 6 para o detalhe de como cada rule foi
validada. Nenhum passo deste plano toca `src/main`: é adição pura de um
único arquivo novo em `src/test/java`.

## 1. Decisão técnica adicional não fechada pela spec

A spec (seção 6) fecha todos os pontos de produto/processo, mas deixa
explicitamente para este plano "a formulação exata de cada uma das 9
regras na API do ArchUnit" (seção 8). Ao formular, surgiu um ponto técnico
que a spec não cobre e que decido aqui:

| Ponto técnico | Decisão | Justificativa |
|---|---|---|
| `@AnalyzeClasses` deve importar só `src/main` ou também `src/test`? | **Só `src/main`**, via `importOptions = ImportOption.DoNotIncludeTests.class` | A spec enuncia repetidamente "classe de produção" (seção 4, critério de aceite; seção 3.1, item 5). Sem esse filtro, o `@AnalyzeClasses(packages = "dev.dfsantos.myreadings")` padrão importa **todas** as classes do pacote no classpath de teste, incluindo as próprias classes de teste (`BookControllerTest`, `AuthControllerTest`, `ArchitectureTest` etc.) — hoje elas já residem nos pacotes permitidos pela regra 5 (nenhuma violação), mas deixar o filtro de fora tornaria a regra 5 e a regra de dependência entre módulos (regras 6/7) frágeis a qualquer classe de apoio de teste futura (ex.: um pacote `testsupport` ou uma fixture em `book.dto` só para teste) que não tem nenhuma relação com a convenção de arquitetura de produção que estamos verificando. `ImportOption.DoNotIncludeTests` é a opção pronta da própria biblioteca (reconhece o padrão de diretório de output do Gradle, `build/classes/java/test`). |

> **Correção pós-implementação (RF-07, `docs/tasks/25-regras-archunit-convencoes.md`):**
> a referência original acima — `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS.class`
> — estava incorreta: `DO_NOT_INCLUDE_TESTS` é uma constante do enum
> `ImportOption.Predefined`, não uma classe, e não compila como valor de
> `importOptions()` (que exige `Class<? extends ImportOption>[]`). A API
> real do ArchUnit 1.5.0, confirmada via `javap` no jar real durante a
> implementação, é a classe separada
> `com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests` —
> já corrigida no texto e no bloco de código abaixo, e é o que está em
> uso em `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`.

Confirmado por `javap` que `@AnalyzeClasses` aceita
`importOptions()` como `Class<? extends ImportOption>[]`, então a anotação
final da classe de teste é:

```java
@AnalyzeClasses(
        packages = "dev.dfsantos.myreadings",
        importOptions = ImportOption.DoNotIncludeTests.class
)
```

## 2. Escopo

Um único arquivo novo:

```
src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java
```

Nenhuma mudança em `src/main`, `build.gradle` ou `application.yaml` — a
dependência `archunit-junit5` e o `useJUnitPlatform()` já existem (ver
`CLAUDE.md` → "Stack confirmada" → "Governança de arquitetura").

## 3. Limite da API confirmado por inspeção (não por memória)

Como o ambiente de implementação pode estar offline em relação ao Gradle
(cache de dependências vazio), a validação abaixo foi feita baixando os
três artefatos relevantes diretamente do Maven Central e inspecionando as
classes com `javap -p`:

- `archunit-1.5.0.jar` — API fluente (`ArchRuleDefinition`, `ClassesThat`,
  `ClassesShould`, `GivenClassesConjunction`, `ClassesShouldConjunction`,
  `ArchCondition`, `ConditionEvents`, `SimpleConditionEvent`,
  `DescribedPredicate`, `JavaClass`, `JavaAnnotation`,
  `ImportOption.Predefined`).
- `archunit-junit5-api-1.5.0.jar` — anotações `@AnalyzeClasses`
  (`packages()`, `importOptions()` etc.) e `@ArchTest` (marcador, sem
  atributos).
- `archunit-junit5-engine-1.5.0.jar` — o `TestEngine` que resolve campos
  `@ArchTest`. **Achado relevante que mudou o desenho deste plano:** o
  bytecode de `ArchUnitTestDescriptor.resolveChildren` mostra que um campo
  anotado com `@ArchTest` só é tratado como:
  (a) um campo do tipo `ArchTests` (classe de composição de regras — não
  usada aqui), via `resolveArchRules`; ou
  (b) um `checkcast` direto para `ArchRule` (`com.tngtech.archunit.lang.ArchRule`),
  **nunca** para `ArchRule[]`.
  Ou seja: **não existe suporte a campo `ArchRule[]` anotado com
  `@ArchTest`** nesta versão — cada campo é exatamente uma regra. Isso
  invalidaria um desenho inicial (um campo por convenção, usando array
  para a convenção 6, que precisa de duas regras simétricas) e é a razão
  pela qual a seção 4.6 abaixo usa **dois** campos `@ArchTest` para a
  convenção 6, em vez de um array — ver tabela da seção 5.

## 4. Formulação de cada uma das 9 convenções

Todas as constantes de pacote usadas abaixo:

```java
private static final String ROOT = "dev.dfsantos.myreadings";
private static final String USER_PACKAGE = ROOT + ".user";
private static final String BOOK_PACKAGE = ROOT + ".book";
private static final String COMMON_PACKAGE = ROOT + ".common";
private static final String SECURITY_PACKAGE = ROOT + ".security";
```

### 4.1 — Controller não acessa Repository diretamente

```java
@ArchTest
static final ArchRule controllersMustNotAccessRepositoriesDirectly = noClasses()
        .that().haveSimpleNameEndingWith("Controller")
        .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
        .because("Controller não deve acessar Repository diretamente — "
                + "regra de negócio pertence ao Service (code-conventions.md, seção Camadas)");
```

`dependOnClassesThat()` (sem predicado, retornando `ClassesThat<ClassesShouldConjunction>`)
é o método confirmado em `ClassesShould` — cobre qualquer tipo de
dependência (campo, parâmetro de método, tipo de retorno, variável local),
mais amplo que só "injeção por campo", o que é desejável: nenhuma forma de
acesso a um `*Repository` a partir de um `*Controller` é permitida.

### 4.2 — Controller não depende de `security.CurrentUser`

```java
@ArchTest
static final ArchRule controllersMustNotDependOnCurrentUser = noClasses()
        .that().haveSimpleNameEndingWith("Controller")
        .should().dependOnClassesThat().haveFullyQualifiedName(SECURITY_PACKAGE + ".CurrentUser")
        .because("CurrentUser.id() só deve ser chamado pela camada de Service "
                + "(code-conventions.md, convenção obrigatória desde a US-04)");
```

### 4.3 — Exceção de módulo de negócio não mora em `common`

Formulação por "lista de exceções genéricas permitidas", conforme decisão
4(a) da spec (seção 6, ponto 4):

```java
@ArchTest
static final ArchRule onlyKnownGenericExceptionsMayLiveInCommon = classes()
        .that().haveSimpleNameEndingWith("Exception")
        .and().resideInAPackage(COMMON_PACKAGE)
        .should().haveSimpleName("NotFoundException")
        .because("common só deve conter exceções genéricas de framework/aplicação, "
                + "nunca uma exceção de regra de negócio de user/book "
                + "(code-conventions.md, seção 'Exception handler por módulo')");
```

Nota: esta é a única regra de "lista explícita" hoje, porque
`NotFoundException` é a única exceção genérica existente em `common`
(confirmado pela spec, seção 5). `ClassesShould` não expõe um
`haveSimpleNameIn(List<String>)` nativo — se `common` vier a ganhar uma
segunda exceção genérica legítima, trocar `.haveSimpleName(...)` por um
`DescribedPredicate` customizado (`DescribedPredicate.describe(desc,
javaClass -> ALLOWED_NAMES.contains(javaClass.getSimpleName()))`) é a
extensão natural, não um redesenho.

A parte (b) da decisão 4 da spec ("`*Exception` de `user`/`book` não deve
ser importada por outro módulo de negócio") **não** gera uma regra
própria — é estruturalmente idêntica às regras 4.6a/4.6b (nenhuma
dependência cruzada `user`↔`book`, de qualquer classe, não só exceções) e
ganharia zero cobertura adicional por duplicar a mesma asserção com um
filtro de nome mais restrito. Registrado aqui para rastreabilidade, não
como lacuna.

### 4.4 — Exception handler por módulo

Esta é a única das 9 que não é expressável com a sintaxe fluente pura
(`that()...should()...`), porque o "resultado esperado" depende do próprio
pacote da classe analisada (classes em `user`/`book` exigem
`basePackages` apontando para si mesmas; classes em `common`/`security`
exigem a ausência de `basePackages`). Usa um `ArchCondition<JavaClass>`
customizado, API confirmada em `com.tngtech.archunit.lang.ArchCondition`
(construtor `ArchCondition(String, Object...)`, método abstrato
`check(T, ConditionEvents)`) e `com.tngtech.archunit.lang.SimpleConditionEvent`
(`violated(Object, String)` / `satisfied(Object, String)`):

```java
@ArchTest
static final ArchRule exceptionHandlersMustBeScopedToTheirOwnModule = classes()
        .that().haveSimpleNameEndingWith("ExceptionHandler")
        .should(beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule())
        .because("UserExceptionHandler/BookExceptionHandler devem usar basePackages do "
                + "próprio módulo; GlobalExceptionHandler/SecurityExceptionHandler devem "
                + "ser agnósticos de módulo, sem basePackages "
                + "(code-conventions.md, seção 'Exception handler por módulo')");

private static ArchCondition<JavaClass> beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule() {
    return new ArchCondition<JavaClass>(
            "estar anotada com @RestControllerAdvice com basePackages == próprio pacote, "
                    + "se for de user/book, ou sem basePackages, caso contrário") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            RestControllerAdvice annotation = javaClass.tryGetAnnotationOfType(RestControllerAdvice.class)
                    .orElse(null);
            if (annotation == null) {
                events.add(SimpleConditionEvent.violated(javaClass,
                        javaClass.getFullName() + " não está anotada com @RestControllerAdvice"));
                return;
            }

            boolean isBusinessModuleHandler = javaClass.getPackageName().equals(USER_PACKAGE)
                    || javaClass.getPackageName().equals(BOOK_PACKAGE);
            String[] basePackages = annotation.basePackages();

            boolean satisfied = isBusinessModuleHandler
                    ? basePackages.length == 1 && basePackages[0].equals(javaClass.getPackageName())
                    : basePackages.length == 0;

            String message = satisfied
                    ? javaClass.getFullName() + " está corretamente escopada"
                    : javaClass.getFullName() + " tem basePackages " + Arrays.toString(basePackages)
                            + ", esperado " + (isBusinessModuleHandler
                                    ? "[" + javaClass.getPackageName() + "]"
                                    : "nenhum (handler global)");
            events.add(new SimpleConditionEvent(javaClass, satisfied, message));
        }
    };
}
```

`JavaClass.tryGetAnnotationOfType(Class<A>)` (confirmado em
`com.tngtech.archunit.core.domain.properties.HasAnnotations`, interface
implementada por `JavaClass`) retorna o proxy dinâmico da anotação real —
`annotation.basePackages()` chama o atributo do `@RestControllerAdvice`
com tipagem forte, sem precisar navegar o mapa genérico
`JavaAnnotation.getProperties()`.

Esta formulação é deliberadamente genérica por pacote (não uma lista fixa
de 4 nomes de classe): qualquer `*ExceptionHandler` futuro em `user`/`book`
precisa seguir o mesmo padrão automaticamente, e qualquer um em
`common`/`security` também — consistente com a regra de convenção, que é
sobre o papel do pacote, não sobre os 4 nomes de classe de hoje.

### 4.5 — Estrutura de pacotes fechada

```java
@ArchTest
static final ArchRule productionClassesMustResideInTheDefinedPackageStructure = classes()
        .should().resideInAnyPackage(
                ROOT,
                ROOT + ".config",
                SECURITY_PACKAGE,
                USER_PACKAGE,
                USER_PACKAGE + ".dto",
                BOOK_PACKAGE,
                BOOK_PACKAGE + ".dto",
                COMMON_PACKAGE
        )
        .because("a árvore de pacotes é fechada: config, security, user(.dto), book(.dto), "
                + "common, mais MyreadingsApplication na raiz — nenhum pacote 'auth' "
                + "remanescente (code-conventions.md, seção 'Estrutura de pacotes')");
```

`resideInAnyPackage(String...)` usa comparação exata por pacote listado
(sem `..`), por isso cada subpacote (`user.dto`, `book.dto`) precisa estar
listado explicitamente — é exatamente o comportamento desejado para uma
lista "fechada": um pacote novo não listado (ex.: um futuro `auth`
reintroduzido por engano) quebra a regra.

### 4.6 — Direção de dependência entre módulos de negócio (duas regras)

Ver seção 3 (achado sobre `ArchRule[]` não suportado por campo): esta
convenção única se traduz em **dois** campos `@ArchTest`, simétricos:

```java
@ArchTest
static final ArchRule userModuleMustNotDependOnBookModule = noClasses()
        .that().resideInAPackage(USER_PACKAGE + "..")
        .should().dependOnClassesThat().resideInAPackage(BOOK_PACKAGE + "..")
        .because("user e book são módulos de negócio irmãos, sem contrato explícito "
                + "entre eles hoje (code-conventions.md, seção 'Estrutura de pacotes')");

@ArchTest
static final ArchRule bookModuleMustNotDependOnUserModule = noClasses()
        .that().resideInAPackage(BOOK_PACKAGE + "..")
        .should().dependOnClassesThat().resideInAPackage(USER_PACKAGE + "..")
        .because("user e book são módulos de negócio irmãos, sem contrato explícito "
                + "entre eles hoje (code-conventions.md, seção 'Estrutura de pacotes')");
```

`resideInAPackage(USER_PACKAGE + "..")` usa o sufixo `..` do ArchUnit
("este pacote e todos os subpacotes"), necessário para cobrir
`user.dto`/`book.dto` também, diferente da regra 4.5 (que precisa de
correspondência exata por ser uma lista fechada).

### 4.7 — `common`/`security` não dependem de módulo de negócio

```java
@ArchTest
static final ArchRule commonAndSecurityMustNotDependOnBusinessModules = noClasses()
        .that().resideInAnyPackage(COMMON_PACKAGE + "..", SECURITY_PACKAGE + "..")
        .should().dependOnClassesThat().resideInAnyPackage(USER_PACKAGE + "..", BOOK_PACKAGE + "..")
        .because("common e security são infraestrutura compartilhada; a direção de "
                + "dependência é sempre user/book -> security/common, nunca o inverso "
                + "(plano de modularização, princípio 2)");
```

### 4.8 — Repository é interface `JpaRepository`

```java
@ArchTest
static final ArchRule repositoriesMustBeJpaRepositoryInterfaces = classes()
        .that().haveSimpleNameEndingWith("Repository")
        .should().beInterfaces()
        .andShould().beAssignableTo(JpaRepository.class)
        .because("Repository é sempre uma interface Spring Data, sem lógica própria "
                + "(code-conventions.md, seção Camadas)");
```

Não é verificado estruturalmente o uso adicional de
`JpaSpecificationExecutor` — a spec já registra isso como "quando
aplicável" (seção 3.1, item 8), uma condição de negócio (só repositórios
com filtro dinâmico precisam dela) que não tem um sinal estrutural capaz
de diferenciar "aplicável" de "não aplicável" sem conhecimento de domínio;
`BookRepository` já estende as duas interfaces hoje (confirmado na seção 5
da spec), mas a regra ArchUnit generaliza apenas a parte comum a 100% dos
repositórios (`JpaRepository`).

### 4.9 — Nomenclatura de DTO

```java
@ArchTest
static final ArchRule dtosMustBeNamedRequestOrResponse = classes()
        .that().resideInAnyPackage(USER_PACKAGE + ".dto", BOOK_PACKAGE + ".dto")
        .should().haveSimpleNameEndingWith("Request")
        .orShould().haveSimpleNameEndingWith("Response")
        .because("DTO de request termina em Request, DTO de resposta termina em Response "
                + "(code-conventions.md, seção Nomenclatura)");
```

## 5. Mapa convenção → campo(s) `@ArchTest`

| # | Convenção (spec, seção 3.1) | Campo(s) `@ArchTest` |
|---|---|---|
| 1 | Controller não acessa Repository | `controllersMustNotAccessRepositoriesDirectly` |
| 2 | Controller não depende de `CurrentUser` | `controllersMustNotDependOnCurrentUser` |
| 3 | Exceção de módulo não mora em `common` | `onlyKnownGenericExceptionsMayLiveInCommon` |
| 4 | Exception handler por módulo | `exceptionHandlersMustBeScopedToTheirOwnModule` |
| 5 | Estrutura de pacotes fechada | `productionClassesMustResideInTheDefinedPackageStructure` |
| 6 | Sem dependência cíclica `user`↔`book` | `userModuleMustNotDependOnBookModule` **+** `bookModuleMustNotDependOnUserModule` |
| 7 | `common`/`security` não dependem de `user`/`book` | `commonAndSecurityMustNotDependOnBusinessModules` |
| 8 | Repository é `JpaRepository` | `repositoriesMustBeJpaRepositoryInterfaces` |
| 9 | Nomenclatura de DTO | `dtosMustBeNamedRequestOrResponse` |

Total: **10 campos `@ArchTest`** para as 9 convenções (a convenção 6 usa 2
campos — ver achado da seção 3). Cada campo tem exatamente uma falha
possível e uma mensagem `.because(...)` citando a seção de
`code-conventions.md` de origem, conforme decisão 3 da spec (seção 6).

## 6. Esqueleto completo do arquivo

```java
package dev.dfsantos.myreadings;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "dev.dfsantos.myreadings",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String ROOT = "dev.dfsantos.myreadings";
    private static final String USER_PACKAGE = ROOT + ".user";
    private static final String BOOK_PACKAGE = ROOT + ".book";
    private static final String COMMON_PACKAGE = ROOT + ".common";
    private static final String SECURITY_PACKAGE = ROOT + ".security";

    // ... os 10 campos @ArchTest das seções 4.1 a 4.9 ...

    private static ArchCondition<JavaClass> beAnnotatedWithRestControllerAdviceScopedToOwnPackageWhenBusinessModule() {
        // ... corpo da seção 4.4 ...
    }
}
```

Observações sobre o esqueleto:

- Classe **sem** `public` (visibilidade padrão/package-private) e campos
  **sem** `public`, só `static final` — mesmo padrão de qualquer classe de
  teste JUnit 5 do projeto hoje (`BookControllerTest`,
  `JwtAuthenticationFilterTest`); o `ArchUnitTestEngine` resolve campos via
  reflexão (`ReflectionUtils.getAllFields`), não exige visibilidade
  `public`.
- `import static ArchRuleDefinition.classes` / `.noClasses` — os dois
  únicos métodos estáticos de entrada usados (nenhuma regra parte de
  `theClass()`/`methods()`/`fields()`).
- O método privado auxiliar da regra 4.4 não é anotado com `@ArchTest`
  (não é um campo, é um método `private static` comum) — o
  `ArchUnitTestEngine` só considera campos e métodos **anotados**
  (`ReflectionUtils.withAnnotation(ArchTest.class)`, confirmado na seção
  3), então um método auxiliar sem a anotação é invisível para o engine,
  exatamente como um método privado comum em qualquer outra classe de
  teste.

## 7. Ordem de execução

**Uma única fase.** Justificativa: a tarefa é puramente aditiva (nenhuma
regra depende de outra para compilar ou para ser avaliada — cada campo
`@ArchTest` é independente, e o `ArchUnitTestEngine` os executa todos na
mesma análise de classpath, reaproveitando o mesmo `JavaClasses`
importado uma única vez por classe de teste, conforme a nota da spec,
seção 6, ponto 2) e não há nenhuma dependência de ordem entre os 9 itens
da seção 3.1 — diferente da refatoração de modularização (RF-01 a RF-06),
que precisou de fases sequenciais porque cada uma alterava código de
produção que a fase seguinte dependia. Aqui, criar o arquivo inteiro de
uma vez, com as 10 regras, é estritamente mais simples e não há
nenhum artefato intermediário que precise ficar "verde" antes de outro.

| # | Fase | Arquivos tocados | Commit |
|---|---|---|---|
| 1 | Criar `ArchitectureTest.java` com as 10 regras (seções 4.1–4.9) | `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java` (novo) | `test: adiciona regras ArchUnit para as convenções de código documentadas` |

### Mensagem de commit — tipo escolhido e por quê

**Tipo: `test`** (não `build`). Pela tabela de
`.claude/rules/commit-conventions.md`: `build` é "mudança em build
(Gradle, dependências, toolchain)" — não é o caso aqui, porque a
dependência `archunit-junit5` e a configuração de `useJUnitPlatform()` já
foram adicionadas numa tarefa anterior (ver `CLAUDE.md` → "Stack
confirmada"); este commit não toca `build.gradle` nem nenhum arquivo de
configuração de build, só adiciona uma classe em `src/test/java`. `test` é
literalmente "adição/ajuste de teste, sem mudança de código de produção"
— descrição exata desta tarefa. Sem escopo entre parênteses: a mudança não
pertence a um módulo de negócio (`user`/`book`) nem à infraestrutura
(`security`/`common`), é transversal ao projeto inteiro — forçar um
escopo (ex.: `test(architecture)`) inventaria uma palavra de módulo que
não existe na árvore de pacotes do projeto.

## 8. Critério de verificação (cada fase e final)

Como há uma única fase, o critério abaixo é também o critério final:

```bash
./gradlew test --tests "dev.dfsantos.myreadings.ArchitectureTest"
```

Esperado: **verde**, 10 testes (um por campo `@ArchTest`) passando — nenhum
deveria falhar, porque a spec (seção 5) já confirmou por inspeção manual
que o código atual satisfaz as 9 convenções antes de este plano ser
escrito. Se qualquer um falhar, seguir a decisão 5 da spec (seção 6): não
afrouxar a regra, investigar se é um achado real de divergência (corrigir
`src/main`) ou um erro de formulação da regra (corrigir este plano e o
teste) — e registrar o achado na task correspondente.

Em seguida, confirmar que nada mais quebrou:

```bash
./gradlew build
```

Esperado: verde (compilação de `src/main` e `src/test` + suíte de testes
completa, incluindo `ArchitectureTest` junto do restante via o mesmo
`ArchUnitTestEngine` já registrado por `ServiceLoader` — nenhuma task
Gradle nova, nenhuma mudança em `build.gradle`).

## 9. Riscos técnicos

- **`@AnalyzeClasses` sem `importOptions` incluiria classes de teste na
  análise.** Mitigado pela decisão da seção 1 deste plano
  (`DO_NOT_INCLUDE_TESTS`). Sem essa opção, o pior cenário não seria uma
  regra falhando hoje (todas as classes de teste atuais já residem em
  pacotes permitidos — confirmado na seção 1), e sim uma regra *frágil*
  para qualquer classe de teste futura fora desses pacotes; a mitigação
  elimina o risco por completo, não apenas o reduz.
- **Campo `@ArchTest` de tipo `ArchRule[]` pareceria mais elegante para a
  convenção 6, mas não é suportado** nesta versão (achado da seção 3,
  confirmado por bytecode, não por documentação desatualizada). Mitigado
  por usar dois campos simétricos em vez de um array — sem perda de
  cobertura, só uma pequena duplicação textual entre as duas regras
  (aceitável: são a imagem especular uma da outra).
- **`JavaClass.tryGetAnnotationOfType(RestControllerAdvice.class)` exige
  que a anotação `@RestControllerAdvice` esteja presente em tempo de
  análise estática (bytecode), o que é sempre o caso** — anotações
  `RUNTIME`/`CLASS` retention (como as do Spring) são sempre visíveis ao
  importador do ArchUnit, que lê o `.class` compilado, não depende de
  carregar a classe em um `ClassLoader` nem do contexto Spring. Nenhuma
  dependência de contexto Spring é necessária para este teste, consistente
  com o critério de aceite da spec (seção 4: "análise estática de
  classpath, sem I/O externo, sem contexto Spring").
- **Precisão do `javap` como fonte de verdade.** As assinaturas citadas
  neste plano foram confirmadas baixando os jars reais de
  `archunit:1.5.0`, `archunit-junit5-api:1.5.0` e
  `archunit-junit5-engine:1.5.0` do Maven Central e inspecionando-os
  diretamente (não a partir de memória/treinamento) — ver seção 3. Ainda
  assim, a implementação (`spring-boot-dev`) deve compilar o arquivo antes
  de rodar os testes; qualquer typo de método neste plano aparece como
  erro de compilação imediato, não como falha de teste silenciosa.

## 10. Rastreabilidade com a spec

| Decisão da spec | Componente técnico deste plano |
|---|---|
| Seção 6, ponto 1 — classe única `ArchitectureTest` no pacote raiz | Seção 6 (esqueleto do arquivo) |
| Seção 6, ponto 2 — `@AnalyzeClasses` + campos `@ArchTest` | Seção 6 (esqueleto do arquivo) |
| Seção 6, ponto 3 — 9 regras distintas, uma por convenção, com `.because(...)` | Seção 5 (mapa convenção → campo), seções 4.1–4.9 (todas usam `.because(...)`) |
| Seção 6, ponto 4 — exceção de módulo nasce no módulo, formulada por nome/pacote (não fluxo) | Seção 4.3 |
| Seção 6, ponto 5 — se uma regra revelar violação real, corrigir `src/main`, não afrouxar a regra | Seção 8 (critério de verificação) |
| Seção 4 — build vermelho em caso de violação, build já verde hoje | Seção 8 |
| Seção 7 — fora de escopo (CI separado, biblioteca compartilhada, métricas de performance) | Não há nenhum componente técnico correspondente neste plano — confirmando que nada foi adicionado além do pedido |

## 11. Próximo passo

Task única em `docs/tasks/` (próximo número sequencial do backlog,
25) cobrindo a criação de `ArchitectureTest.java` com as 10 regras
descritas neste plano, a cargo do `spring-boot-dev` — fora do escopo deste
documento (que é o plano técnico, não a task em si).
