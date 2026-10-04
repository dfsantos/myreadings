---
name: feature-workflow
description: >
  Use sempre que o usuário pedir uma funcionalidade nova, uma mudança de
  comportamento, uma correção estrutural ou uma refatoração para o projeto
  myreadings — mesmo que o pedido venha informal ("quero que o catálogo
  também...", "precisamos mudar como...", "vamos limpar/reorganizar..."),
  sem usar as palavras "spec" ou "plano". Conduz o trabalho pelo fluxo de 4
  estágios documentado em CLAUDE.md (pedido → spec → plano técnico → tasks),
  com dois gates de aprovação explícitos do usuário antes de qualquer código
  ser escrito, e garante que a escrita em docs/ e CLAUDE.md seja sempre
  delegada ao subagente docs-plan-keeper e o código em src/ ao
  spring-boot-dev. NÃO use para perguntas puramente informativas ("como
  funciona X?", "por que o Y foi feito assim?") nem para um pedido tão
  pequeno e já detalhado pelo usuário que criar spec/plano separados seria
  burocracia sem valor (ex.: "corrige esse typo na mensagem de erro") — use
  julgamento, mas no caso de dúvida prefira seguir o fluxo completo, porque
  o custo de pular um gate de aprovação (retrabalho, código que o usuário
  não pediu daquele jeito) é maior que o custo de confirmar demais.
---

# Fluxo de desenvolvimento do myreadings

Este projeto trata toda mudança de escopo não trivial como uma sequência de
três documentos — spec, plano técnico, tasks — cada um só escrito depois que
o anterior foi aprovado pelo usuário. A razão de ser rígido com os gates:
documentar *depois* de já ter implementado tende a racionalizar decisões já
tomadas em vez de dar ao usuário a chance real de redirecionar antes do
custo de escrever código. Já aconteceu nesta sessão duas vezes de forma
natural (o backlog original US-01 a US-18 e a refatoração RF-01 a RF-06) —
esta skill existe para que o próximo pedido siga o mesmo padrão sem precisar
ser reexplicado do zero.

## Visão geral dos 4 estágios

```
Pedido do usuário
      │
      ▼
 1. Gerar SPEC  (docs/specs/<slug>.md)         ──► usuário aprova ──┐
                                                                      │
      ┌───────────────────────────────────────────────────────────┘
      ▼
 2. Gerar PLANO TÉCNICO (docs/plans/<slug>.md)  ──► usuário aprova ──┐
                                                                      │
      ┌───────────────────────────────────────────────────────────┘
      ▼
 3. Gerar TASKS (docs/tasks/NN-<slug>.md, uma por história/tarefa)
      │
      ▼
 4. Implementar (spring-boot-dev), com dashboard/checkboxes
    sincronizados tarefa a tarefa (docs-plan-keeper) — nunca só ao final
```

Cada seta de "usuário aprova" é um ponto de parada real: não escreva o
documento do estágio seguinte, nem comece a implementar, antes da aprovação
explícita. Se o usuário pedir várias mudanças pequenas e relacionadas de
uma vez, ainda cabe tudo numa spec só — o fluxo é por *unidade de trabalho*
aprovada, não por arquivo.

## Estágio 1 — Spec (`docs/specs/`)

Objetivo: capturar o *quê* e o *porquê*, nunca o *como* (isso é o plano
técnico). Antes de escrever, leia pelo menos uma spec já existente no
projeto (`docs/specs/catalogo-de-leituras-backend.md` para uma feature de
produto, `docs/specs/refatoracao-modularizacao-vertical-slice.md` para uma
refatoração técnica) como modelo estrutural — não invente uma estrutura
nova, use a que o projeto já convencionou. Elementos que uma spec real
deste projeto sempre tem: contexto/motivação, escopo (o que entra e o que
fica explicitamente fora), critérios de aceite ou objetivo observável, e
pontos em aberto que dependem de decisão do usuário (nunca decididos por
conta própria — veja a nota de "pontos em aberto" na spec de refatoração
como exemplo de como registrar isso).

Depois de escrever a spec, pare e peça aprovação explícita ao usuário antes
de seguir para o plano técnico. Se o usuário pedir ajustes, edite a mesma
spec — não comece o plano com uma spec ainda em disputa.

## Estágio 2 — Plano técnico (`docs/plans/`)

Só começa depois da aprovação da spec. Objetivo: o *como* — decisões de
arquitetura, classes/arquivos afetados, de/para quando for refatoração,
ordem de execução e mensagens de commit sugeridas por fase. Use
`docs/plans/catalogo-de-leituras-backend.md` ou
`docs/plans/refatoracao-modularizacao-vertical-slice.md` como modelo
estrutural, dependendo se é uma feature nova ou uma refatoração. Todo
"ponto em aberto" que a spec deixou pendente de decisão do usuário precisa
estar resolvido aqui (ou ter sido resolvido numa troca com o usuário antes
de escrever o plano) — um plano técnico não deveria ter pontos em aberto
sobrando.

Depois de escrever o plano, pare e peça aprovação explícita antes de
seguir para as tasks.

## Estágio 3 — Tasks (`docs/tasks/`)

Só começa depois da aprovação do plano técnico. Cada task é um arquivo
`docs/tasks/NN-<slug-descritivo>.md`, numerado sequencialmente a partir do
maior número já usado no diretório (não reinicie a numeração nem reutilize
IDs — confira com `ls docs/tasks/` antes de escolher o próximo número).
Use um task file recente como modelo estrutural (`docs/tasks/18-remover-livro.md`
para uma história de produto, `docs/tasks/19-extrair-pacote-security.md`
para uma task de refatoração): título com o ID, objetivo, critérios de
aceite como checklist, lista de tarefas técnicas como checklist, uma seção
"Depende de" linkando outras tasks quando houver ordem de execução
obrigatória, e uma seção "Observações" para qualquer nuance que só aparece
durante a implementação.

## Coerência entre os três documentos

Spec, plano e tasks de uma mesma unidade de trabalho precisam contar a
mesma história em três níveis de detalhe — nunca editar um sem checar se os
outros dois ainda fazem sentido. Se a implementação revelar que algo
divergiu do plano (ex.: um endpoint que não existia e precisou ser criado,
como aconteceu na US-18), o documento certo a atualizar é o que descreve
aquele nível de detalhe (o plano técnico, não a spec), com uma nota
explicando o porquê — nunca deixe a divergência só implícita no código.

## Quem escreve o quê

- **`docs/specs/`, `docs/plans/`, `docs/tasks/`, `docs/dashboard.html`,
  `CLAUDE.md`** — sempre delegados ao subagente `docs-plan-keeper`. O
  assistente principal nunca edita esses arquivos diretamente; isso vale
  tanto para criar os três documentos desta skill quanto para qualquer
  atualização posterior.
- **Código em `src/`, dependências do `build.gradle`,
  `application.yaml`** — sempre delegados ao subagente `spring-boot-dev`,
  um pela vez por task (ou por fase, se o plano técnico definir fases com
  build verde entre elas — ver o padrão usado na refatoração RF-01 a
  RF-06, onde cada fase rodava `./gradlew build` antes do commit da
  seguinte).
- **Revisão de convenção antes do merge** — `pr-reviewer`, sob pedido do
  usuário ou antes de abrir PR.

## Dashboard e checkboxes — sincronizar a cada task, não só ao final

Depois de cada task implementada e com build verde, acione o
`docs-plan-keeper` para marcar as checkboxes daquela task em
`docs/tasks/` e atualizar `docs/dashboard.html` — mesmo que ainda faltem
outras tasks do mesmo plano. Não acumule várias tasks concluídas para
sincronizar tudo de uma vez só no final: o dashboard existe para
acompanhamento em tempo real (ver `CLAUDE.md`, seção sobre cadência do
`docs-plan-keeper`). Se várias tasks forem implementadas em sequência
rápida, está tudo bem acionar o `docs-plan-keeper` uma vez por task
concluída mesmo que isso signifique várias chamadas próximas uma da
outra — o custo de uma chamada extra é menor que o de o dashboard ficar
desatualizado durante o trabalho.

## Ao final de tudo

Quando a última task do plano for implementada e sincronizada, confirme
que não sobrou nenhum "ponto em aberto" sem resposta na spec/plano, rode a
suíte completa (`./gradlew build`) uma última vez, e só então ofereça
push/PR ao usuário — nunca abra PR ou dê push sem pedir primeiro (ver
seção de execução de ações no comportamento padrão do assistente).
