# RF-04 — Reduzir `GlobalExceptionHandler` ao conjunto genérico

**Tipo:** Refatoração técnica (sem mudança de comportamento) — não faz
parte do backlog de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seção 4.4)
**Depende de:** [RF-01](19-extrair-pacote-security.md), [RF-02](20-fundir-auth-em-user.md) e [RF-03](21-internalizar-invalid-date-range-em-book.md) — esta tarefa é a confirmação/limpeza final das três anteriores, não introduz nenhuma mudança própria.

## Objetivo da refatoração

> Confirmar que `common/GlobalExceptionHandler.java`, depois das RF-01 a
> RF-03, ficou reduzido exatamente ao que é genérico e sem conhecimento de
> domínio — e remover qualquer import morto deixado para trás pelas
> extrações anteriores. Se RF-01/02/03 forem feitas corretamente, esta
> tarefa pode já estar concluída; ela existe para garantir que a limpeza
> não seja esquecida.

## Critérios de aceite

- [ ] `common/GlobalExceptionHandler.java` contém **apenas**:
      `handleDataIntegrityViolation`, `handleNotFound`,
      `handleMessageNotReadable` (+ `buildInvalidFormatDetail`),
      `handleMethodArgumentTypeMismatch`, `handleConstraintViolation`
      (+ `lastPathNode`), `handleValidation`, e o record privado
      `FieldErrorDetail`.
- [ ] Nenhum import não utilizado no arquivo (em particular:
      `EmailAlreadyInUseException`, `JwtException`, `BadCredentialsException`,
      `InvalidDateRangeException` não devem mais aparecer).
- [ ] `./gradlew build` passa sem nenhuma asserção de teste alterada.

## Tarefas

- [ ] Revisar `common/GlobalExceptionHandler.java` linha a linha contra a
      lista de métodos esperada acima.
- [ ] Remover qualquer import não utilizado.
- [ ] Rodar `./gradlew build` e confirmar suíte completa verde.

## Observações

- Esta tarefa fecha o diagnóstico da spec (seção 2.2): o import de
  `auth.EmailAlreadyInUseException` em `common` — a violação de fronteira
  mais grave identificada — só desaparece de fato quando esta tarefa é
  confirmada.
