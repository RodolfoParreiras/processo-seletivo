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
| `APP_PUBLIC_URL` | URL pública do sistema, usada nos links enviados por e-mail |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Servidor SMTP |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Autenticação e STARTTLS no SMTP (em produção: `true`) |
| `MAIL_FROM` | Remetente dos e-mails |
| `BOOTSTRAP_ADMIN_*` | Provisionamento do primeiro Administrador Geral (ver abaixo) |
| `STORAGE_PATH` | Diretório privado dos arquivos enviados (no compose: volume `app-storage`, que deve entrar no backup) |
| `SCHEDULING_ENABLED` | Abertura/encerramento automático das inscrições (padrão `true`) |

Os usuários do banco são criados somente na primeira inicialização do volume `postgres-data`.
Para alterar essas senhas depois, altere-as também no PostgreSQL.

## Executar

```bash
docker compose --profile dev up -d --build
```

O perfil `dev` sobe também o Mailpit, que captura os e-mails enviados (interface em `http://localhost:8025`).
Sem o perfil, configure `MAIL_HOST` para um SMTP real.

Acesse `http://localhost:8080`. O banco não é exposto fora da rede interna do compose,
e os endpoints `/actuator` não são acessíveis pelo Nginx.

## Primeiro Administrador Geral

Não existe usuário ou senha padrão. Para criar o primeiro administrador:

1. No `.env`, defina `BOOTSTRAP_ADMIN_ENABLED=true` e preencha `BOOTSTRAP_ADMIN_CPF`,
   `BOOTSTRAP_ADMIN_NAME` e `BOOTSTRAP_ADMIN_EMAIL`.
2. Suba (ou reinicie) o backend. Se ainda não houver nenhuma conta administrativa, a conta é criada
   e um link de definição de senha, válido por 24 horas, é enviado ao e-mail informado.
3. Volte `BOOTSTRAP_ADMIN_ENABLED` para `false`.

Se já existir conta administrativa, o provisionamento não faz nada. A operação é registrada na auditoria.
Os demais administradores serão criados pela área administrativa (fase 7).

## Endpoints

| Método | Caminho | Acesso |
|---|---|---|
| GET | `/api/auth/csrf` | Público; emite o cookie `XSRF-TOKEN` |
| POST | `/api/auth/register` | Público; cadastro de candidato |
| POST | `/api/auth/login` | Público; login de candidato |
| POST | `/api/admin/auth/login` | Público; login administrativo |
| POST | `/api/auth/forgot-password`, `/api/admin/auth/forgot-password` | Público; resposta sempre genérica |
| POST | `/api/auth/reset-password` | Público; exige token válido |
| GET | `/api/auth/session` | Autenticado |
| POST | `/api/auth/logout` | Autenticado |
| PUT | `/api/auth/password` | Autenticado; exige a senha atual e encerra as demais sessões |
| GET, PUT | `/api/candidate/me` | Candidato; dados do próprio titular da sessão |
| PUT | `/api/candidate/me/email` | Candidato; exige a senha atual |

| GET | `/api/processes`, `/api/processes/{id}` | Público; nunca mostra rascunhos |
| GET | `/api/processes/{id}/notices/{versão}/file` | Público; somente versões publicadas do edital |
| GET | `/api/admin/processes`, `/{id}`, `/{id}/history`, `/{id}/notices/{versão}/file` | `PROCESSO_VISUALIZAR` |
| POST | `/api/admin/processes` | `PROCESSO_CRIAR` |
| PUT, POST, DELETE | `/api/admin/processes/{id}`, `/positions`, `/document-requirements` | `PROCESSO_EDITAR` (somente rascunho) |
| POST | `/api/admin/processes/{id}/notices` (multipart) | `PROCESSO_EDITAR`; após publicação exige `reason` (retificação) |
| POST | `/api/admin/processes/{id}/extend-registration` | `PROCESSO_EDITAR` |
| POST | `/api/admin/processes/{id}/publish` | `PROCESSO_PUBLICAR` |
| POST | `/api/admin/processes/{id}/suspend`, `/resume`, `/cancel`, `/archive` | `PROCESSO_ENCERRAR`; exigem `reason` |
| GET, POST | `/api/candidate/applications` | Candidato; lista as próprias inscrições / inicia rascunho |
| GET, DELETE | `/api/candidate/applications/{id}` | Candidato; detalhe / descarte do rascunho |
| POST, DELETE, GET | `/api/candidate/applications/{id}/documents[/{doc}[/file]]` | Candidato; envio e remoção só em rascunho |
| POST | `/api/candidate/applications/{id}/confirm` | Candidato; exige documentos obrigatórios e período aberto |
| GET | `/api/candidate/applications/{id}/receipt` | Candidato; comprovante em PDF |
| GET | `/api/receipts/{código}` | Público; verificação do comprovante, sem dados pessoais |
| GET | `/api/admin/processes/{id}/applications` | `INSCRICAO_VISUALIZAR`; sem rascunhos |
| GET | `/api/admin/applications/{id}`, `/documents/{doc}/file` | `INSCRICAO_VISUALIZAR`; dados PcD só com `DADOS_PCD_VISUALIZAR`; download auditado |
| POST | `/api/admin/applications/{id}/defer` | `INSCRICAO_DEFERIR` |
| POST | `/api/admin/applications/{id}/deny` | `INSCRICAO_INDEFERIR`; exige `reason` |
| GET | `/api/processes/{id}/documents`, `/{doc}/file` | Público; Documentos do Processo não retirados |
| GET | `/api/admin/processes/{id}/documents`, `/{doc}/file` | `PROCESSO_VISUALIZAR`; inclui retirados |
| POST | `/api/admin/processes/{id}/documents` (multipart: `name`, `file`) | `RESULTADO_PUBLICAR` |
| POST | `/api/admin/processes/{id}/documents/{doc}/withdraw` | `RESULTADO_PUBLICAR`; exige `reason` |

Todo `POST` exige o header `X-XSRF-TOKEN` com o valor do cookie `XSRF-TOKEN`.

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
