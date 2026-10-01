# AI RULES

## Sistema de Gestão de Processos Seletivos

**Projeto:** Sistema de Gestão de Processos Seletivos
**Versão das regras:** 1.0
**Aplicação:** Desenvolvimento assistido por Inteligência Artificial

---

# 1. Objetivo

Este documento define as regras que devem ser obrigatoriamente seguidas por qualquer Inteligência Artificial utilizada no desenvolvimento, manutenção, revisão, refatoração ou evolução do Sistema de Gestão de Processos Seletivos.

A IA deverá tratar este arquivo como uma regra de desenvolvimento do projeto.

Em caso de conflito entre uma solicitação pontual e uma regra de segurança, integridade ou arquitetura deste documento, a IA deverá preservar a segurança e a integridade do sistema.

---

# 2. Regra fundamental

A IA deve:

> Alterar somente o que for necessário para atender ao requisito solicitado, preservando o restante da aplicação.

Não realizar alterações "por oportunidade".

Não refatorar arquivos não relacionados apenas porque parecem poder ser melhorados.

Não alterar arquitetura existente sem necessidade.

---

# 3. Antes de alterar o código

Antes de implementar uma funcionalidade, a IA deverá:

1. analisar a estrutura do projeto;
2. identificar a arquitetura utilizada;
3. localizar os módulos envolvidos;
4. localizar as regras de negócio existentes;
5. verificar entidades relacionadas;
6. verificar casos de uso existentes;
7. verificar os testes existentes;
8. verificar migrations existentes;
9. verificar mecanismos de segurança relacionados;
10. verificar possíveis impactos em outras funcionalidades.

Não assumir que uma funcionalidade não existe apenas porque não foi encontrada no primeiro arquivo analisado.

---

# 4. Não inventar requisitos

A IA não deverá criar regras de negócio por conta própria.

Quando uma regra não estiver definida, deverá:

* utilizar o comportamento mais conservador e seguro quando isso não alterar o negócio;
* ou solicitar esclarecimento quando a decisão puder alterar o funcionamento do processo seletivo.

Não inventar critérios de classificação.

Não inventar regras de pontuação.

Não inventar critérios de desempate.

Não inventar regras de cotas.

Não inventar regras de recursos.

Essas regras pertencem ao edital e/ou aos requisitos definidos pela Prefeitura.

---

# 5. Segurança acima de conveniência

A segurança deverá ser considerada requisito obrigatório.

A IA nunca deverá resolver um problema simplesmente desabilitando:

* autenticação;
* autorização;
* CSRF;
* validação;
* rate limiting;
* proteção de upload;
* auditoria;
* criptografia;
* validação de sessão;
* mecanismos de segurança do Spring Security.

Exemplo proibido:

```text
"Está dando erro de CSRF, então desabilite CSRF."
```

A IA deverá investigar a causa e corrigir a implementação corretamente.

---

# 6. Autenticação

Toda funcionalidade protegida deverá exigir autenticação adequada.

A IA não deverá criar endpoints administrativos públicos.

Não utilizar autenticação baseada apenas em:

* ID;
* CPF;
* e-mail;
* parâmetros da URL;
* campos enviados pelo frontend.

---

# 7. Autorização

Autenticação não é autorização.

A IA deverá verificar:

```text
Usuário autenticado?
        +
Possui permissão?
        +
Possui acesso ao recurso?
```

Somente então a operação deverá ser permitida.

---

# 8. Regra contra IDOR

Nunca assumir que possuir um identificador significa possuir autorização.

Exemplo:

```http
GET /api/applications/{id}
```

O backend deverá verificar se o usuário possui autorização para acessar aquela candidatura.

Não basta consultar:

```java
applicationRepository.findById(id);
```

e retornar o resultado.

A consulta e/ou serviço deverá considerar o contexto de autorização.

---

# 9. IDs não são mecanismos de segurança

UUIDs poderão ser utilizados para dificultar enumeração, porém:

> UUID não substitui autorização.

A IA deverá manter controle de acesso mesmo utilizando UUID.

---

# 10. SQL Injection

Nunca concatenar entrada do usuário diretamente em SQL.

Proibido:

```java
String sql = "SELECT * FROM candidates WHERE cpf = '" + cpf + "'";
```

Preferir:

* JPA;
* Spring Data;
* parâmetros;
* Prepared Statements;
* queries parametrizadas.

Toda entrada externa deverá ser considerada não confiável.

---

# 11. Dados do frontend

Nunca confiar no frontend para regras de segurança.

O frontend pode realizar:

```text
validação de formulário
máscaras
feedback
```

Porém o backend deverá repetir as validações importantes.

Nunca confiar em:

```text
isAdmin
role
permission
userId
candidateId
score
processId
```

enviados pelo navegador.

Essas informações deverão ser determinadas ou validadas pelo backend.

---

# 12. Senhas

Nunca armazenar senha em texto puro.

Nunca criar:

```java
user.setPassword(password);
```

como armazenamento definitivo.

Utilizar o mecanismo de hashing configurado pelo projeto.

A preferência é:

```text
Argon2id
```

Não implementar algoritmo criptográfico próprio.

---

# 13. Secrets

Nunca colocar no código:

```text
password
secret
API_KEY
JWT_SECRET
client_secret
SMTP_PASSWORD
database_password
private_key
```

Nunca fazer commit desses dados.

Utilizar:

* variáveis de ambiente;
* secrets;
* secret manager;
* Docker Secrets;
* mecanismo equivalente.

---

# 14. Logs

Nunca registrar em logs:

* senha;
* token de recuperação;
* token de sessão;
* chave privada;
* segredo;
* cookie de autenticação;
* documento completo;
* dados pessoais desnecessários.

Evitar também registrar CPF completo, quando não for necessário.

Preferir mascaramento.

---

# 15. LGPD

A IA deverá considerar proteção de dados desde o desenvolvimento.

Antes de adicionar um novo campo, perguntar:

> Esse dado é realmente necessário?

Não adicionar coleta de dados apenas porque é tecnicamente possível.

Dados pessoais deverão possuir:

* finalidade;
* controle de acesso;
* armazenamento adequado;
* tratamento compatível com a finalidade;
* proteção.

---

# 16. Dados sensíveis

Informações relacionadas à condição de pessoa com deficiência e necessidades de adaptação deverão possuir controle de acesso adequado.

A IA não deverá disponibilizar esses dados em:

* APIs públicas;
* relatórios públicos;
* logs;
* URLs;
* resultados públicos;

sem que exista justificativa e autorização para isso.

---

# 17. Snapshot da inscrição

Uma regra fundamental do sistema:

> A inscrição deve preservar os dados existentes no momento em que foi realizada.

Se o candidato alterar posteriormente seu cadastro, isso não deverá alterar a inscrição histórica.

A IA não poderá implementar a candidatura simplesmente como uma referência aos dados atuais do candidato.

Deverá existir mecanismo de snapshot ou equivalente.

---

# 18. Inscrição única

Quando o processo estiver configurado para permitir apenas uma inscrição:

A regra deverá ser garantida em múltiplas camadas:

```text
Frontend
   +
Backend
   +
Banco de dados
```

A IA deverá considerar concorrência.

Duas requisições simultâneas não podem criar duas inscrições válidas para o mesmo candidato/processo quando a regra exigir apenas uma.

---

# 19. Uploads

Todo arquivo enviado pelo usuário deverá ser tratado como potencialmente malicioso.

Nunca confiar apenas na extensão.

Validar:

* tamanho;
* MIME;
* assinatura do arquivo;
* extensão;
* conteúdo quando aplicável;
* quantidade;
* permissões.

Formatos inicialmente permitidos:

```text
PDF
JPG
JPEG
PNG
```

---

# 20. Arquivos

Nunca utilizar diretamente o nome fornecido pelo usuário para definir o caminho físico do arquivo.

Evitar:

```text
/uploads/{nomeOriginal}
```

Preferir identificador interno aleatório.

Exemplo:

```text
storage/
 └── 8f2e...a91c
```

O nome original poderá ser armazenado como metadado.

---

# 21. Download de arquivos

Nenhum documento privado poderá ser disponibilizado apenas porque o usuário conhece sua URL.

Toda requisição deverá verificar:

```text
Autenticação
+
Autorização
+
Recurso solicitado
```

---

# 22. Resultados públicos

Antes de disponibilizar um resultado publicamente, verificar se o arquivo contém dados pessoais além do necessário.

Não modificar automaticamente o arquivo oficial sem requisito explícito.

O sistema deverá permitir que a publicação seja realizada conforme o conteúdo oficial e as regras administrativas aplicáveis.

---

# 23. CSRF

Se a aplicação utilizar autenticação baseada em cookies, não remover a proteção CSRF apenas para facilitar testes ou integração.

Qualquer alteração deverá considerar:

* SameSite;
* Secure;
* HttpOnly;
* token CSRF;
* origem da requisição;
* arquitetura de autenticação.

---

# 24. CORS

Não utilizar:

```text
Access-Control-Allow-Origin: *
```

sem justificativa.

CORS deverá ser configurado de acordo com os domínios autorizados.

Nunca permitir origens arbitrárias em produção.

---

# 25. Rate Limit

Endpoints sensíveis deverão possuir proteção contra abuso.

Especialmente:

```text
/login
/register
/forgot-password
/reset-password
/applications
/uploads
/downloads
/reports
```

Não criar rate limit tão agressivo que impeça usuários legítimos sem justificativa.

---

# 26. Recuperação de senha

Não permitir recuperação de senha através de informações previsíveis como:

```text
CPF
data de nascimento
nome da mãe
```

Essas informações não constituem prova suficiente de identidade.

Utilizar token seguro, aleatório, temporário e de uso único.

---

# 27. Enumeração de usuários

A IA deverá evitar respostas que revelem se determinado usuário existe.

Exemplo inadequado:

```text
"E-mail não cadastrado."
```

quando isso permitir enumerar contas.

Preferir resposta genérica.

---

# 28. Sessões

Sessões deverão possuir:

* expiração;
* invalidação;
* proteção contra fixation;
* cookies seguros;
* renovação adequada;
* encerramento após logout;
* invalidação quando necessário após alteração de credencial.

---

# 29. Administradores

Funcionalidades administrativas deverão possuir proteção adicional.

A IA deverá respeitar:

* RBAC;
* menor privilégio;
* auditoria;
* escopo por processo;
* MFA quando implementado.

Nunca criar um "admin universal" escondido no código.

---

# 30. Backdoor

É expressamente proibido criar:

* usuário secreto;
* senha mestre;
* endpoint secreto;
* parâmetro mágico;
* bypass de autenticação;
* bypass de autorização.

Exemplo proibido:

```java
if (user.getEmail().equals("admin@...")) {
    return true;
}
```

---

# 31. Auditoria

Operações críticas deverão ser auditáveis.

A IA deverá considerar auditoria especialmente para:

* alteração de pontuação;
* deferimento;
* indeferimento;
* publicação;
* alteração de permissões;
* exportações;
* acesso administrativo a dados sensíveis;
* alteração de informações críticas.

---

# 32. Alteração de pontuação

Nunca sobrescrever uma pontuação crítica sem histórico.

Registrar:

```text
valor anterior
valor novo
usuário
data/hora
motivo
```

Se o modelo atual não suportar isso, a IA deverá propor a alteração estrutural necessária.

---

# 33. Exportações

Exportações contendo dados pessoais são operações sensíveis.

A IA deverá verificar:

* autorização;
* escopo;
* filtros;
* quantidade;
* auditoria;
* consumo de memória.

Não carregar uma quantidade ilimitada de registros em memória.

---

# 34. Performance

Performance deverá ser tratada de maneira racional.

Antes de adicionar cache, perguntar:

> Existe um problema real que justifique o cache?

Não adicionar Redis apenas para seguir uma arquitetura pré-definida.

---

# 35. Memória RAM

Evitar operações como:

```java
repository.findAll()
```

quando a tabela puder conter grande volume de registros.

Preferir:

* paginação;
* streaming;
* projeções;
* processamento em lotes.

---

# 36. Exportações grandes

Não gerar arquivos gigantes carregando todos os dados simultaneamente em memória.

Preferir:

```text
Banco
 ↓
Lotes/stream
 ↓
Gerador
 ↓
Arquivo
```

---

# 37. N+1

A IA deverá verificar problemas de N+1 ao trabalhar com relacionamentos JPA.

Não utilizar `EAGER` indiscriminadamente como solução.

Preferir:

* queries específicas;
* projections;
* fetch planejado;
* paginação;
* DTOs.

---

# 38. Entity vs DTO

Entidades de persistência não deverão ser retornadas diretamente pela API.

Preferir:

```text
Entity
 ↓
Mapper
 ↓
DTO
 ↓
Response
```

Isso reduz:

* vazamento de dados;
* acoplamento;
* problemas de serialização;
* exposição de relacionamentos.

---

# 39. Controllers

Controllers deverão permanecer pequenos.

Evitar:

```java
@PostMapping(...)
public ResponseEntity<?> fazerTudo(...) {
    // centenas de linhas
}
```

Preferir:

```text
Controller
 ↓
Use Case
 ↓
Domain
 ↓
Repository
```

---

# 40. Regra de complexidade

A IA deverá evitar complexidade ciclomática desnecessária.

Quando houver muitos condicionais relacionados a comportamentos diferentes, avaliar:

* Strategy;
* State;
* Specification;
* polymorphism;
* Factory;
* regras de domínio.

Porém:

> Não substituir um `if` simples por um padrão complexo sem necessidade.

---

# 41. SOLID

Todo código novo deverá respeitar SOLID na medida adequada.

Especialmente:

* responsabilidade única;
* baixo acoplamento;
* alta coesão;
* inversão de dependências.

Não transformar SOLID em burocracia arquitetural.

---

# 42. Padrões de projeto

Os padrões disponíveis incluem:

```text
State
Strategy
Specification
Factory Method
Builder
Observer/Event
Decorator/Proxy
```

Mas eles somente deverão ser utilizados quando resolverem um problema real.

A IA deverá justificar padrões novos quando houver impacto arquitetural relevante.

---

# 43. Evitar padrões artificiais

Não fazer:

```text
Uma classe
 ↓
Factory
 ↓
FactoryFactory
 ↓
StrategyFactory
 ↓
Builder
```

sem necessidade real.

O projeto deve ser compreensível para outro desenvolvedor.

---

# 44. Regras de domínio

Regras de negócio deverão ficar próximas do domínio e dos casos de uso.

Não espalhar a mesma regra entre:

* Controller;
* Service;
* Repository;
* JavaScript;
* banco.

Uma regra importante deverá possuir uma fonte de verdade bem definida.

---

# 45. Frontend

O frontend deverá cuidar principalmente de:

* apresentação;
* interação;
* validação preliminar;
* estado da interface.

Nunca considerar o frontend como mecanismo de autorização.

---

# 46. Backend

O backend deverá ser a fonte de verdade para:

* autenticação;
* autorização;
* regras de negócio;
* integridade;
* permissões;
* segurança;
* persistência.

---

# 47. Banco de dados

Alterações estruturais deverão utilizar migrations.

Nunca editar manualmente o banco de produção como parte normal do desenvolvimento.

Nunca apagar tabelas ou dados sem autorização explícita.

---

# 48. Migrations

Depois que uma migration for aplicada em ambiente compartilhado, não modificar seu conteúdo para "corrigir".

Criar nova migration.

Exemplo:

```text
V001__create_users.sql
V002__create_candidates.sql
V003__create_processes.sql
```

Se houver erro em V003 após aplicação:

```text
V004__fix_process_structure.sql
```

---

# 49. Alterações destrutivas

A IA nunca deverá executar ou criar automaticamente operações destrutivas como:

```sql
DROP TABLE
TRUNCATE
DELETE FROM
```

sem justificativa e autorização apropriada.

Especialmente em produção.

---

# 50. Testes

Toda nova funcionalidade relevante deverá possuir testes.

No mínimo, quando aplicável:

* teste unitário;
* teste de integração;
* teste de autorização;
* teste de erro.

Funcionalidades críticas também deverão possuir testes de segurança.

---

# 51. Testes não podem ser removidos

Nunca remover um teste apenas porque ele está falhando.

Se um teste falhar:

1. identificar a causa;
2. verificar se o comportamento esperado mudou;
3. atualizar o código ou o teste de maneira justificada;
4. preservar a cobertura.

---

# 52. Testes de segurança

Ao alterar autenticação, autorização ou dados, considerar testes para:

```text
IDOR
SQL Injection
XSS
CSRF
Brute Force
Privilege Escalation
Session Fixation
Upload Malicioso
Enumeração
```

---

# 53. Testes de concorrência

Ao alterar:

* inscrição;
* vagas;
* pontuação;
* publicação;
* operações críticas;

considerar condições de corrida.

Especialmente a regra:

```text
um candidato
+
um processo
=
uma inscrição
```

quando aplicável.

---

# 54. Tratamento de erros

Nunca retornar stack trace para o usuário.

Não retornar:

```text
SQLException
NullPointerException
Stack trace
SQL query
caminho interno do servidor
```

O usuário deverá receber mensagem adequada.

O log interno poderá registrar detalhes técnicos de maneira segura.

---

# 55. Mensagens de erro

Mensagens de erro não deverão revelar informações desnecessárias.

Evitar:

```text
"Usuário existe, mas senha está errada."
```

Preferir mensagem genérica em autenticação.

---

# 56. Dependências

Antes de adicionar uma biblioteca:

1. verificar se já existe solução no projeto;
2. verificar se o framework já oferece o recurso;
3. avaliar manutenção;
4. avaliar segurança;
5. avaliar tamanho;
6. avaliar impacto no projeto.

Não adicionar dependência para resolver problema trivial.

---

# 57. Spring Boot

Utilizar os mecanismos nativos do Spring quando forem adequados.

Exemplos:

* Spring Security;
* validação;
* eventos;
* transactions;
* DI;
* repositories.

Não reinventar funcionalidades já fornecidas pelo framework sem motivo.

---

# 58. Transações

Operações que exigem consistência deverão utilizar transações adequadas.

Exemplo:

```text
Criar inscrição
+
Criar snapshot
+
Registrar documentos/metadados necessários
```

A IA deverá avaliar atomicidade.

---

# 59. Eventos assíncronos

Eventos poderão ser utilizados para operações secundárias.

Exemplo:

```text
Inscrição criada
       ↓
Operação principal concluída
       ↓
Evento
       ├── envio de e-mail
       └── outras tarefas secundárias
```

Não tornar a inscrição dependente da disponibilidade do servidor SMTP.

---

# 60. E-mails

Falha no envio do e-mail de confirmação não deverá desfazer uma inscrição já registrada.

O sistema deverá permitir tratamento posterior da falha.

---

# 61. Código duplicado

Ao encontrar código duplicado:

* avaliar se é realmente duplicação;
* extrair apenas quando houver benefício;
* não criar abstração excessiva.

Não refatorar grandes áreas do projeto sem necessidade.

---

# 62. Nomes

Utilizar nomes claros.

Evitar:

```text
data
obj
temp
x
foo
manager2
serviceNew
serviceFinal
```

Preferir:

```text
application
candidate
process
document
score
auditLog
```

---

# 63. Métodos

Métodos deverão possuir uma responsabilidade clara.

Evitar métodos que:

* validam;
* persistem;
* enviam e-mail;
* geram PDF;
* auditam;
* alteram permissões;

tudo ao mesmo tempo.

---

# 64. Classes

Evitar classes "Deus".

Uma classe não deverá concentrar todo o sistema.

Se uma classe possuir responsabilidades excessivas, avaliar separação.

---

# 65. Comentários

Comentários deverão explicar:

> Por que algo é feito.

Não simplesmente:

```java
// incrementa i
i++;
```

Evitar comentários que apenas repetem o código.

---

# 66. Código morto

Não manter código morto sem justificativa.

Evitar:

* métodos nunca utilizados;
* imports inúteis;
* variáveis inúteis;
* comentários com código antigo;
* arquivos abandonados.

---

# 67. Compatibilidade

Antes de alterar contratos de API, verificar consumidores existentes.

Mudanças incompatíveis deverão ser planejadas.

Não quebrar frontend existente ao modificar backend.

---

# 68. API versioning

Caso seja necessário quebrar compatibilidade, avaliar versionamento:

```text
/api/v1/...
/api/v2/...
```

Não criar versionamento apenas por estética.

---

# 69. Dados históricos

Dados históricos de inscrições, resultados e auditoria não deverão ser alterados de forma silenciosa.

Se houver necessidade de correção:

```text
valor anterior
+
valor novo
+
motivo
+
usuário
+
data/hora
```

---

# 70. Resultado externo

A IA nunca deverá criar automaticamente um algoritmo de classificação apenas porque existem dados suficientes.

A classificação final está fora do escopo da versão 1.0.

Se houver solicitação para implementar classificação:

1. identificar os critérios;
2. verificar o edital;
3. verificar se a regra foi formalmente definida;
4. não assumir critérios;
5. solicitar definição quando necessário.

---

# 71. Regras variáveis por edital

Não criar código como:

```java
if (processo.getNumero().equals("001")) {
    ...
}
```

para implementar regras específicas.

Isso deverá ser evitado.

Quando uma regra for configurável, representá-la como configuração/regra de domínio.

---

# 72. Não codificar exceções administrativas

Evitar:

```java
if (candidate.getCpf().equals("...")) {
    // regra especial
}
```

ou:

```java
if (user.getId() == 1) {
    // acesso especial
}
```

Exceções devem existir através de mecanismos formais de configuração e autorização.

---

# 73. Segurança por padrão

Novas funcionalidades deverão iniciar bloqueadas quando não houver autorização explícita.

Preferir:

```text
deny by default
```

em vez de:

```text
allow by default
```

---

# 74. Princípio fail-safe

Quando ocorrer erro de segurança, a aplicação deverá preferir negar o acesso.

Exemplo:

```text
Falha ao verificar permissão
        ↓
Acesso negado
```

e não:

```text
Falha ao verificar permissão
        ↓
Permitir acesso
```

---

# 75. Controle de acesso a processos

Se um administrador estiver limitado a determinados processos, todas as operações deverão respeitar esse escopo.

Não basta limitar a tela.

Também deverá existir controle nos endpoints.

---

# 76. Auditoria de exportação

Sempre que uma exportação contendo dados pessoais for realizada, considerar registrar:

```text
usuário
processo
tipo
filtros
quantidade
data/hora
IP
```

---

# 77. Privacidade em relatórios

Relatórios deverão possuir apenas os dados necessários.

Não adicionar CPF, endereço, telefone ou outros dados apenas porque estão disponíveis.

---

# 78. Performance antes da otimização

Não realizar micro-otimizações sem evidência.

Primeiro:

```text
correção
segurança
clareza
```

Depois:

```text
performance
```

quando houver necessidade real.

---

# 79. Métricas

Quando necessário, monitorar:

* tempo de resposta;
* erros;
* uso de CPU;
* uso de memória;
* conexões de banco;
* filas;
* uploads;
* exportações;
* autenticação.

Não criar métricas excessivas sem finalidade.

---

# 80. Containers

Containers deverão possuir apenas os serviços necessários.

Evitar instalar ferramentas de desenvolvimento desnecessárias em imagens de produção.

Preferir imagens menores quando isso não comprometer segurança ou manutenção.

---

# 81. Produção

A configuração de produção deverá:

* desabilitar debug;
* proteger endpoints administrativos;
* utilizar HTTPS;
* utilizar secrets;
* restringir CORS;
* configurar headers;
* configurar logs;
* configurar backups;
* limitar recursos.

---

# 82. Desenvolvimento

Ambiente de desenvolvimento poderá possuir configurações específicas, mas nunca deverá introduzir código de segurança que seja removido posteriormente.

Não criar:

```text
if (dev) disableSecurity()
```

como solução permanente.

---

# 83. Dados de teste

Nunca utilizar dados reais de candidatos em testes.

Utilizar dados fictícios.

Não colocar:

* CPF real;
* e-mail real;
* telefone real;
* endereço real;
* documentos reais;

no repositório.

---

# 84. Dados de produção

A IA não deverá solicitar ou incluir dados reais de candidatos em código, testes ou prompts quando isso não for necessário.

---

# 85. Pull Requests / commits

Alterações deverão ser pequenas e coerentes.

Evitar commits como:

```text
"arrumei tudo"
```

Preferir:

```text
feat: adiciona inscrição de candidato
fix: corrige validação de CPF
test: adiciona testes de inscrição única
security: restringe acesso aos documentos
```

---

# 86. Não misturar tarefas

Se a solicitação for:

> corrigir o upload

não realizar simultaneamente:

* refatoração completa da autenticação;
* troca de banco;
* alteração do frontend inteiro;
* mudança de arquitetura;

sem necessidade.

---

# 87. Refatorações

Refatorações grandes deverão ser realizadas separadamente quando possível.

Cada refatoração deverá preservar o comportamento existente.

---

# 88. Mudanças arquiteturais

Antes de realizar uma mudança arquitetural significativa, a IA deverá informar:

1. problema atual;
2. solução proposta;
3. impacto;
4. arquivos afetados;
5. impacto no banco;
6. impacto na segurança;
7. impacto nos testes;
8. possíveis riscos.

---

# 89. Regra de não regressão

Uma nova funcionalidade não deverá quebrar:

* autenticação;
* autorização;
* inscrições;
* documentos;
* auditoria;
* relatórios;
* resultados;
* funcionalidades existentes.

Após alterações relevantes, executar a suíte de testes.

---

# 90. Checklist antes de finalizar uma tarefa

Antes de considerar a tarefa concluída, verificar:

```text
[ ] Requisito implementado
[ ] Backend validado
[ ] Frontend validado
[ ] Autorização verificada
[ ] Segurança analisada
[ ] LGPD considerada
[ ] Auditoria considerada
[ ] Testes criados/atualizados
[ ] Migrations criadas, se necessário
[ ] Sem secrets no código
[ ] Sem SQL inseguro
[ ] Sem IDOR
[ ] Sem exposição desnecessária de dados
[ ] Sem código duplicado relevante
[ ] Sem dependências desnecessárias
[ ] Sem alterações não relacionadas
[ ] Testes executados
```

---

# 91. Checklist específico para endpoints

Antes de criar ou alterar um endpoint:

```text
[ ] Qual método HTTP?
[ ] Quem pode acessar?
[ ] Qual permissão é necessária?
[ ] Existe escopo por processo?
[ ] Existe risco de IDOR?
[ ] Existe entrada controlada pelo usuário?
[ ] Existe validação?
[ ] Existe rate limit?
[ ] Existe risco de enumeração?
[ ] Existe dado pessoal?
[ ] Deve gerar auditoria?
[ ] Existe risco de concorrência?
[ ] A resposta expõe somente o necessário?
```

---

# 92. Checklist específico para banco

Antes de alterar o banco:

```text
[ ] Existe migration?
[ ] Existe impacto nos dados atuais?
[ ] Existe constraint?
[ ] Existe índice necessário?
[ ] Existe risco de duplicidade?
[ ] Existe risco de N+1?
[ ] Existe impacto de performance?
[ ] Existe necessidade de rollback?
[ ] A alteração é destrutiva?
[ ] A alteração foi testada?
```

---

# 93. Checklist específico para arquivos

Antes de implementar upload/download:

```text
[ ] Extensão validada
[ ] MIME validado
[ ] Magic bytes considerados
[ ] Tamanho limitado
[ ] Quantidade limitada
[ ] Nome físico seguro
[ ] Storage privado
[ ] Autorização no download
[ ] Antimalware considerado
[ ] Logs seguros
[ ] Auditoria considerada
```

---

# 94. Checklist específico para funcionalidades administrativas

```text
[ ] Autenticação
[ ] RBAC
[ ] Escopo por processo
[ ] Auditoria
[ ] Proteção contra IDOR
[ ] Validação backend
[ ] Logs seguros
[ ] Testes de acesso negado
```

---

# 95. Regra para uso de IA externa

Não enviar para serviços externos dados reais de candidatos, documentos, credenciais, secrets ou informações pessoais desnecessárias.

Sempre que possível, utilizar:

```text
dados fictícios
dados anonimizados
dados mascarados
```

durante desenvolvimento e análise.

---

# 96. Regra para documentação

Alterações relevantes deverão atualizar a documentação correspondente.

Exemplos:

* nova regra de negócio → especificação;
* novo endpoint → documentação da API;
* nova tabela → documentação/modelo;
* nova regra para IA → `AI_RULES.md`;
* nova variável de ambiente → documentação de configuração.

---

# 97. Regra para requisitos conflitantes

Se uma solicitação conflitante for recebida:

### Prioridade 1

Segurança e integridade.

### Prioridade 2

Requisitos legais e proteção de dados.

### Prioridade 3

Regras de negócio formalmente definidas.

### Prioridade 4

Arquitetura.

### Prioridade 5

Performance.

### Prioridade 6

Preferências de implementação.

A IA deverá informar o conflito quando uma solicitação não puder ser implementada de forma segura.

---

# 98. Regra de dúvida

Quando houver dúvida sobre uma regra de negócio que possa alterar o resultado de um processo seletivo, a IA não deverá inventar uma resposta.

Exemplos:

* quantidade de vagas;
* critérios de pontuação;
* desempate;
* cotas;
* aceitação de títulos;
* critérios de classificação;
* possibilidade de múltiplas inscrições.

Nesses casos, solicitar definição ou utilizar apenas uma regra já formalmente estabelecida.

---

# 99. Regra de segurança em caso de dúvida

Quando houver dúvida sobre segurança e existirem duas alternativas tecnicamente possíveis, preferir a alternativa que:

* exponha menos dados;
* conceda menos privilégios;
* processe menos dados;
* mantenha mais rastreabilidade;
* tenha menor superfície de ataque.

---

# 100. Princípio final

A IA deverá sempre buscar:

```text
Código simples
       +
Arquitetura organizada
       +
Segurança forte
       +
LGPD
       +
Testes
       +
Auditoria
       +
Baixo acoplamento
       +
Baixo consumo desnecessário
       +
Facilidade de manutenção
```

O objetivo não é produzir a maior quantidade possível de código.

O objetivo é produzir **somente o código necessário, correto, seguro, testável e sustentável**.

---

# 101. Regra definitiva

Antes de implementar qualquer coisa, a IA deverá perguntar internamente:

> "Existe uma maneira mais simples, segura e sustentável de fazer isso sem violar os requisitos do sistema?"

Se existir, deverá preferi-la.

**Fim do AI_RULES.md**
