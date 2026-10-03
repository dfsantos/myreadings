# US-06 — Rejeitar cadastro com campo obrigatório ausente

**Grupo da spec:** Cadastro de livros/leituras
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#64-tratamento-de-erros)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero que o cadastro seja rejeitado com uma mensagem clara se eu omitir um campo obrigatório (ex: título), para corrigir o erro antes de tentar de novo.

## Critérios de aceite

- [x] `POST /api/v1/books` sem `title` retorna `400` com mensagem indicando o campo `title`.
- [x] `POST /api/v1/books` sem `author` retorna `400` com mensagem indicando o campo `author`.
- [x] O corpo do erro segue o formato `ProblemDetail` (RFC 7807) definido no plano técnico.

## Tarefas

- [x] Adicionar `@NotBlank` em `title` e `author` no `BookCreateRequest`.
- [x] Criar `common/GlobalExceptionHandler.java` (`@RestControllerAdvice`) tratando `MethodArgumentNotValidException` → `ProblemDetail` `400`, listando cada campo inválido e sua mensagem.
- [x] Garantir que a mensagem de erro identifica claramente qual campo falhou (ex: `{"errors": [{"field": "title", "message": "não pode estar em branco"}]}`).
- [x] Teste de integração: POST sem `title` retorna `400` citando o campo `title`.
- [x] Teste de integração: POST sem `author` retorna `400` citando o campo `author`.
- [x] Teste de integração: POST sem `title` e sem `author` retorna `400` citando os dois campos.

## Observações

- O `@NotBlank` em `title`/`author` já existia desde a US-04; nesta história só foi
  confirmado que a validação dispara o fluxo de erro estruturado abaixo.
- Formato final do erro de validação: `ProblemDetail` com extension property
  `errors` (array de `{field, message}`), além de `detail` com o resumo textual
  concatenado. Ver nota na seção 6.4 do plano técnico — este é o padrão a seguir
  em validações futuras (ex.: US-15, `minRating`/`maxRating`).
