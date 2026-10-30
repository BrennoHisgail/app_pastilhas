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
| Segurança | Spring Security (login por sessão, perfis ADMIN e OPERADOR, senha em BCrypt) |
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
de exemplo e dois usuários, então o sistema abre com dados.

Abra http://localhost:8080 e entre com um dos usuários de demonstração:

| Perfil | E-mail | Senha | O que pode fazer |
|---|---|---|---|
| Administrador | `admin@dda.com.br` | `admin123` | tudo, inclusive cadastro de pastilhas e de usuários |
| Operador | `operador@dda.com.br` | `operador123` | lançar entradas e saídas, consultar e exportar |

A documentação da API (Swagger) fica em http://localhost:8080/swagger-ui.html e
abre sem login. Para testar as rotas por ela, entre no sistema antes na mesma
janela do navegador.

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

São 176 testes divididos em quatro níveis:

| Nível | O que cobre | Onde fica |
|---|---|---|
| Entidade | Regras que ficam na `Pastilha`, na `Movimentacao` e no `Usuario`: soma e subtração do saldo, nível mínimo, valor em estoque | `domain/` |
| Serviço | Regras de negócio com os repositórios simulados (Mockito): código duplicado, saldo insuficiente, pastilha inativa, período invertido, último administrador, cálculo dos indicadores e do CSV | `service/` |
| Repositório | As consultas escritas à mão, rodando contra um banco de verdade (`@DataJpaTest`) | `repository/` |
| API | Os códigos HTTP e o JSON de cada endpoint (`@WebMvcTest`), e quem pode chamar cada um (401 sem login, 403 para o perfil errado) | `controller/` |

Além desses, dois testes sobem a aplicação inteira:

- `FluxoDeEstoqueTest` — faz o caminho completo por HTTP, logado como administrador
  (cadastra, dá entrada, dá saída, confere o saldo, o histórico, o painel, os
  indicadores e o CSV). É o teste que confere se o saldo fecha.
- `MigracaoFlywayTest` — sobe do jeito que sobe em produção, com o Flyway criando
  as tabelas e o Hibernate conferindo (`ddl-auto: validate`). Se alguém mexer numa
  entidade e esquecer a migration, quebra aqui e não na hora de publicar.

## Publicar na AWS EC2

O projeto tem o `Dockerfile` (compila com o Maven e copia só o `.jar` para uma
imagem menor) e o `docker-compose.yml`, que sobe a aplicação junto com o PostgreSQL.
No servidor não precisa instalar Java nem banco, só o Docker.

1. Na AWS, crie uma instância EC2 com **Amazon Linux 2023**. A `t3.small` (2 GB) é a
   mais segura; a `t2.micro`/`t3.micro` do nível gratuito funciona, mas o build fica
   lento e pode faltar memória.
2. No grupo de segurança, libere a porta **22** (SSH, só para o seu IP) e a porta
   **80** (HTTP, para todos).
3. Entre no servidor por SSH e instale o Docker e o Git:

   ```bash
   sudo dnf install -y docker git
   sudo systemctl enable --now docker
   sudo usermod -aG docker ec2-user      # depois disso, saia e entre de novo no SSH
   sudo mkdir -p /usr/local/lib/docker/cli-plugins
   sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
        -o /usr/local/lib/docker/cli-plugins/docker-compose
   sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
   ```

4. Baixe o projeto, defina a senha do banco e suba:

   ```bash
   git clone https://github.com/BrennoHisgail/app_pastilhas.git
   cd app_pastilhas/backend
   echo "DB_PASSWORD=troque-esta-senha" > .env
   docker compose up -d --build
   ```

5. Depois de uns minutos o sistema abre em `http://IP-PUBLICO-DA-INSTANCIA/`.

Para atualizar depois de um `git push`: `git pull && docker compose up -d --build`.
Os dados do banco ficam num volume do Docker e não se perdem na atualização.

**Antes de apresentar:** entre como `admin@dda.com.br` e troque a senha dos dois
usuários de demonstração na tela **Usuários**, porque elas estão escritas neste README.

## Organização das pastas

```
backend/src/main/java/br/com/ddametalurgica/estoque/
├── config/          configurações do Spring Security e do Swagger
├── controller/api/  os @RestController, que são a API REST
├── controller/web/  devolve as páginas HTML e coloca o usuário logado no menu
├── security/        liga o Usuario do banco ao login do Spring Security
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

**Login por sessão, não por token.** As telas são Thymeleaf servidas pelo próprio
backend, então o login clássico do Spring Security (formulário + cookie de sessão) é o
caminho mais simples. Como cookie de sessão é vulnerável a CSRF, a proteção de CSRF
fica ligada: o layout coloca o token numa `<meta>` e o `api.js` manda ele em toda
chamada que altera dados.

**A permissão é conferida no backend, não só na tela.** O operador não vê os botões de
cadastro, mas mesmo que chame a API direto recebe 403. As regras ficam todas juntas
no `SecurityConfig`.

**Cada movimentação guarda quem lançou.** A `Movimentacao` aponta para o `Usuario`
logado, e o histórico e o CSV mostram o responsável.

**Os indicadores são calculados na hora.** Ponto de Pedido, Giro, Cobertura e Curva ABC
saem das movimentações do período escolhido (30 a 365 dias), sem tabela nova. O banco
só soma as entradas e saídas por pastilha; as contas ficam no `IndicadorService`.

**O CSV abre direto no Excel.** Separador `;`, decimal com vírgula e o BOM do UTF-8 no
começo do arquivo, que é o que o Excel em português espera.

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
| GET | `/api/indicadores/estoque?dias=90` | RF11 — PP, Giro, Cobertura e Curva ABC |
| GET | `/api/alertas/reposicao` | RF09 |
| GET | `/api/relatorios/estoque.csv` | RF14 |
| GET | `/api/relatorios/movimentacoes.csv` | RF14 |
| GET · POST | `/api/usuarios` | RF13 |
| GET · PUT · DELETE | `/api/usuarios/{id}` | RF13 — inativação no DELETE |
| PATCH | `/api/usuarios/{id}/senha` · `/api/usuarios/{id}/reativar` | RF13 |
| POST | `/login` · `/logout` | RF12 |

Consultar, movimentar, ver indicadores e exportar: qualquer usuário logado.
Cadastrar, editar e inativar pastilhas, e tudo de `/api/usuarios`: só ADMIN.

## Telas

| Rota | Tela | Arquivo |
|---|---|---|
| `/` | Painel | `painel.html` |
| `/pastilhas` | Lista de pastilhas | `pastilhas/lista.html` |
| `/pastilhas/nova` · `/pastilhas/{id}/editar` | Cadastro e edição | `pastilhas/form.html` |
| `/movimentacoes` | Entradas, saídas e histórico | `movimentacoes.html` |
| `/indicadores` | Ponto de Pedido, Giro, Cobertura e Curva ABC | `indicadores.html` |
| `/usuarios` | Lista de usuários (só ADMIN) | `usuarios/lista.html` |
| `/usuarios/novo` · `/usuarios/{id}/editar` | Cadastro, edição e troca de senha (só ADMIN) | `usuarios/form.html` |
| `/login` | Entrada no sistema | `login.html` |

## Situação das sprints

| Data | O que era para entregar | Situação |
|---|---|---|
| 18/09 | Setup, banco, CRUD de pastilhas e Swagger | **feito** |
| 23/09 | Entradas, saídas e saldo automático (RF05-RF07, RF10) | **feito** |
| 02/10 | Alertas e indicadores (RF09, RF11) | **feito** |
| 09/10 | Login, perfis de usuário e senha criptografada (RF12, RF13) | **feito** |
| 16/10 | Telas em Thymeleaf | **feito** |
| 30/10 | Testes e relatório em CSV (RF14) | **feito** — 176 testes |
| 13/11 | Publicar na AWS EC2 | `Dockerfile` e `docker-compose.yml` prontos, falta subir a instância |

Adiantamos as telas, e junto com elas as movimentações, porque o painel e a tela de
movimentação não teriam o que mostrar sem esses endpoints prontos.

## Indicadores de estoque

Calculados pelo `IndicadorService` a partir das movimentações do período escolhido
na tela (30, 90, 180 ou 365 dias), usando o material da disciplina:

| Indicador | Fórmula |
|---|---|
| Consumo médio diário (d) | `saídas do período / dias do período` |
| Ponto de Pedido | `PP = (d × t) + Qs`, com t = `lead_time_dias` e Qs = `estoque_seguranca` |
| Cobertura | `saldo atual / d`: quantos dias o estoque aguenta |
| Giro de estoque | `saídas do período / estoque médio`, com estoque médio = (saldo no início + saldo atual) / 2 |
| Curva ABC | pastilhas ordenadas pelo valor consumido (saídas × custo): A até 80% do total, B até 95%, C o resto |

Pastilha sem consumo no período fica sem cobertura (o estoque não acaba) e na classe C.
