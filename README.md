# Sistema de Gestão de Processos Seletivos

Prefeitura Municipal de Paraíba do Sul.

Documentos de referência:

- [`ESPECIFICACAO.md`](ESPECIFICACAO.md): requisitos funcionais e técnicos.
- [`AI_RULES.md`](AI_RULES.md): regras para desenvolvimento assistido por IA.
- [`docs/DECISOES.md`](docs/DECISOES.md): decisões complementares à especificação.

## Estrutura

```text
backend/            API Spring Boot (Java 21)
frontend/           Next.js (Node 24)
infra/nginx/        Proxy reverso: frontend e API na mesma origem
infra/postgres/     Script de criação dos usuários do banco
docs/               Decisões e documentação complementar
docker-compose.yml  Ambiente local completo
```

## Pré-requisitos

Somente Docker (Docker Desktop no Windows). JDK, Maven e Node não precisam estar instalados.

## Configuração

1. Copie `.env.example` para `.env`.
2. Preencha todas as senhas com valores fortes e diferentes entre si.

O arquivo `.env` nunca deve ser versionado.

| Variável | Uso |
|---|---|
| `POSTGRES_DB` | Nome do banco |
| `POSTGRES_SUPERUSER_PASSWORD` | Superusuário do PostgreSQL; usado apenas na inicialização do container |
| `DB_OWNER_USER` / `DB_OWNER_PASSWORD` | Dono do schema `app`; usado apenas pelo Flyway |
| `DB_APP_USER` / `DB_APP_PASSWORD` | Usuário da aplicação; somente SELECT/INSERT/UPDATE/DELETE |
| `HTTP_PORT` | Porta local publicada pelo Nginx (padrão 8080) |

Os usuários do banco são criados somente na primeira inicialização do volume `postgres-data`.
Para alterar essas senhas depois, altere-as também no PostgreSQL.

## Executar

```bash
docker compose up -d --build
```

Acesse `http://localhost:8080`. O banco não é exposto fora da rede interna do compose,
e os endpoints `/actuator` não são acessíveis pelo Nginx.

## Testes do backend

Os testes de integração usam Testcontainers e precisam acessar o Docker.
No Git Bash (Windows):

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)":/workspace -v ps-maven-repo:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /workspace/backend maven:3-eclipse-temurin-21 mvn -B verify
```

Em Linux/macOS, troque `$(pwd -W)` por `$(pwd)` e remova `MSYS_NO_PATHCONV=1`.

## Verificação do frontend

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)/frontend":/app -w /app node:24-alpine sh -c "npm ci && npm run typecheck && npm run build"
```

## Arquitetura do backend

Pacote base `br.gov.pmps.processoseletivo`, organizado conforme a especificação (§53 e §91):
`presentation`, `application`, `domain`, `infrastructure`, `security` e `shared`.
Os pacotes são criados à medida que as funcionalidades são implementadas.

Segurança padrão: toda rota é negada, exceto as liberadas explicitamente em
`security/SecurityConfiguration.java`.

## Produção (pendente)

A configuração atual atende o ambiente local. Antes de produção, ainda faltam:
TLS e HSTS no Nginx, gestão de secrets, backups com restauração testada e limites de recursos revisados (especificação §81 e fase 10).
