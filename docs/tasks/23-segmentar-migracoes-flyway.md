# RF-05 — Segmentar migrações Flyway por módulo

**Tipo:** Refatoração técnica (sem mudança de comportamento) — não faz
parte do backlog de histórias de usuário (US-01 a US-18, já concluído).
**Spec de origem:** [spec](../specs/refatoracao-modularizacao-vertical-slice.md) (seção 4.2)
**Plano técnico:** [plano técnico](../plans/refatoracao-modularizacao-vertical-slice.md) (seção 5)
**Depende de:** nenhuma das RF anteriores — pode ser feita a qualquer momento, inclusive em paralelo, mas segue a ordem do plano por simplicidade de revisão.

## Objetivo da refatoração

> Organizar as migrações Flyway em um diretório por módulo
> (`db/migration/user/`, `db/migration/book/`), antecipando a estrutura
> que cada serviço levaria consigo numa decomposição futura — sem alterar
> conteúdo SQL, numeração de versão ou o histórico já aplicado em qualquer
> ambiente.

## Critérios de aceite

- [ ] `./gradlew build` passa sem nenhuma asserção de teste alterada — os
      testes de integração existentes já sobem o schema via Flyway em
      SQLite temporário a cada execução e falham imediatamente se alguma
      tabela não existir.
- [ ] Conteúdo de `V1__create_users_table.sql` e
      `V2__create_books_table.sql` permanece byte-a-byte idêntico — só o
      caminho do arquivo muda.
- [ ] `flyway_schema_history` continua sendo uma única tabela por banco
      (comportamento padrão do Flyway, não requer nenhuma configuração
      adicional).

## Tarefas

- [ ] Criar `src/main/resources/db/migration/user/` e mover
      `V1__create_users_table.sql` para lá.
- [ ] Criar `src/main/resources/db/migration/book/` e mover
      `V2__create_books_table.sql` para lá.
- [ ] Adicionar em `application.yaml`, dentro da chave `spring.flyway` já
      existente:
      ```yaml
      spring:
        flyway:
          enabled: true
          locations: classpath:db/migration/user,classpath:db/migration/book
      ```
- [ ] Rodar `./gradlew build` e confirmar suíte completa verde.

## Observações

- Baixo risco e sem dependência das demais RFs — pode ser a primeira fase
  implementada se for mais conveniente, mas o plano técnico a lista por
  último entre as mudanças de código por não se sobrepor a nenhuma delas.
