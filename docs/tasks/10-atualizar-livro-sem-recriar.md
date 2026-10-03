# US-10 — Atualizar status/datas/nota sem recriar o livro

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#11-riscos-e-trade-offs-assumidos)
**Depende de:** [US-07](07-marcar-status-leitura.md), [US-08](08-registrar-datas-inicio-fim.md), [US-09](09-avaliar-livro.md)

## História de usuário

> Como usuário, quero atualizar o status/datas/nota de um livro já cadastrado sem precisar recriá-lo, para refletir o progresso ao longo do tempo.

## Critérios de aceite

- [x] Múltiplos `PATCH`s sequenciais no mesmo livro (um alterando `status`, outro `rating`, outro datas) resultam no estado acumulado esperado — nenhum PATCH anterior é perdido pelo seguinte.
- [x] Um `PATCH` enviando `{"rating": 4}` não altera `title`, `status`, `coverUrl` ou qualquer outro campo não enviado.
- [x] O comportamento de "campo omitido = não altera" está documentado (já registrado no plano técnico, seção de trade-offs) e coberto por teste.

## Tarefas

- [x] Revisar `BookService.update` (implementado na US-07) para confirmar que o merge é genuinamente parcial: apenas os campos presentes no JSON do `BookUpdateRequest` sobrescrevem a entidade existente.
- [x] Garantir que `updatedAt` é atualizado em toda chamada de `update`, mesmo quando só um campo muda.
- [x] Teste de integração: sequência de 3 PATCHs (status → rating → datas) no mesmo livro resulta no estado final esperado, com todos os campos definidos anteriormente preservados.
- [x] Teste de integração: PATCH enviando somente `{"rating": 4}` não altera `title`/`author`/`status`/`coverUrl` previamente cadastrados.
- [x] Teste de integração: PATCH enviando um objeto vazio `{}` retorna `200` sem alterar nenhum campo.

## Observações/Pendências

Nenhuma alteração em `src/main` foi necessária: `BookService.update` (US-07)
já fazia merge genuinamente parcial (`request.x() != null ? request.x() :
existing.getX()`, campo a campo) e já chamava `Instant.now()`
incondicionalmente para `updatedAt`, independente de haver ou não
diferença em algum campo de negócio. Único trabalho desta história: 3
testes de integração novos em `BookControllerTest`
(`sequentialPatchesEachChangingOneFieldAccumulateAllChangesWithoutLosingPreviousOnes`,
`patchSendingOnlyRatingPreservesAllOtherPreviouslyPersistedFields`,
`patchWithEmptyBodyReturns200WithoutChangingAnyFieldButUpdatesUpdatedAt`).

Decisão explícita registrada pelo terceiro teste: um `PATCH` com corpo
vazio `{}` retorna `200` sem alterar nenhum campo de negócio, mas
`updatedAt` avança mesmo assim — interpretação de que a tarefa 2 desta
história ("`updatedAt` atualizado... mesmo quando só um campo muda") cobre
também o caso-limite de "nenhum campo muda". Esse comportamento foi
explicitado na seção 11 do plano técnico para não ser lido como bug no
futuro. Verificado em código (`BookService.update`, linhas 54-88) e
confirmado por `./gradlew test` (suíte completa verde, `BookControllerTest`
passou de 19 para 22 casos).
