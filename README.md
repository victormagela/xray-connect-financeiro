# X-Ray Connect — Módulo Financeiro ERP

Sistema web de gerenciamento financeiro desenvolvido para a empresa fictícia X-Ray Connect, como parte de um projeto acadêmico do curso de Análise e Desenvolvimento de Sistemas.

A aplicação permite gerenciar contas a pagar, contas a receber, categorias financeiras e consultar indicadores financeiros por meio de um Dashboard e de relatórios de Fluxo de Caixa.

## Tecnologias utilizadas

**Back-end**
- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- Bean Validation
- Flyway
- Maven

**Front-end**
- HTML5
- CSS3
- JavaScript Vanilla

**Banco de dados**
- MySQL 8.4
- Docker e Docker Compose

## Pré-requisitos

Para executar o projeto, é necessário ter instalado:

- [Java JDK 21](https://adoptium.net/)
- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) com Docker Compose
- Uma IDE Java, como o IntelliJ IDEA (opcional)

Não é necessário instalar o Maven separadamente, pois o projeto possui o Maven Wrapper.

Também não é necessário instalar Node.js, npm ou um servidor separado para o front-end.

## Como executar o projeto

### 1. Clonar o repositório

Abra um terminal e execute:

```bash
git clone https://github.com/victormagela/xray-connect-financeiro.git
```

Acesse a pasta do projeto:

```bash
cd xray-connect-financeiro
```

### 2. Configurar as variáveis de ambiente

Na raiz do projeto, no mesmo diretório do arquivo `compose.yaml`, crie um arquivo chamado `.env`.

Adicione o seguinte conteúdo:

```properties
MYSQL_DATABASE=xray_financeiro
MYSQL_USER=xray_user
MYSQL_PASSWORD=senha_local
MYSQL_ROOT_PASSWORD=senha_root_local
MYSQL_PORT=3306
```

Os valores das senhas são exemplos e podem ser substituídos por outras senhas de sua preferência.

**Importante:** o arquivo `.env` contém credenciais do banco de dados e não deve ser enviado ao GitHub. Ele já está incluído nas regras do `.gitignore` do projeto.

O Spring Boot utiliza essas variáveis para estabelecer a conexão com o MySQL, conforme definido em `src/main/resources/application.properties`.

### 3. Iniciar o banco de dados MySQL

Certifique-se de que o Docker Desktop esteja em execução.

Na raiz do projeto, execute:

```bash
docker compose up -d
```

Esse comando cria e inicia o container do MySQL utilizando as configurações do arquivo `compose.yaml`.

O banco de dados será criado automaticamente na primeira inicialização, com o nome definido em `MYSQL_DATABASE`.

Para verificar se o container está em execução:

```bash
docker compose ps
```

Se a porta `3306` já estiver sendo utilizada por outra instalação do MySQL, altere `MYSQL_PORT` no `.env`, por exemplo, para `3307`.

### 4. Executar a aplicação Spring Boot

Com o banco de dados iniciado, execute o projeto utilizando o Maven Wrapper.

**Windows (PowerShell ou CMD):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux ou macOS:**

```bash
./mvnw spring-boot:run
```

Na primeira execução, o Maven poderá precisar baixar as dependências do projeto.

Durante a inicialização, o Flyway verificará as migrations disponíveis em:

```text
src/main/resources/db/migration/
```

As migrations criam automaticamente as tabelas necessárias, incluindo `categorias` e `lancamentos_financeiros`.

Não é necessário executar scripts SQL manualmente.

### 5. Acessar a aplicação

Após a inicialização do Spring Boot, abra um navegador e acesse:

**http://localhost:8080/**

A aplicação disponibiliza as seguintes telas:

| Tela | Endereço |
|---|---|
| Dashboard | `http://localhost:8080/#dashboard` |
| Contas a Receber | `http://localhost:8080/#receber` |
| Contas a Pagar | `http://localhost:8080/#pagar` |
| Fluxo de Caixa | `http://localhost:8080/#fluxo-caixa` |
| Categorias | `http://localhost:8080/#categorias` |

O front-end é servido diretamente pelo Spring Boot, não sendo necessário executar outro servidor HTTP.

## Primeiros passos no sistema

Após iniciar a aplicação:

1. Acesse a tela **Categorias** e cadastre categorias do tipo `RECEITA` e `DESPESA`.
2. Acesse **Contas a Receber** ou **Contas a Pagar** e cadastre os lançamentos financeiros.
3. Utilize a opção de liquidação para registrar o recebimento ou pagamento de uma conta.
4. Acesse o **Dashboard** para visualizar os indicadores financeiros.
5. Acesse **Fluxo de Caixa** para consultar as movimentações liquidadas em determinado período.

**Observação:** os valores do Fluxo de Caixa são calculados a partir da data de liquidação dos lançamentos. Lançamentos pendentes não são considerados no fluxo realizado.

## Endpoints principais da API

A API REST também pode ser utilizada diretamente por clientes HTTP.

| Método | Endpoint | Funcionalidade |
|---|---|---|
| GET | `/api/categorias` | Listar categorias |
| POST | `/api/categorias` | Cadastrar categoria |
| GET | `/api/categorias/{id}` | Buscar categoria |
| PUT | `/api/categorias/{id}` | Atualizar categoria |
| DELETE | `/api/categorias/{id}` | Excluir categoria |
| GET | `/api/lancamentos` | Listar lançamentos |
| POST | `/api/lancamentos` | Cadastrar lançamento |
| GET | `/api/lancamentos/{id}` | Buscar lançamento |
| PUT | `/api/lancamentos/{id}` | Atualizar lançamento |
| DELETE | `/api/lancamentos/{id}` | Excluir lançamento |
| PATCH | `/api/lancamentos/{id}/liquidar` | Liquidar lançamento |
| GET | `/api/relatorios/dashboard` | Consultar indicadores financeiros |
| GET | `/api/relatorios/fluxo-caixa` | Consultar fluxo de caixa |

A listagem de lançamentos aceita parâmetros opcionais de paginação e filtro, como:

```http
GET /api/lancamentos?tipo=RECEITA&page=0&size=10
```

O Fluxo de Caixa utiliza os parâmetros `inicio` e `fim`:

```http
GET /api/relatorios/fluxo-caixa?inicio=2026-11-01&fim=2026-11-30
```

## Executar os testes automatizados

**Windows:**

```powershell
.\mvnw.cmd test
```

**Linux ou macOS:**

```bash
./mvnw test
```

O Maven executará os testes automatizados disponíveis em `src/test`.

## Encerrar a aplicação

Para interromper o Spring Boot, utilize `Ctrl + C` no terminal em que a aplicação está sendo executada.

Para interromper o banco de dados:

```bash
docker compose down
```

Esse comando remove o container, mas preserva os dados armazenados no volume Docker.

Para remover também os dados do banco:

```bash
docker compose down -v
```

**Atenção:** o comando acima exclui o volume persistente do MySQL e causa a perda dos dados armazenados.

## Solução de problemas

**Erro de conexão com o banco de dados**

Verifique se o Docker Desktop está em execução, se o container MySQL foi iniciado e se as credenciais do `.env` estão corretas.

**Porta 3306 ocupada**

Altere a variável `MYSQL_PORT` no arquivo `.env` e reinicie os containers.

**Porta 8080 ocupada**

Encerre o programa que está utilizando a porta ou configure outra porta para o Spring Boot.

**Front-end não apresenta as alterações recentes**

Faça uma atualização forçada no navegador utilizando `Ctrl + Shift + R` ou reinicie o navegador.

**Erro de execução relacionado ao Java**

Verifique se o JDK 21 está instalado e configurado corretamente:

```bash
java -version
```
