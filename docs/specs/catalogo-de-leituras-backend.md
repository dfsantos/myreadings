# PRD — Backend do Catálogo de Leituras (myreadings)

**Status:** Rascunho v1
**Data:** 2026-10-02
**Stack confirmada no repositório:** Spring Boot 4.1.1, Java 25, Spring Data JPA, Spring Web MVC, SQLite (JDBC)

## Problem Statement

Quem lê muitos livros perde o controle do que já leu, do que está lendo e do que achou de cada leitura, porque essa informação hoje vive espalhada (memória, planilhas, apps de terceiros sem API própria). Sem um catálogo próprio, o usuário não consegue buscar rapidamente um livro já lido, retomar uma leitura em andamento ou revisar sua avaliação passada. Este projeto cobre apenas o **backend**: uma API que permita a cada usuário cadastrar, consultar, atualizar e remover os livros que leu ou está lendo, com dados básicos, capa e progresso de leitura.

## Goals

1. Permitir que um usuário autenticado cadastre um livro (dados básicos + capa) em menos de uma chamada de API, sem etapas obrigatórias além dos campos essenciais.
2. Permitir buscar, entre os livros do próprio usuário, por título/autor em texto livre, retornando resultados relevantes.
3. Permitir filtrar a lista de livros por status de leitura, gênero/categoria e faixa de avaliação, isoladamente ou combinados.
4. Garantir isolamento total de dados entre usuários (um usuário nunca lê/edita/remove livros de outro).
5. Suportar o ciclo de vida completo de uma leitura (cadastro → em andamento → concluída/abandonada → avaliada) via atualizações incrementais do mesmo registro.

## Non-Goals

- **Catálogo compartilhado/deduplicado de livros entre usuários** (ex: um "livro canônico" reaproveitado por múltiplos usuários). Fora de escopo nesta v1 — cada usuário mantém seus próprios registros, mesmo que dois usuários cadastrem o "mesmo" livro.
- **Upload/armazenamento de arquivo de imagem para a capa.** Definido que a capa é apenas uma URL informada pelo usuário; não há processamento, resize ou hospedagem de imagem no backend.
- **Integração com APIs externas de livros** (Google Books, OpenLibrary, ISBN lookup automático). Pode acelerar o cadastro no futuro, mas não é necessário para o CRUD básico funcionar.
- **Múltiplas releituras do mesmo livro** (histórico de relidas com datas distintas). V1 assume um registro = uma passagem de leitura por livro cadastrado; releitura fica como consideração futura (P2).
- **Frontend/UI.** Este documento especifica apenas a API backend.
- **Recursos sociais** (compartilhar listas, seguir outros usuários, comentários públicos). Não há evidência de demanda para isso ainda; mantém o escopo focado em uso individual.

## User Stories

**Autenticação e conta**
- Como usuário novo, quero criar uma conta para ter minha própria lista de leituras isolada das de outros usuários.
- Como usuário cadastrado, quero fazer login para acessar meu catálogo pessoal.
- Como usuário autenticado, quero que toda operação sobre livros seja automaticamente restrita aos meus próprios registros, sem precisar informar um "meu usuário" manualmente.

**Cadastro de livros/leituras**
- Como usuário, quero cadastrar um livro informando dados básicos (título, autor, editora, ano, número de páginas, gênero) e uma URL de capa, para registrar que estou lendo ou já li esse livro.
- Como usuário, quero cadastrar um livro sem preencher todos os campos opcionais (ex: sem capa, sem editora), para não ser bloqueado por informação que não tenho em mãos.
- Como usuário, quero que o cadastro seja rejeitado com uma mensagem clara se eu omitir um campo obrigatório (ex: título), para corrigir o erro antes de tentar de novo.

**Acompanhamento da leitura**
- Como usuário, quero marcar o status de um livro (quero ler / lendo / lido / abandonado), para saber em que ponto estou com cada leitura.
- Como usuário, quero registrar a data de início e a data de término da leitura, para acompanhar quanto tempo levei.
- Como usuário, quero dar uma nota (avaliação) a um livro que terminei, para lembrar o que achei dele depois.
- Como usuário, quero atualizar o status/datas/nota de um livro já cadastrado sem precisar recriá-lo, para refletir o progresso ao longo do tempo.

**Consulta e busca**
- Como usuário, quero listar todos os livros do meu catálogo, para ter uma visão geral da minha coleção.
- Como usuário, quero buscar por título ou autor usando texto livre, para encontrar rapidamente um livro específico.
- Como usuário, quero filtrar minha lista por status de leitura (ex: só "lendo"), para ver o que está em andamento.
- Como usuário, quero filtrar por gênero/categoria, para navegar minha coleção por tipo de livro.
- Como usuário, quero filtrar por faixa de avaliação (ex: nota >= 4), para relembrar meus livros favoritos.
- Como usuário, quero combinar múltiplos filtros (ex: gênero + status), para refinar a busca.
- Como usuário, quero receber uma lista vazia (não um erro) quando nenhum livro corresponder à busca, para distinguir "sem resultados" de "algo quebrou".

**Remoção**
- Como usuário, quero remover um livro cadastrado por engano ou que não quero mais acompanhar, para manter meu catálogo limpo.

## Requirements

### Must-Have (P0)

**Autenticação multiusuário**
- Endpoint de registro de usuário (ex: e-mail + senha, senha armazenada com hash).
- Endpoint de login retornando um token (JWT ou equivalente) usado para autenticar as demais chamadas.
- Todas as rotas de livros exigem autenticação; o usuário autenticado só acessa seus próprios registros.
- Critério de aceite: requisição sem token válido a qualquer rota de livros retorna 401; tentativa de acessar/editar/remover livro de outro usuário retorna 404 (não revela existência do recurso de terceiros).

**CRUD de livros/leituras**
- Criar livro: título (obrigatório), autor (obrigatório), editora, ano de publicação, número de páginas, gênero/categoria, URL da capa, status de leitura, data de início, data de término, avaliação — todos opcionais exceto título e autor.
- Consultar um livro por id.
- Atualizar um livro (qualquer campo, incluindo status/datas/avaliação).
- Remover um livro.
- Critério de aceite:
  - [ ] POST sem título ou autor retorna 400 com mensagem indicando o campo faltante.
  - [ ] POST válido retorna 201 com o recurso criado, incluindo id gerado.
  - [ ] GET por id de livro pertencente a outro usuário retorna 404.
  - [ ] PUT/PATCH atualiza apenas os campos enviados sem apagar os demais.
  - [ ] DELETE em livro inexistente ou de outro usuário retorna 404.

**Listagem e busca**
- Listar livros do usuário autenticado com paginação.
- Buscar por texto livre em título e autor (case-insensitive, substring).
- Filtrar por status de leitura, por gênero/categoria e por faixa de avaliação (min/max), combináveis entre si e com a busca textual.
- Critério de aceite:
  - [ ] Busca sem filtros retorna todos os livros do usuário, paginados.
  - [ ] Busca por termo que não corresponde a nada retorna lista vazia com 200 (não erro).
  - [ ] Filtros combinados (ex: gênero=ficção AND status=lido) aplicam AND entre critérios.

**Modelo de dados — status de leitura**
- Enum fechado: `QUERO_LER`, `LENDO`, `LIDO`, `ABANDONADO`.
- Critério de aceite: valor fora do enum no corpo da requisição retorna 400.

### Nice-to-Have (P1)

- Ordenação customizável na listagem (por título, data de término, avaliação).
- Validação de formato da URL da capa (ex: deve ser http/https válido) além de apenas aceitar string.
- Endpoint de estatísticas simples do usuário (total de livros lidos, média de avaliação, livros por gênero).
- Soft delete (manter histórico em vez de remoção física), caso se queira recuperar um livro removido por engano.

### Future Considerations (P2)

- Suporte a múltiplas releituras do mesmo livro (histórico de datas por leitura, não só uma data de início/fim por livro).
- Integração com API externa de livros para preencher dados automaticamente a partir de ISBN/título.
- Upload de imagem de capa como alternativa à URL.
- Catálogo compartilhado entre usuários (livro canônico reaproveitável).
- Exportação dos dados (CSV/JSON) do catálogo pessoal.

## Success Metrics

Projeto pessoal/uso individual — métricas de validação técnica em vez de métricas de produto em escala:

**Leading indicators**
- Todos os critérios de aceite dos requisitos P0 passam em testes automatizados antes de considerar o backend "pronto para uso".
- Tempo de resposta da listagem/busca com paginação permanece abaixo de 200ms em volume de uso pessoal (até ~5.000 livros por usuário).

**Lagging indicators**
- Uso contínuo real pelo próprio autor para registrar leituras por pelo menos 4 semanas após o lançamento, sem necessidade de edição manual direta no banco de dados.

## Open Questions

- Qual mecanismo de autenticação exatamente — JWT stateless, sessão com cookie, ou OAuth de terceiro (Google, etc.)? **(engenharia)**
- Lista fixa de gêneros/categorias ou texto livre definido pelo usuário? Afeta se o filtro de gênero é um enum ou busca por string. **(produto/engenharia)**
- Faixa válida da avaliação (0–5? 0–10? com decimais?). **(produto)**
- SQLite é definitivo para produção ou apenas para desenvolvimento local? Isso afeta decisões de migração de schema e concorrência de escrita. **(engenharia)**

## Timeline Considerations

- Sem prazo contratual ou evento externo identificado — projeto pessoal sem deadline fixo.
- Dependência interna de ordem: autenticação (P0) precisa existir antes do CRUD de livros poder aplicar isolamento por usuário; recomenda-se implementar nessa ordem.
- Fase sugerida: v1 = autenticação + CRUD + busca/filtros (P0 completo). v1.1 = itens P1 conforme necessidade percebida de uso real.
