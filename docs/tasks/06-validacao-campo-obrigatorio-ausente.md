# US-06 — Rejeitar cadastro com campo obrigatório ausente

**Grupo da spec:** Cadastro de livros/leituras
**Referências:** [spec](../specs/catalogo-de-leituras-backend.md) · [plano técnico](../plans/catalogo-de-leituras-backend.md#64-tratamento-de-erros)
**Depende de:** [US-04 — Cadastrar livro com dados básicos e capa](04-cadastrar-livro-dados-basicos.md)

## História de usuário

> Como usuário, quero que o cadastro seja rejeitado com uma mensagem clara se eu omitir um campo obrigatório (ex: título), para corrigir o erro antes de tentar de novo.

## Critérios de aceite

- [ ] `POST /api/v1/books` sem `title` retorna `400` com mensagem indicando o campo `title`.
- [ ] `POST /api/v1/books` sem `author` retorna `400` com mensagem indicando o campo `author`.
- [ ] O corpo do erro segue o formato `ProblemDetail` (RFC 7807) definido no plano técnico.

## Tarefas

- [ ] Adicionar `@NotBlank` em `title` e `author` no `BookCreateRequest`.
- [ ] Criar `common/GlobalExceptionHandler.java` (`@RestControllerAdvice`) tratando `MethodArgumentNotValidException` → `ProblemDetail` `400`, listando cada campo inválido e sua mensagem.
- [ ] Garantir que a mensagem de erro identifica claramente qual campo falhou (ex: `{"errors": [{"field": "title", "message": "não pode estar em branco"}]}`).
- [ ] Teste de integração: POST sem `title` retorna `400` citando o campo `title`.
- [ ] Teste de integração: POST sem `author` retorna `400` citando o campo `author`.
- [ ] Teste de integração: POST sem `title` e sem `author` retorna `400` citando os dois campos.
