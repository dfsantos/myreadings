# Convenção de Commits

Este projeto segue [Conventional Commits 1.0.0](https://www.conventionalcommits.org/).

## Formato

```
<tipo>[escopo opcional]: <descrição>

[corpo opcional]

[rodapé opcional]
```

- **Tipo**: sempre em inglês, minúsculo. Um dos listados abaixo.
- **Escopo**: opcional, entre parênteses, nome do pacote/módulo afetado em
  inglês minúsculo (ex.: `auth`, `book`, `security`, `docs`, `build`).
- **Descrição**: em português, modo imperativo, minúscula na primeira
  palavra, sem ponto final no fim. Até ~100 caracteres na primeira linha.
- **Breaking change**: marcar com `!` depois do tipo/escopo (ex.:
  `feat(book)!: ...`) e/ou um rodapé `BREAKING CHANGE: <explicação>`.

## Tipos permitidos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade visível para quem usa a API |
| `fix` | Correção de bug |
| `docs` | Mudança só em `docs/`, `CLAUDE.md` ou comentários — sem mudança de comportamento |
| `style` | Formatação, sem mudança de lógica (ex.: indentação, import ordenado) |
| `refactor` | Mudança de código que não altera comportamento externo nem corrige bug |
| `perf` | Mudança que melhora performance |
| `test` | Adição/ajuste de teste, sem mudança de código de produção |
| `build` | Mudança em build (Gradle, dependências, toolchain) |
| `ci` | Mudança em pipeline de CI |
| `chore` | Tarefa de manutenção que não se encaixa nos demais (ex.: `.gitignore`) |
| `revert` | Reverte um commit anterior |

## Exemplos

```
feat(book): adiciona endpoint de criação de livro

fix(auth): corrige expiração do token calculada em segundos em vez de minutos

docs(tasks): marca US-04 como concluída

refactor(book)!: renomeia BookFilter para BookSearchCriteria

BREAKING CHANGE: clientes que importavam BookFilter diretamente devem migrar para BookSearchCriteria
```

## Enforcement

A primeira linha de todo commit é validada automaticamente pelo git hook
`commit-msg` em `.githooks/commit-msg` contra o formato acima. O hook é
instalado automaticamente ao rodar qualquer comando Gradle (`core.hooksPath`
configurado em `settings.gradle`) — não exige passo manual de setup.

O hook bloqueia o commit localmente antes de criar o objeto de commit. Ele
pode ser contornado com `git commit --no-verify` (ex.: emergência) ou por
um commit feito fora deste clone (ex.: edição direto no GitHub). Por isso o
agente `pr-reviewer` também confere a mensagem de cada commit do PR contra
esta convenção como segunda camada — o hook evita o erro mais comum
localmente, o review é o backstop.
