# US-10 — Atualizar status/datas/nota sem recriar o livro

**Grupo da spec:** Acompanhamento da leitura
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#11-riscos-e-trade-offs-assumidos)
**Depende de:** [US-07](07-marcar-status-leitura.md), [US-08](08-registrar-datas-inicio-fim.md), [US-09](09-avaliar-livro.md)

## História de usuário

> Como usuário, quero atualizar o status/datas/nota de um livro já cadastrado sem precisar recriá-lo, para refletir o progresso ao longo do tempo.

## Critérios de aceite

- [ ] Múltiplos `PATCH`s sequenciais no mesmo livro (um alterando `status`, outro `rating`, outro datas) resultam no estado acumulado esperado — nenhum PATCH anterior é perdido pelo seguinte.
- [ ] Um `PATCH` enviando `{"rating": 4}` não altera `title`, `status`, `coverUrl` ou qualquer outro campo não enviado.
- [ ] O comportamento de "campo omitido = não altera" está documentado (já registrado no plano técnico, seção de trade-offs) e coberto por teste.

## Tarefas

- [ ] Revisar `BookService.update` (implementado na US-07) para confirmar que o merge é genuinamente parcial: apenas os campos presentes no JSON do `BookUpdateRequest` sobrescrevem a entidade existente.
- [ ] Garantir que `updatedAt` é atualizado em toda chamada de `update`, mesmo quando só um campo muda.
- [ ] Teste de integração: sequência de 3 PATCHs (status → rating → datas) no mesmo livro resulta no estado final esperado, com todos os campos definidos anteriormente preservados.
- [ ] Teste de integração: PATCH enviando somente `{"rating": 4}` não altera `title`/`author`/`status`/`coverUrl` previamente cadastrados.
- [ ] Teste de integração: PATCH enviando um objeto vazio `{}` retorna `200` sem alterar nenhum campo.
