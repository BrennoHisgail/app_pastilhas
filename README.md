# Sistema de Controle de Estoque de Pastilhas Industriais

Sistema web para controlar o estoque de pastilhas industriais da **DDA Metalúrgica**.

**Projeto Aplicado III** — ADS 4ª fase — UniSENAI
Orientador: Prof. Fernando R. Lengler, Dr.
Equipe: Brenno Meurer, Vinícius Barcellos, Pedro Ptelzi e Pedro Luis

---

## Tecnologias usadas

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.3 |
| Banco de dados | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Segurança | Spring Security (ainda liberado, fecha na sprint 4) |
| Telas | Thymeleaf + CSS próprio |
| Documentação da API | springdoc-openapi (Swagger UI) |

## Como executar

Você precisa ter instalado: **JDK 21** e **PostgreSQL 16**. O Maven não precisa
ser instalado: o projeto tem o `mvnw` (Maven Wrapper), que baixa a versão certa sozinho.

Primeiro crie o banco no PostgreSQL:

```sql
CREATE DATABASE estoque;
CREATE USER estoque WITH PASSWORD 'estoque';
GRANT ALL PRIVILEGES ON DATABASE estoque TO estoque;
```

Depois rode o projeto:

```bash
cd backend
./mvnw spring-boot:run       # no Windows: mvnw.cmd spring-boot:run
```

Na primeira execução o Flyway cria as tabelas e já insere algumas pastilhas
de exemplo, então o sistema abre com dados.

Com a aplicação rodando:

| Tela | Endereço |
| --- | --- |
| Painel | http://localhost:8080/ |
| Pastilhas | http://localhost:8080/pastilhas |
| Movimentações | http://localhost:8080/movimentacoes |
| Swagger (documentação da API) | http://localhost:8080/swagger-ui.html |

Se o seu PostgreSQL estiver em outro endereço ou com outra senha, dá para mudar
sem mexer no código, usando as variáveis de ambiente `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER` e `DB_PASSWORD`.

## Testes

Os testes **não precisam de banco instalado**: eles sobem um H2 em memória, que
nasce vazio a cada execução e some no fim.

```bash
cd backend
./mvnw test                  # no Windows: mvnw.cmd test
```

São 122 testes divididos em quatro níveis:

| Nível | O que cobre | Onde fica |
|---|---|---|
| Entidade | Regras que ficam na `Pastilha` e na `Movimentacao`: soma e subtração do saldo, nível mínimo, valor em estoque | `domain/` |
| Serviço | Regras de negócio com os repositórios simulados (Mockito): código duplicado, saldo insuficiente, pastilha inativa, período invertido | `service/` |
| Repositório | As consultas escritas à mão, rodando contra um banco de verdade (`@DataJpaTest`) | `repository/` |
| API | Os códigos HTTP e o JSON de cada endpoint (`@WebMvcTest`) | `controller/` |

Além desses, dois testes sobem a aplicação inteira:

- `FluxoDeEstoqueTest` — faz o caminho completo por HTTP (cadastra, dá entrada,
  dá saída, confere o saldo, o histórico e o painel). É o teste que prova que o
  saldo fecha.
- `MigracaoFlywayTest` — sobe do jeito que sobe em produção, com o Flyway criando
  as tabelas e o Hibernate conferindo (`ddl-auto: validate`). Se alguém mexer numa
  entidade e esquecer a migration, quebra aqui e não na hora de publicar.

## Publicar na internet

O projeto tem o `Dockerfile` pronto, em duas etapas: a primeira compila com o
Maven e a segunda copia só o `.jar` para uma imagem menor.

```bash
cd backend
docker build -t controle-estoque .
docker run -p 8080:8080 -e DB_HOST=... -e DB_NAME=... -e DB_USER=... -e DB_PASSWORD=... controle-estoque
```

## Organização das pastas

```
backend/src/main/java/br/com/ddametalurgica/estoque/
├── config/          configurações do Spring Security e do Swagger
├── controller/api/  os @RestController, que são a API REST
├── controller/web/  devolve as páginas HTML
├── service/         as regras de negócio
├── repository/      interfaces do Spring Data JPA
├── domain/          entidades do banco e enums
├── dto/             records usados para entrada e saída da API
└── exception/       tratamento de erro da API

backend/src/main/resources/db/migration/   scripts do Flyway
backend/src/main/resources/templates/      telas em Thymeleaf
backend/src/main/resources/static/         CSS e JavaScript
backend/src/test/java/...                  testes, espelhando os pacotes acima
```

## Decisões que tomamos

**O saldo fica guardado na própria pastilha.** Pensamos em calcular o saldo somando
todas as movimentações, mas isso ia ficar lento conforme o histórico crescesse. Então
cada pastilha guarda o `saldo_atual`, e cada movimentação guarda o `saldo_apos` para
conseguirmos conferir depois se está tudo certo.

**O saldo não pode ser editado no cadastro.** A tela de cadastro não tem campo de saldo,
e a classe `Pastilha` não tem `setSaldoAtual`. A única forma de mudar o saldo é registrando
uma entrada ou uma saída, inclusive quando a pastilha é nova.

**Quem cria as tabelas é o Flyway, não o Hibernate.** Deixamos o `ddl-auto` como `validate`,
assim o Hibernate só confere se as classes batem com as tabelas. As tabelas ficam nos
scripts SQL, que ficam versionados junto com o código.

**Duas travas contra saldo negativo.** A primeira é no service, que verifica o saldo antes
de registrar a saída. A segunda é um `CHECK (saldo_atual >= 0)` no banco, caso algo passe
pela primeira.

**Inativar em vez de apagar.** O `DELETE /api/pastilhas/{id}` não apaga nada, só marca a
pastilha como inativa. Se apagasse, as movimentações antigas ficariam apontando para uma
pastilha que não existe mais.

**Categoria é um campo de texto, não uma tabela.** Criar uma tabela de categorias daria
mais uma entidade, mais um repositório e mais um CRUD, e por enquanto só precisamos filtrar
por ela. Se depois virar necessidade, dá para criar a tabela numa migration nova.

**Os testes rodam sem banco instalado.** Em produção é PostgreSQL, mas nos testes
usamos um H2 em memória, senão ninguém conseguiria rodar `mvnw test` sem antes subir
um banco. Como os scripts do Flyway são escritos em dialeto PostgreSQL, na maioria dos
testes quem cria as tabelas é o Hibernate; o `MigracaoFlywayTest` é a exceção, e roda
os scripts de verdade para conferir que eles batem com as entidades.

**A saída trava a linha da pastilha no banco.** Duas saídas da mesma pastilha ao mesmo
tempo poderiam ler o mesmo saldo e as duas passarem na conferência, zerando o estoque.
Por isso a movimentação lê a pastilha com `SELECT ... FOR UPDATE`: a segunda espera a
primeira terminar e enxerga o saldo já atualizado.

**O tratador de erro vale só para `/api`.** Ele está limitado ao pacote
`controller.api`, senão um erro numa tela devolveria JSON no navegador em vez da
página de erro.

**Não usamos Lombok.** Os DTOs são `record`, que já não precisam de get e set. Só as duas
entidades têm getters e setters escritos na mão.

**As telas não acessam o banco.** O `PaginaController` só devolve o HTML, e os dados são
buscados pelo JavaScript chamando a API. Assim existe um caminho de dados só, que é a API,
e ela é a mesma coisa que o Swagger documenta.

## Endpoints da API

| Método | Rota | Requisito |
|---|---|---|
| GET | `/api/pastilhas` | RF02 — busca, filtros e paginação |
| GET | `/api/pastilhas/categorias` | RF02 — filtro de categoria |
| GET | `/api/pastilhas/{id}` | RF02 |
| POST | `/api/pastilhas` | RF01 |
| PUT | `/api/pastilhas/{id}` | RF03 |
| PATCH | `/api/pastilhas/{id}/nivel-minimo` | RF08 |
| PATCH | `/api/pastilhas/{id}/reativar` | RF04 |
| DELETE | `/api/pastilhas/{id}` | RF04 — inativação |
| POST | `/api/movimentacoes/entrada` | RF05, RF07 |
| POST | `/api/movimentacoes/saida` | RF06, RF07 |
| GET | `/api/movimentacoes` | RF10 — histórico |
| GET | `/api/dashboard/indicadores` | RF11 |
| GET | `/api/alertas/reposicao` | RF09 |

## Telas

| Rota | Tela | Arquivo |
|---|---|---|
| `/` | Painel | `painel.html` |
| `/pastilhas` | Lista de pastilhas | `pastilhas/lista.html` |
| `/pastilhas/nova` · `/pastilhas/{id}/editar` | Cadastro e edição | `pastilhas/form.html` |
| `/movimentacoes` | Entradas, saídas e histórico | `movimentacoes.html` |

## Situação das sprints

| Data | O que era para entregar | Situação |
|---|---|---|
| 18/09 | Setup, banco, CRUD de pastilhas e Swagger | **feito** |
| 23/09 | Entradas, saídas e saldo automático (RF05-RF07, RF10) | **feito** |
| 02/10 | Alertas e indicadores (RF09, RF11) | **parcial** — falta Ponto de Pedido, Giro, Cobertura e ABC |
| 09/10 | Login, perfis de usuário e senha criptografada (RF12, RF13) | a fazer |
| 16/10 | Telas em Thymeleaf | **feito** |
| 30/10 | Testes e relatório em CSV (RF14) | **parcial** — testes feitos (122), falta o CSV |
| 13/11 | Publicar na AWS EC2 | a fazer |

Adiantamos as telas, e junto com elas as movimentações, porque o painel e a tela de
movimentação não teriam o que mostrar sem esses endpoints prontos.

## Indicadores que ainda vamos calcular

Usando o material da disciplina, a partir das movimentações:

| Indicador | Fórmula |
|---|---|
| Ponto de Pedido | `PP = (d × t) + Qs` |
| Giro de estoque | `consumo do período / estoque médio` |
| Cobertura | dias que o estoque aguenta |
| Curva ABC | Pareto por valor movimentado |

Os campos `lead_time_dias` (o t) e `estoque_seguranca` (o Qs) já estão no cadastro
esperando esses cálculos.
