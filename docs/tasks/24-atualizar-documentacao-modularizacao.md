# RF-06 — Atualizar documentação de convenção de pacotes

**Tipo:** Documentação (sem mudança de código) — não faz parte do backlog
de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seção 6, fase 5)
**Depende de:** [RF-01](19-extrair-pacote-security.md), [RF-02](20-fundir-auth-em-user.md), [RF-03](21-internalizar-invalid-date-range-em-book.md), [RF-04](22-reduzir-global-exception-handler.md) e [RF-05](23-segmentar-migracoes-flyway.md) — só faz sentido depois que a árvore de pacotes nova existe de fato no código.

## Objetivo da refatoração

> Atualizar `CLAUDE.md` e `.claude/rules/code-conventions.md` para que a
> documentação do projeto reflita a árvore de pacotes final
> (`user`/`book`/`security`/`common`/`config`) e a convenção de exception
> handler por módulo — evitando que a documentação fique divergente do
> código, como já exige a convenção vigente do projeto.

**Esta tarefa é de competência do agente `docs-plan-keeper`**, não do
`spring-boot-dev` — não envolve alterar `src/`.

## Critérios de aceite

- [x] A árvore de pacotes em `CLAUDE.md` (seção "Convenções de
      pacote/estrutura") reflete exatamente o código após RF-01 a RF-05.
- [x] A árvore de pacotes em `.claude/rules/code-conventions.md` (seção
      "Estrutura de pacotes") reflete o mesmo.
- [x] `.claude/rules/code-conventions.md` registra a convenção de
      exception handler por módulo (cada módulo de negócio trata suas
      próprias exceções via `@RestControllerAdvice(basePackages = ...)`;
      `common` cobre só erros genéricos de framework; `security` é
      infraestrutura compartilhada e seu advice não é escopado por
      `basePackages`).
- [x] `CLAUDE.md` registra, na seção "Estado atual" (ou equivalente), que
      a refatoração de modularização (RF-01 a RF-05) foi concluída, com
      link para a spec e o plano técnico.

## Tarefas

- [x] Atualizar a árvore de pacotes em `CLAUDE.md`.
- [x] Atualizar a árvore de pacotes em `.claude/rules/code-conventions.md`.
- [x] Adicionar a convenção de exception handler por módulo em
      `.claude/rules/code-conventions.md`.
- [x] Adicionar nota de conclusão da refatoração em `CLAUDE.md`.
- [x] Marcar as checkboxes de RF-01 a RF-06 neste diretório
      (`docs/tasks/19-*.md` a `docs/tasks/24-*.md`) conforme forem
      verificadas no código, seguindo o mesmo padrão usado em US-01 a
      US-18.

## Observações

- Esta tarefa fecha o ciclo da refatoração: a spec (seção 6) já previa que
  essa atualização de documentação não é responsabilidade do mesmo agente
  que move código de produção.
