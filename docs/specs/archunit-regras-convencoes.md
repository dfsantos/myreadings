# Spec — Regras de arquitetura com ArchUnit

**Status:** Aprovada (fluxo sem pausa de aprovação nesta rodada, a pedido
explícito do usuário — ver nota ao final desta seção).
**Data:** 2026-10-04
**Tipo:** mudança técnica interna (governança de arquitetura), não feature
de usuário final — formato análogo ao de
[`docs/specs/refatoracao-modularizacao-vertical-slice.md`](refatoracao-modularizacao-vertical-slice.md).

> Nota sobre o processo: o fluxo padrão do projeto (`CLAUDE.md` → "Fluxo de
> desenvolvimento") prevê aprovação explícita do usuário depois desta spec
> e depois do plano técnico correspondente. Nesta rodada específica o
> usuário pediu para pular as duas pausas e seguir direto até a conclusão.
> Por isso, todos os pontos que normalmente ficariam em aberto para decisão
> do usuário (seção 6) já vêm com uma decisão tomada e justificada, para que
> o plano técnico seguinte não herde nenhuma pendência real.

## 1. Contexto e motivação

O projeto já documenta, em `.claude/rules/code-conventions.md`, um conjunto
de convenções de arquitetura (camadas, estrutura de pacotes, exception
handler por módulo, nomenclatura de DTO) que hoje são verificadas apenas
por revisão humana — do próprio desenvolvedor, do agente `spring-boot-dev`
ao implementar, e do agente `pr-reviewer` ao revisar um PR. Não há nenhum
mecanismo automatizado que impeça uma violação de convenção de entrar no
código sem alguém notar.

A dependência `com.tngtech.archunit:archunit-junit5:1.5.0` já foi
adicionada a `build.gradle` (`testImplementation`) numa tarefa anterior,
deliberadamente **sem** nenhuma regra escrita ainda — ver
`CLAUDE.md` → "Stack confirmada" → "Governança de arquitetura". Essa nota
já registra que a versão escolhida integra nativamente com o JUnit 5 via
`TestEngine` próprio (`ArchUnitTestEngine`, descoberto por `ServiceLoader`
junto do engine do Jupiter), e que o projeto já tem
`tasks.named('test') { useJUnitPlatform() }` e
`testRuntimeOnly 'org.junit.platform:junit-platform-launcher'` configurados
— ou seja, nenhuma mudança de configuração Gradle é necessária para esta
tarefa, só a classe de teste com as regras.

**Por que agora:** o backlog de produto (US-01 a US-18) e a refatoração de
modularização em vertical slices (RF-01 a RF-06) estão concluídos. As
convenções de camada e de módulo que motivaram a refatoração (ver
`docs/specs/refatoracao-modularizacao-vertical-slice.md`) estão
implementadas, mas continuam sem nenhuma rede de segurança automatizada
contra regressão — ex.: nada hoje impede um PR futuro de voltar a importar
uma exceção de módulo em `common`, ou de injetar um `*Repository`
diretamente num controller. Esta é a tarefa de "pagar a dívida" deixada
deliberadamente em aberto na nota do `CLAUDE.md`.

## 2. Objetivo

Traduzir em regras ArchUnit executáveis o subconjunto de convenções já
documentadas em `.claude/rules/code-conventions.md` que são verificáveis
estruturalmente (dependência entre pacotes, nomenclatura, anotação,
herança/implementação de interface) — não regras de estilo de código que
exigiriam análise semântica (essas continuam a cargo do `pr-reviewer` e de
revisão humana).

## 3. Escopo

### 3.1. Entra nesta rodada

Regras cobrindo exatamente as convenções já escritas em
`.claude/rules/code-conventions.md`, para a árvore de pacotes **hoje
existente** (`config`, `security`, `user`, `book`, `common`):

1. **Controller sem lógica de negócio / sem acesso a Repository.**
   Nenhuma classe cujo nome termina em `Controller` deve depender
   diretamente de uma classe cujo nome termina em `Repository`.
2. **Único chamador de `CurrentUser.id()` é a camada de Service.**
   Nenhuma classe cujo nome termina em `Controller` deve depender da
   classe `security.CurrentUser`. (O acesso em si — `Service` chama
   `CurrentUser.id()` — é o padrão positivo já vigente desde a US-04; a
   regra ArchUnit verifica a face negativa, que é a estruturalmente
   checável: controller não deve depender de `CurrentUser`.)
3. **Exceção de módulo de negócio nasce no próprio módulo.** Nenhuma
   classe cujo nome termina em `Exception` e reside em `user` ou `book`
   deve residir em `common` — formulada de forma checável como: toda
   classe que estende `RuntimeException` (ou qualquer subtipo de
   `Exception`) e que é lançada/capturada só dentro de um módulo de
   negócio deve estar fisicamente naquele pacote, nunca em `common`. Na
   prática, implementada como regra de **localização por sufixo**: classes
   `*Exception` em `common` só podem ser as hoje existentes e genéricas
   (`NotFoundException`) — ver detalhe de formulação técnica no plano.
4. **Exception handler por módulo.** `user.UserExceptionHandler` e
   `book.BookExceptionHandler` devem estar anotados com
   `@RestControllerAdvice` com `basePackages` apontando para o próprio
   módulo; `common.GlobalExceptionHandler` e
   `security.SecurityExceptionHandler` devem estar anotados com
   `@RestControllerAdvice` **sem** `basePackages` (agnósticos de módulo).
5. **Estrutura de pacotes fechada.** Toda classe de produção deve residir
   em um dos pacotes `config`, `security`, `user`, `book`, `common` (ou
   subpacote `dto` de `user`/`book`) — nenhuma classe solta no pacote raiz
   além de `MyreadingsApplication`, e nenhum pacote `auth` (confirma que a
   fusão da RF-02 não regride).
6. **Direção de dependência entre módulos de negócio.** `user` não deve
   depender de `book`, e `book` não deve depender de `user` — os dois
   módulos de negócio são irmãos, sem contrato explícito entre eles hoje.
7. **`common` e `security` não dependem de módulo de negócio.** Nenhuma
   classe em `common` ou `security` deve depender de classe em `user` ou
   `book` — a direção de dependência de mão única estabelecida na RF-01
   (seção 3, item 2 do plano de modularização).
8. **Repository é `JpaRepository`/`JpaSpecificationExecutor`, sem lógica de
   negócio.** Toda classe cujo nome termina em `Repository` deve ser uma
   `interface` que estende `JpaRepository` (e, quando aplicável,
   `JpaSpecificationExecutor`) — não uma classe concreta com lógica
   própria.
9. **Nomenclatura de DTO.** Toda classe no subpacote `dto` de `user` ou
   `book` deve ter nome terminando em `Request` ou `Response`.

### 3.2. Fora de escopo nesta rodada

- **Regras para pacotes/módulos que não existem ainda** (ex.: um módulo de
  negócio futuro além de `user`/`book`, ou uma separação física em módulos
  Gradle). Regra para o que não existe é regra hipotética sem como validar
  hoje — fica para quando o cenário existir de fato, mesmo princípio já
  registrado como "fora de escopo" na refatoração de modularização (seção
  7 do plano RF).
- **Regras de estilo/semântica que ArchUnit não verifica bem** (ex.: "o
  Service contém toda a regra de negócio" é uma afirmação positiva de
  *conteúdo*, não de *estrutura* — não há como uma regra ArchUnit atestar
  que uma validação cruzada específica está no lugar certo sem ler o corpo
  do método). Essas continuam cobertas por revisão humana e pelo
  `pr-reviewer`.
- **Regra para idioma de identificadores/comentários/mensagens de erro**
  (seção "Idioma" de `code-conventions.md`) — não é uma propriedade
  estrutural de arquitetura, está fora do que ArchUnit modela.
- **Regra para convenção de migração Flyway** (nomenclatura de arquivo,
  segmentação por módulo) — ArchUnit analisa bytecode/classes Java, não
  arquivos de recurso (`.sql`). Fora do alcance da ferramenta.
- **Regra para a convenção "Toda tabela tem `created_at`"** ou qualquer
  verificação de schema de banco — mesma limitação acima.
- **Regra para "todo endpoint novo exige teste de integração"** — ArchUnit
  pode verificar a *existência* de uma classe de teste correspondente a
  uma classe de produção em alguns cenários, mas isso não está documentado
  como convenção verificável hoje e adicionaria acoplamento frágil
  (nomenclatura de teste) sem benefício claro nesta rodada.

## 4. Critério de aceite / objetivo observável

- `./gradlew test` executa as regras ArchUnit automaticamente, sem task
  Gradle separada nem runner especial — consequência direta do
  `ArchUnitTestEngine` já registrado via `ServiceLoader` (nenhuma mudança
  de configuração Gradle necessária, só a classe de teste).
- Build fica **vermelho** se qualquer uma das 9 regras da seção 3.1 for
  violada, com mensagem de falha do ArchUnit identificando a classe e a
  regra violada (comportamento padrão da biblioteca, não precisa de
  customização de mensagem).
- No estado atual do código (confirmado nesta spec por inspeção direta
  antes de propor as regras — ver seção 5), **todas as 9 regras já
  passam** sem nenhuma mudança em `src/main`: esta tarefa é puramente
  aditiva (nova classe de teste), não corretiva. Nenhum código de produção
  deve precisar mudar para o build ficar verde.
- A suíte ArchUnit roda junto do restante de `./gradlew test`/`build`, sem
  aumentar o tempo de execução de forma perceptível (análise estática de
  classpath, sem I/O externo, sem contexto Spring).

## 5. Evidência de que o código atual já satisfaz as regras propostas

Checagem feita diretamente no código antes de escrever esta spec (não é
suposição):

- Nenhum `*Controller.java` em `src/main/java` importa qualquer
  `*Repository` — confirmado via busca textual em
  `book/BookController.java` (o único controller de negócio hoje, junto de
  `user/AuthController.java`).
- `CurrentUser.id()` só é chamado dentro de `book/BookService.java` (5
  ocorrências) — nenhum controller o referencia.
- A árvore de pacotes de `src/main/java/dev/dfsantos/myreadings` é
  exatamente `config/`, `security/`, `user/` (+ `user/dto/`), `book/` (+
  `book/dto/`), `common/`, mais `MyreadingsApplication.java` na raiz — sem
  pacote `auth/` remanescente.
- `user/UserExceptionHandler.java` e `book/BookExceptionHandler.java`
  existem com `@RestControllerAdvice(basePackages = "dev.dfsantos.myreadings.<modulo>")`;
  `common/GlobalExceptionHandler.java` e
  `security/SecurityExceptionHandler.java` existem sem `basePackages` —
  exatamente como descrito na seção 4.1 do plano de modularização.
- Todo DTO em `user/dto/` e `book/dto/` termina em `Request` ou `Response`
  (`LoginRequest`, `RegisterRequest`, `RegisterResponse`, `TokenResponse`,
  `BookCreateRequest`, `BookResponse`, `BookUpdateRequest`).
- `BookRepository`/`UserRepository` são interfaces estendendo
  `JpaRepository` (`BookRepository` também `JpaSpecificationExecutor`),
  sem implementação própria.

Esta evidência é o que permite que o critério de aceite da seção 4 declare
"build já fica verde sem mudar `src/main`" com confiança, em vez de como
hipótese a confirmar durante a implementação.

## 6. Pontos que pediriam decisão do usuário — decisão já tomada

Como esta rodada não terá pausa de aprovação entre spec/plano/implementação,
cada ponto abaixo já vem com a decisão tomada e a justificativa, para que o
plano técnico seguinte não carregue nenhuma pendência real.

1. **Nome e localização da classe de teste.**
   **Decisão:** `ArchitectureTest`, em
   `src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java` (pacote
   raiz, não dentro de `user`/`book`/`security`) — são regras do *projeto
   como um todo*, atravessam todos os módulos, não pertencem a um módulo
   de negócio específico. Alternativa descartada: uma classe de teste por
   módulo (`user/UserArchitectureTest`, `book/BookArchitectureTest`)  —
   rejeitada porque fragmentaria regras que, na maioria dos casos (ex.:
   "controller não acessa Repository"), são genéricas a qualquer módulo,
   gerando duplicação da mesma regra parametrizada por pacote.

2. **Estilo de API do ArchUnit: `@AnalyzeClasses` + campos `@ArchTest` vs.
   `ArchRuleDefinition` fluente em métodos `@Test`.**
   **Decisão:** classe anotada com
   `@AnalyzeClasses(packages = "dev.dfsantos.myreadings")` e cada regra
   como um campo `public static final ArchRule` anotado com `@ArchTest`.
   Justificativa: é o estilo recomendado pela própria documentação do
   ArchUnit para JUnit 5 (evita reimportar o classpath a cada método de
   teste, já que `@AnalyzeClasses` com campos compartilha a mesma
   `JavaClasses` importada uma única vez por classe), e deixa cada regra
   como uma unidade nomeada e independente no relatório de teste (uma
   falha por regra, não uma falha agregada de um método `@Test` genérico
   que chama várias regras em sequência).

3. **Granularidade: uma regra ArchUnit por convenção (9 campos) vs. menos
   regras agregando múltiplas convenções.**
   **Decisão:** 9 regras distintas, uma por item da seção 3.1, cada uma com
   nome descritivo e, quando a API do ArchUnit permitir, uma mensagem via
   `.because(...)` citando a convenção de origem em
   `code-conventions.md`. Justificativa: diagnóstico de falha mais claro
   (saber exatamente qual convenção quebrou, não "alguma regra de
   arquitetura falhou"); consistente com o padrão de testes já no projeto
   (um teste por critério de aceite, não um teste monolítico).

4. **Regra de "exceção de módulo nasce no módulo" (item 3 da seção 3.1) —
   como formular estruturalmente, já que ArchUnit não lê "onde a exceção é
   lançada" de forma trivial.**
   **Decisão:** formular como duas regras simples e checáveis por
   nome/pacote, em vez de uma análise de fluxo de lançamento: (a) nenhuma
   classe cujo nome termina em `Exception` reside em `common` **exceto**
   uma lista explícita de exceções genéricas conhecidas hoje
   (`NotFoundException`); (b) toda classe `*Exception` em `user` ou `book`
   não deve ser importada por nenhuma classe de outro módulo de negócio
   (reaproveita a regra de dependência cíclica do item 6). Isso cobre o
   caso concreto que a refatoração RF-03 corrigiu (`InvalidDateRangeException`
   movida de `common` para `book`) sem exigir análise de fluxo de dados,
   que o ArchUnit não faz.

5. **O que fazer se uma regra nova revelar uma violação real durante a
   implementação (plano técnico/tarefa), dado que a seção 4 desta spec
   declara que não deveria haver nenhuma).**
   **Decisão:** se isso ocorrer, é sinal de que a checagem manual desta
   spec (seção 5) deixou passar algo — a tarefa de implementação deve
   então corrigir o código de produção para se adequar à convenção já
   documentada (não afrouxar a regra ArchUnit para acomodar o código), e
   registrar o achado como pendência/observação na task correspondente em
   `docs/tasks/`, sinalizando ao usuário — mesmo princípio já estabelecido
   no papel do `docs-plan-keeper` para divergência entre documentação e
   código.

## 7. Não-metas (fora de escopo mesmo como consideração futura próxima)

- Rodar as regras ArchUnit como gate de CI separado do `./gradlew test`
  padrão — não há pipeline de CI configurado no projeto hoje (ver
  `.claude/rules/commit-conventions.md`, que já prevê `ci` como tipo de
  commit, mas nenhum workflow existe em `.github/`); escapa do escopo desta
  tarefa.
- Publicar as regras como biblioteca compartilhada/reutilizável fora deste
  repositório.
- Regras de performance/complexidade ciclomática — ArchUnit também suporta
  esse tipo de métrica, mas não foi pedido e não está em
  `code-conventions.md`.

## 8. Próximo passo

Plano técnico em `docs/plans/archunit-regras-convencoes.md`, detalhando a
formulação exata de cada uma das 9 regras na API do ArchUnit 1.5.0
(`@AnalyzeClasses`, `@ArchTest`, `ArchRuleDefinition`/`ArchCondition`), a
localização do único arquivo novo
(`src/test/java/dev/dfsantos/myreadings/ArchitectureTest.java`), e a
confirmação pós-implementação de que `./gradlew test` permanece verde.
</content>
