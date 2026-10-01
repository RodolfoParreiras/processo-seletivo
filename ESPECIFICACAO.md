# ESPECIFICAÇÃO FUNCIONAL E TÉCNICA

## Sistema de Gestão de Processos Seletivos

**Versão:** 1.0
**Órgão:** Prefeitura Municipal de Paraíba do Sul
**Área responsável:** Gestão Pública / Secretaria de Ciência e Tecnologia
**Status:** Especificação para desenvolvimento

---

# 1. Objetivo

O Sistema de Gestão de Processos Seletivos tem como objetivo permitir que a Prefeitura Municipal de Paraíba do Sul realize, de forma centralizada, segura e auditável, a gestão de processos seletivos, desde a publicação do edital e abertura das inscrições até o recebimento e análise de documentos, geração de dados para classificação e publicação dos resultados oficiais.

O sistema deverá disponibilizar duas áreas principais:

* **Área do Candidato**;
* **Área Administrativa**.

O sistema deverá priorizar:

* segurança da informação;
* proteção de dados pessoais;
* conformidade com a LGPD;
* rastreabilidade das operações;
* facilidade de manutenção;
* baixo consumo desnecessário de recursos;
* arquitetura modular;
* código limpo;
* aplicação dos princípios SOLID;
* utilização criteriosa de padrões de projeto;
* prevenção contra vulnerabilidades conhecidas;
* possibilidade de evolução futura sem necessidade de reescrita da aplicação.

---

# 2. Escopo da versão 1.0

A versão inicial deverá contemplar:

1. Cadastro de candidatos;
2. Autenticação;
3. Recuperação de senha;
4. Área do candidato;
5. Cadastro e gerenciamento de processos seletivos;
6. Cadastro de cargos e vagas;
7. Configuração de regras do processo;
8. Publicação do edital;
9. Abertura e encerramento das inscrições;
10. Inscrição dos candidatos;
11. Geração de comprovante de inscrição;
12. Envio de confirmação de inscrição por e-mail;
13. Upload de documentos;
14. Análise de documentos e títulos;
15. Registro de pontuação, quando aplicável;
16. Deferimento ou indeferimento de inscrições;
17. Exportação de dados para classificação externa;
18. Publicação de resultado preliminar;
19. Publicação de resultado definitivo;
20. Gerenciamento de usuários administrativos;
21. Controle granular de permissões;
22. Auditoria;
23. Relatórios em PDF e Excel;
24. Proteção de dados;
25. Controle de acesso;
26. Logs de segurança;
27. Backup e recuperação;
28. Controle de sessões.

---

# 3. Funcionalidades fora do escopo da versão 1.0

Não deverão fazer parte da primeira versão:

* aplicação de provas;
* banco de questões;
* cronômetro de prova;
* correção automática de provas;
* sistema de recursos administrativos;
* recebimento de recursos dentro do sistema;
* algoritmo genérico de classificação;
* cálculo automático de ranking final;
* integrações com sistemas externos;
* integração com sistemas de autenticação externos;
* criação de formulários completamente dinâmicos;
* mecanismo que permita ao administrador programar regras através de código;
* automação de critérios específicos que dependam exclusivamente de interpretação do edital.

Esses recursos poderão ser avaliados futuramente.

---

# 4. Princípio fundamental sobre classificação

A classificação final dos candidatos **não deverá ser calculada pelo sistema na versão 1.0**.

Os critérios de classificação podem variar significativamente de acordo com cada edital.

Portanto, o sistema deverá funcionar como uma plataforma de gestão das informações utilizadas no processo seletivo.

O fluxo será:

```text
Inscrições
     ↓
Análise documental
     ↓
Registro de informações/pontuações
     ↓
Exportação dos dados
     ↓
Classificação realizada externamente
     ↓
Resultado oficial
     ↓
Publicação no sistema
```

O sistema poderá registrar pontuações relacionadas à análise de títulos quando necessário, porém não deverá assumir que essas pontuações são suficientes para determinar a classificação final.

A classificação oficial será produzida externamente, utilizando os dados exportados pelo sistema.

O resultado final poderá ser posteriormente disponibilizado no sistema por meio de arquivo oficial.

---

# 5. Perfis de usuário

## 5.1 Candidato

O candidato poderá:

* criar sua conta;
* autenticar-se;
* recuperar sua senha;
* consultar seus dados;
* alterar seus dados pessoais;
* consultar processos abertos;
* realizar inscrições;
* acompanhar suas candidaturas;
* consultar documentos enviados;
* consultar situação da inscrição;
* emitir comprovante;
* consultar resultados publicados.

---

## 5.2 Administrador

O administrador será responsável pelas operações administrativas autorizadas de acordo com suas permissões.

Poderá, conforme autorização:

* criar processos;
* editar processos;
* publicar processos;
* cadastrar cargos;
* configurar regras;
* consultar inscrições;
* analisar documentos;
* registrar pontuações;
* deferir ou indeferir inscrições;
* exportar informações;
* publicar resultados;
* gerar relatórios;
* consultar auditoria.

---

## 5.3 Administrador Geral

O Administrador Geral possuirá controle sobre os usuários administrativos.

Poderá:

* criar administradores;
* desativar administradores;
* definir permissões;
* definir quais processos poderão ser acessados;
* consultar atividades administrativas;
* gerenciar perfis e permissões.

O Administrador Geral também deverá estar sujeito à auditoria.

---

# 6. Controle de permissões

O sistema deverá utilizar RBAC — Role-Based Access Control.

As permissões deverão ser granulares.

Exemplos:

```text
PROCESSO_VISUALIZAR
PROCESSO_CRIAR
PROCESSO_EDITAR
PROCESSO_PUBLICAR
PROCESSO_ENCERRAR

INSCRICAO_VISUALIZAR
INSCRICAO_EDITAR
INSCRICAO_DEFERIR
INSCRICAO_INDEFERIR

DOCUMENTO_VISUALIZAR
DOCUMENTO_ANALISAR
DOCUMENTO_APROVAR
DOCUMENTO_REJEITAR

PONTUACAO_VISUALIZAR
PONTUACAO_EDITAR

EXPORTACAO_GERAR

RESULTADO_PUBLICAR
RESULTADO_EDITAR

RELATORIO_GERAR

USUARIO_GERENCIAR
PERMISSAO_GERENCIAR

AUDITORIA_VISUALIZAR
```

As permissões deverão ser verificadas no backend.

A ocultação de botões no frontend **não constitui mecanismo de segurança**.

---

# 7. Cadastro do candidato

O cadastro deverá possuir os seguintes campos:

* CPF;
* nome completo;
* data de nascimento;
* nome da mãe;
* e-mail;
* telefone;
* CEP;
* endereço;
* número;
* complemento;
* bairro;
* cidade;
* UF;
* senha;
* pessoa com deficiência;
* necessidade de adaptações.

## 7.1 Regras

O CPF deverá ser único.

O CPF deverá ser validado tanto no frontend quanto no backend.

A validação do frontend terá finalidade de usabilidade.

A validação do backend será obrigatória para segurança e integridade.

O sistema não deverá confiar em dados enviados pelo navegador.

---

# 8. Dados pessoais do candidato

O candidato poderá alterar seus dados posteriormente.

Entretanto, alterações no cadastro não poderão modificar retroativamente uma inscrição já realizada.

Ao realizar uma inscrição, o sistema deverá criar um **snapshot dos dados pessoais utilizados naquela candidatura**.

Exemplo:

```text
Candidato
    ↓
Dados atuais
    ↓
Realiza inscrição
    ↓
Snapshot da inscrição
```

Se o candidato posteriormente alterar:

* telefone;
* endereço;
* e-mail;
* nome;
* outros dados permitidos;

a inscrição histórica deverá continuar vinculada aos dados existentes no momento da inscrição.

Essa regra é obrigatória para garantir:

* integridade histórica;
* auditoria;
* rastreabilidade;
* segurança jurídica;
* reprodução das informações submetidas pelo candidato.

---

# 9. Pessoa com deficiência

O campo:

```text
Pessoa com deficiência?
```

deverá possuir pelo menos:

```text
SIM
NÃO
```

Caso seja selecionado `SIM`, o campo:

```text
Necessidade de Adaptações
```

deverá ser disponibilizado.

A obrigatoriedade desse campo deverá ser definida conforme a regra de negócio adotada.

Por se tratar de informação de natureza especialmente sensível no contexto da proteção de dados, o acesso administrativo deverá ser restrito às pessoas que efetivamente necessitem dessa informação.

---

# 10. Cadastro e autenticação

O cadastro deverá seguir:

```text
Cadastro
   ↓
Validação
   ↓
Criação da conta
   ↓
Login
```

A confirmação de e-mail **não será obrigatória para ativação da conta na versão 1.0**.

O e-mail será utilizado para:

* recuperação de senha;
* confirmação de inscrição;
* outras comunicações estritamente necessárias.

---

# 11. Senhas

As senhas nunca poderão ser armazenadas em texto puro.

Deverá ser utilizado algoritmo moderno de hash de senha, preferencialmente:

```text
Argon2id
```

O sistema deverá utilizar salt individual por senha.

Nunca deverão existir:

```text
senha
senha_original
senha_descriptografada
```

no banco de dados.

O sistema não deverá possuir mecanismo para que administradores visualizem a senha do usuário.

---

# 12. Recuperação de senha

A recuperação deverá utilizar token temporário.

Fluxo:

```text
Usuário informa e-mail
        ↓
Sistema responde de maneira genérica
        ↓
Token temporário
        ↓
Link de recuperação
        ↓
Nova senha
        ↓
Token invalidado
```

O sistema não deverá revelar se determinado e-mail está ou não cadastrado.

O token deverá:

* possuir validade curta;
* ser de uso único;
* ser imprevisível;
* ser invalidado após utilização;
* ser invalidado quando necessário por segurança.

Após alteração de senha, as sessões existentes poderão ser invalidadas como medida de segurança.

---

# 13. Processos seletivos

Cada processo deverá possuir:

* número;
* ano;
* título;
* secretaria responsável;
* edital;
* período de inscrição;
* cargos;
* vagas;
* regras específicas;
* situação;
* datas de criação/publicação/encerramento.

---

# 14. Situação do processo

O processo poderá possuir estados como:

```text
RASCUNHO
PUBLICADO
INSCRICOES_ABERTAS
INSCRICOES_ENCERRADAS
RESULTADO_PRELIMINAR
RESULTADO_DEFINITIVO
ARQUIVADO
```

A mudança de estado deverá respeitar as transições permitidas.

Exemplo:

```text
RASCUNHO
   ↓
PUBLICADO
   ↓
INSCRICOES_ABERTAS
   ↓
INSCRICOES_ENCERRADAS
   ↓
RESULTADO_PRELIMINAR
   ↓
RESULTADO_DEFINITIVO
   ↓
ARQUIVADO
```

As transições deverão ser controladas no backend.

---

# 15. Edital

O edital deverá ser armazenado como arquivo PDF.

O sistema deverá registrar:

* arquivo;
* versão;
* data de publicação;
* usuário responsável pela publicação;
* processo relacionado;
* situação.

Alterações relevantes deverão ser auditadas.

Não deverá ser possível substituir silenciosamente um edital já publicado sem registro histórico.

---

# 16. Cargos e vagas

Um processo poderá possuir vários cargos.

Exemplo:

```text
Processo Seletivo 001/2026

Cargo: Auxiliar Administrativo
Vagas: 10

Cargo: Técnico de Informática
Vagas: 5

Cargo: Assistente Administrativo
Vagas: 8
```

Cada candidatura deverá estar vinculada a um cargo.

As regras de reserva de vagas, cotas e demais critérios poderão ser configuradas conforme o edital, mas o cálculo da classificação final permanece fora do escopo da versão 1.0.

---

# 17. Regras configuráveis

O sistema deverá permitir configurar regras do processo sem transformar a aplicação em um mecanismo genérico de programação.

Exemplos:

* exigir documentos;
* exigir títulos;
* permitir inscrição única;
* permitir mais de uma inscrição, quando previsto;
* exigir determinada documentação;
* utilizar avaliação de títulos;
* definir período de inscrição;
* definir cargos disponíveis.

Não deverá existir uma funcionalidade na qual o administrador escreva código ou expressões arbitrárias para determinar o funcionamento do processo.

---

# 18. Inscrição

Fluxo:

```text
Candidato acessa processo
        ↓
Consulta edital
        ↓
Escolhe cargo
        ↓
Confere dados pessoais
        ↓
Envia documentos, se exigidos
        ↓
Confirma inscrição
        ↓
Sistema gera número da inscrição
        ↓
Comprovante
        ↓
Confirmação por e-mail
```

---

# 19. Inscrição única

Por padrão:

> Um candidato poderá possuir apenas uma inscrição por processo seletivo.

Essa regra poderá ser alterada quando o edital permitir mais de uma inscrição.

A regra deverá ser validada:

* no frontend;
* no backend;
* no banco de dados, quando aplicável.

O banco de dados deverá possuir mecanismos de integridade para impedir duplicidades decorrentes de requisições simultâneas.

Exemplo:

```text
Requisição A ─┐
              ├── Banco de dados
Requisição B ─┘
```

Mesmo que duas requisições sejam enviadas simultaneamente, somente uma deverá conseguir criar a inscrição quando a regra exigir inscrição única.

---

# 20. Número da inscrição

O número da inscrição deverá ser gerado pelo backend.

O candidato não poderá escolher ou manipular o identificador da inscrição.

O sistema deverá possuir identificador interno seguro e, se necessário, número público próprio.

Identificadores internos nunca deverão ser utilizados como mecanismo de autorização.

---

# 21. Status da inscrição

A inscrição poderá possuir estados como:

```text
RECEBIDA
EM_ANALISE
DEFERIDA
INDEFERIDA
```

A alteração de situação deverá ser auditada.

Quando uma inscrição for indeferida, deverá existir registro do motivo quando essa informação fizer parte do fluxo administrativo.

---

# 22. Documentos

Os formatos permitidos inicialmente serão:

```text
PDF
JPG
JPEG
PNG
```

O sistema deverá validar:

* extensão;
* MIME type;
* assinatura/magic bytes;
* tamanho;
* integridade;
* quantidade máxima de arquivos;
* regra do processo.

A extensão do arquivo não deverá ser considerada suficiente para validação.

---

# 23. Segurança dos uploads

Os arquivos:

* não deverão ser armazenados diretamente em diretório público;
* não deverão utilizar o nome original como nome físico;
* deverão possuir identificador interno aleatório;
* deverão possuir metadados controlados pelo sistema;
* deverão ser acessados mediante autorização;
* deverão possuir limite de tamanho;
* deverão possuir limite de quantidade;
* deverão ser submetidos a antivírus/antimalware quando houver infraestrutura disponível.

Exemplo:

```text
arquivo enviado:
meu_curriculo.pdf

armazenamento:
f83c...9a21.dat
```

O nome original deverá ser armazenado apenas como metadado, quando necessário.

---

# 24. Armazenamento de arquivos

O armazenamento deverá ser privado.

Não deverá existir:

```text
/public/uploads/documento.pdf
```

com acesso direto.

O acesso deverá seguir:

```text
Usuário
   ↓
Autenticação
   ↓
Autorização
   ↓
Verificação da inscrição
   ↓
Download
```

O backend deverá verificar a autorização a cada solicitação de arquivo.

---

# 25. Área do candidato

A área do candidato deverá possuir:

```text
Área do Candidato
├── Meus Dados
├── Processos Abertos
└── Minhas Candidaturas
```

---

# 26. Minhas candidaturas

Cada candidatura deverá apresentar:

* processo;
* cargo;
* número da inscrição;
* data da inscrição;
* situação;
* documentos;
* comprovante;
* informações disponíveis sobre resultado.

O candidato somente poderá visualizar suas próprias candidaturas.

---

# 27. Comprovante de inscrição

Após a confirmação da inscrição, o sistema deverá disponibilizar comprovante em PDF.

O comprovante deverá conter, no mínimo:

* identificação do processo;
* número da inscrição;
* cargo escolhido;
* data/hora da inscrição;
* dados pessoais relevantes;
* declaração/termo aplicável;
* identificador de autenticidade, quando adotado.

O comprovante deverá representar os dados registrados no momento da inscrição.

---

# 28. Confirmação por e-mail

Após a inscrição ser efetivamente registrada, o sistema deverá enviar confirmação ao e-mail cadastrado.

O envio de e-mail não deverá comprometer a transação da inscrição.

Preferencialmente:

```text
Transação da inscrição
        ↓
Inscrição confirmada
        ↓
Evento
        ↓
Fila/processamento assíncrono
        ↓
E-mail
```

Se o envio do e-mail falhar, a inscrição não deverá ser automaticamente desfeita.

O erro deverá ser registrado para posterior tratamento.

---

# 29. Análise de documentos e títulos

Quando o edital exigir documentação ou títulos, o administrador autorizado poderá analisar os arquivos.

Cada documento poderá possuir situação:

```text
PENDENTE
APROVADO
REJEITADO
```

A análise deverá ser registrada.

Quando houver pontuação associada ao título, o sistema poderá registrar a pontuação atribuída.

---

# 30. Alteração de pontuação

A alteração manual de pontuação será considerada operação crítica.

Sempre que ocorrer uma alteração, a auditoria deverá registrar:

* candidato/inscrição;
* valor anterior;
* novo valor;
* usuário responsável;
* data e hora;
* justificativa;
* endereço IP, quando aplicável;
* identificação da operação.

Exemplo:

```text
Pontuação anterior: 8,0
Pontuação nova: 6,0
Usuário: administrador X
Motivo: correção da análise documental
Data/hora: ...
```

O sistema não deverá simplesmente sobrescrever o histórico sem preservar a operação anterior.

---

# 31. Classificação externa

O sistema deverá fornecer os dados necessários para que a classificação seja realizada externamente.

Poderá existir exportação em:

* Excel;
* CSV, caso necessário futuramente;
* PDF para relatórios administrativos.

O arquivo exportado poderá conter, conforme permissão:

* inscrição;
* candidato;
* cargo;
* dados relevantes;
* situação da inscrição;
* pontuações registradas;
* informações necessárias para classificação.

O conteúdo exportado deverá respeitar as permissões do usuário.

---

# 32. Resultados

O sistema deverá permitir publicar:

```text
Resultado Preliminar
Resultado Definitivo
```

Os resultados serão produzidos externamente e posteriormente inseridos no sistema.

O sistema deverá armazenar:

* processo;
* tipo do resultado;
* arquivo;
* data de publicação;
* usuário responsável;
* situação.

---

# 33. Recursos

Os recursos administrativos não serão recebidos pelo sistema na versão 1.0.

Os recursos serão tratados externamente, por e-mail ou outro meio administrativo definido pela Prefeitura.

Caso um recurso resulte em alteração de pontuação, o administrador autorizado poderá realizar a alteração no sistema.

A alteração deverá obrigatoriamente ser auditada.

---

# 34. Relatórios

O administrador poderá gerar relatórios conforme suas permissões.

Exemplos:

* inscrições por processo;
* inscrições por cargo;
* inscrições deferidas;
* inscrições indeferidas;
* documentos;
* títulos;
* pontuações;
* candidatos;
* dados estatísticos;
* exportação para classificação.

---

# 35. Exportação

As exportações deverão possuir controle de acesso.

Cada exportação deverá ser auditável.

A auditoria deverá registrar, quando aplicável:

* usuário;
* processo;
* tipo de relatório;
* filtros utilizados;
* data/hora;
* quantidade de registros;
* endereço IP.

Relatórios contendo dados pessoais não deverão ficar disponíveis publicamente.

---

# 36. Auditoria

Operações críticas deverão gerar registros de auditoria.

Exemplos:

* login;
* falha de autenticação;
* alteração de senha;
* recuperação de senha;
* criação de processo;
* alteração de processo;
* publicação de processo;
* encerramento de inscrição;
* criação de inscrição;
* alteração de inscrição;
* análise documental;
* alteração de pontuação;
* geração de relatório;
* exportação;
* publicação de resultado;
* alteração de permissões;
* criação/desativação de administrador;
* acesso administrativo a documentos sensíveis.

O log de auditoria deverá ser protegido contra alterações indevidas.

---

# 37. Segurança — princípio geral

O sistema deverá seguir o princípio:

> Segurança não poderá depender exclusivamente do frontend.

Toda regra crítica deverá ser validada no backend.

O sistema deverá considerar como não confiáveis:

* dados do navegador;
* parâmetros da URL;
* cookies;
* headers;
* arquivos enviados;
* identificadores recebidos;
* informações de formulários;
* requisições automatizadas.

---

# 38. Proteção contra SQL Injection

Não será permitida concatenação de parâmetros diretamente em SQL.

Deverão ser utilizados:

* JPA;
* Hibernate;
* queries parametrizadas;
* Prepared Statements;
* mecanismos seguros equivalentes.

Código como:

```java
"SELECT * FROM candidato WHERE cpf = '" + cpf + "'"
```

não deverá ser utilizado.

---

# 39. Proteção contra IDOR

O sistema deverá impedir acesso indevido por alteração de identificadores.

Exemplo proibido:

```text
/candidatura/123
```

e o simples ato de trocar para:

```text
/candidatura/124
```

não poderá permitir acesso à candidatura de outro candidato.

A autorização deverá ser verificada no backend.

A utilização de UUID não substitui autorização.

---

# 40. Proteção contra XSS

O frontend e o backend deverão tratar adequadamente entradas fornecidas pelo usuário.

Deverão ser adotados:

* escaping;
* sanitização quando necessária;
* Content Security Policy;
* validação de entrada;
* respostas HTTP seguras.

---

# 41. Proteção contra CSRF

Caso a autenticação utilize cookies, deverão ser adotadas medidas de proteção contra CSRF.

Deverão ser avaliados:

* SameSite;
* Secure;
* HttpOnly;
* tokens CSRF;
* validação de origem;
* políticas específicas do framework.

---

# 42. Sessões

As sessões deverão possuir:

* expiração;
* invalidação;
* controle de renovação;
* proteção contra fixation;
* cookies seguros;
* HttpOnly;
* Secure;
* SameSite adequado.

Sessões administrativas deverão possuir políticas mais restritivas.

---

# 43. Rate limiting

O sistema deverá implementar limitação de requisições para endpoints sensíveis.

Principalmente:

* login;
* recuperação de senha;
* criação de conta;
* inscrição;
* upload;
* download;
* geração de relatórios;
* APIs administrativas.

O rate limit deverá considerar a necessidade de evitar bloqueios indevidos de usuários legítimos.

---

# 44. Proteção contra força bruta

O sistema deverá implementar mecanismos para dificultar ataques de força bruta.

Poderão ser utilizados:

* rate limit;
* atraso progressivo;
* bloqueios temporários;
* monitoramento;
* CAPTCHA adaptativo, quando necessário;
* proteção na infraestrutura.

O sistema não deverá revelar se determinada conta existe durante tentativas de login ou recuperação de senha.

---

# 45. DDoS

A proteção contra DDoS deverá existir principalmente na camada de infraestrutura.

A arquitetura deverá permitir utilização de:

```text
Internet
   ↓
Firewall/WAF/CDN/Proteção DDoS
   ↓
Nginx
   ↓
Aplicação
   ↓
Banco
```

A aplicação deverá possuir proteções próprias de rate limit e controle de recursos, mas não deverá ser considerada responsável isoladamente pela mitigação de ataques volumétricos.

---

# 46. Headers de segurança

Deverão ser avaliados e implementados, conforme compatibilidade:

* HSTS;
* Content-Security-Policy;
* X-Content-Type-Options;
* Referrer-Policy;
* Permissions-Policy;
* políticas de frame/embedding;
* cookies seguros.

---

# 47. Criptografia

Todo tráfego deverá utilizar HTTPS/TLS.

Informações especialmente sensíveis que necessitem de proteção adicional poderão ser criptografadas em repouso.

Chaves criptográficas nunca deverão ser armazenadas diretamente no código-fonte.

---

# 48. Segredos

Nunca armazenar no Git:

```text
senha
client_secret
JWT_SECRET
chave privada
senha do banco
credencial SMTP
API key
```

Deverão ser utilizados:

* variáveis de ambiente;
* secrets manager;
* Docker Secrets;
* mecanismo institucional equivalente.

---

# 49. Banco de dados

Banco inicialmente previsto:

```text
PostgreSQL
```

O usuário da aplicação no banco deverá possuir somente as permissões necessárias.

Não deverá ser utilizado usuário administrador do PostgreSQL pela aplicação.

Deverão existir:

* índices adequados;
* constraints;
* foreign keys;
* unique constraints;
* validações;
* migrations versionadas.

---

# 50. Modelo inicial de dados

Entidades sugeridas:

```text
User
Candidate
Role
Permission
RolePermission
Process
ProcessPosition
ProcessRule
Application
ApplicationDataSnapshot
ApplicationDocument
DocumentRequirement
TitleEvaluation
Result
File
AuditLog
PasswordResetToken
Session
```

Relacionamentos simplificados:

```text
Candidate
   │
   ├── Applications
   │       │
   │       ├── ApplicationDataSnapshot
   │       ├── ApplicationDocuments
   │       └── TitleEvaluations
   │
   └── User/Auth

Process
   │
   ├── Positions
   ├── Rules
   ├── Applications
   └── Results
```

---

# 51. Separação entre autenticação e candidato

A arquitetura deverá evitar concentrar todas as responsabilidades em uma única entidade.

Preferencialmente:

```text
User
 └── autenticação/autorização

Candidate
 └── dados cadastrais

Application
 └── candidatura

ApplicationDataSnapshot
 └── dados históricos da candidatura
```

Essa separação facilita:

* segurança;
* manutenção;
* auditoria;
* evolução;
* controle de acesso.

---

# 52. Arquitetura

Stack inicial:

### Backend

```text
Java
Spring Boot
Spring Security
Spring Data JPA
Hibernate
PostgreSQL
```

### Frontend

```text
React
Next.js
```

### Infraestrutura

```text
Docker
Nginx
Redis — somente quando necessário
```

### Documentação

```text
OpenAPI
Swagger
```

### Testes

```text
JUnit
Mockito
Testcontainers
```

---

# 53. Arquitetura lógica

A aplicação deverá seguir separação de responsabilidades.

Estrutura conceitual:

```text
Presentation
      ↓
Application
      ↓
Domain
      ↓
Infrastructure
```

Exemplo:

```text
controller
application
domain
infrastructure
security
```

As regras de negócio não deverão ficar concentradas nos Controllers.

---

# 54. Controllers

Controllers deverão ser responsáveis principalmente por:

* receber requisição;
* validar estrutura da entrada;
* chamar caso de uso;
* devolver resposta.

Não deverão conter grandes blocos de regra de negócio.

Evitar:

```java
if (...)
if (...)
if (...)
if (...)
if (...)
```

concentrados no Controller.

---

# 55. Casos de uso

Operações importantes deverão ser organizadas em casos de uso.

Exemplos:

```text
CriarProcessoUseCase
PublicarProcessoUseCase
InscreverCandidatoUseCase
AnalisarDocumentoUseCase
DeferirInscricaoUseCase
IndeferirInscricaoUseCase
AlterarPontuacaoUseCase
GerarExportacaoUseCase
PublicarResultadoUseCase
```

---

# 56. SOLID

O desenvolvimento deverá seguir os princípios SOLID.

## Single Responsibility

Uma classe deverá possuir responsabilidade clara.

## Open/Closed

Novas funcionalidades deverão preferencialmente ser adicionadas por extensão, evitando alterações desnecessárias em código consolidado.

## Liskov Substitution

Implementações deverão respeitar os contratos de suas abstrações.

## Interface Segregation

Evitar interfaces gigantes.

## Dependency Inversion

Regras de negócio não deverão depender diretamente de detalhes de infraestrutura.

---

# 57. Padrões de projeto

Padrões deverão ser utilizados quando resolverem problemas reais.

Não deverão ser utilizados apenas para “ter padrões”.

---

## 57.1 State

Adequado para controlar o ciclo de vida do processo.

Exemplo:

```text
Rascunho
Publicado
Inscrições abertas
Inscrições encerradas
Resultado preliminar
Resultado definitivo
Arquivado
```

---

## 57.2 Strategy

Poderá ser utilizado quando houver comportamentos variáveis.

Exemplo:

```text
ExportacaoStrategy
 ├── ExcelExportStrategy
 └── PdfExportStrategy
```

Outro possível uso futuro:

```text
TitleScoringStrategy
```

quando houver necessidade de diferentes mecanismos de pontuação.

---

## 57.3 Specification

Poderá ser utilizada para composição de regras de consulta/validação.

Exemplo:

```text
CandidatoAtivo
AND
ProcessoAberto
AND
CargoDisponivel
```

---

## 57.4 Factory Method

Adequado para criação de diferentes tipos de relatórios/exportações.

Exemplo:

```text
ExportFactory
 ├── ExcelExporter
 └── PdfExporter
```

---

## 57.5 Observer / eventos

Pode ser utilizado para eventos como:

```text
InscricaoCriada
       ↓
Auditoria
       ↓
Envio de confirmação
```

O mecanismo de eventos deverá evitar acoplamento desnecessário.

---

## 57.6 Builder

Poderá ser utilizado para construção de objetos complexos, especialmente:

* relatórios;
* comprovantes;
* objetos de configuração.

---

## 57.7 Proxy/Decorator

Poderá ser utilizado para comportamentos transversais como:

* auditoria;
* autorização;
* métricas;
* logging.

Entretanto, recursos nativos do Spring deverão ser utilizados quando forem suficientes.

---

# 58. Regra contra overengineering

Não criar abstrações apenas para cumprir um padrão.

Exemplo:

Se existir apenas uma implementação simples:

```java
interface DocumentoService
```

não deverá necessariamente existir uma fábrica, cinco interfaces, três decorators e duas estratégias.

A arquitetura deverá equilibrar:

```text
segurança
manutenção
clareza
extensibilidade
simplicidade
```

---

# 59. Performance e consumo de memória

O sistema deverá evitar consumo desnecessário de RAM.

Isso não significa micro-otimização prematura.

Deverão ser priorizadas práticas como:

* paginação;
* consultas específicas;
* DTOs;
* projections quando apropriado;
* evitar `SELECT *`;
* evitar N+1;
* processamento em streaming;
* limites de upload;
* processamento assíncrono;
* filas para tarefas pesadas;
* exportações em lotes;
* fechamento adequado de recursos;
* evitar carregar milhares de registros em memória;
* evitar cache desnecessário.

---

# 60. Redis

Redis não deverá ser utilizado simplesmente porque faz parte da stack.

Será utilizado somente quando houver justificativa.

Possíveis aplicações:

* rate limiting distribuído;
* controle de sessões, se necessário;
* cache;
* locks distribuídos;
* filas simples, quando apropriado.

Se não houver necessidade real, a aplicação deverá funcionar sem Redis.

---

# 61. Exportações grandes

Exportações que possam consumir muita memória deverão utilizar processamento em streaming ou em lotes.

Evitar:

```java
List<Candidato> todos = repository.findAll();
```

para grandes volumes.

Preferir mecanismos paginados/streaming.

---

# 62. Frontend

O frontend deverá ser responsável por:

* experiência do usuário;
* validação preliminar;
* máscaras;
* feedback;
* navegação;
* apresentação.

Não deverá ser responsável por decisões de segurança.

Toda validação relevante deverá existir também no backend.

---

# 63. API

A API deverá seguir princípios REST quando aplicáveis.

As respostas deverão utilizar DTOs.

Entidades JPA não deverão ser expostas diretamente.

Exemplo:

```text
Entity
   ↓
Mapper
   ↓
DTO
   ↓
API
```

---

# 64. Tratamento de erros

A API deverá possuir padrão consistente de erro.

Não deverá retornar:

* stack trace;
* SQL;
* nomes de tabelas;
* informações internas;
* credenciais;
* detalhes que auxiliem exploração da aplicação.

Erros deverão possuir mensagens adequadas ao consumidor.

Logs internos poderão possuir detalhes técnicos controlados.

---

# 65. LGPD

O sistema deverá seguir os princípios da LGPD.

Deverão ser observados, entre outros:

* finalidade;
* adequação;
* necessidade;
* livre acesso;
* qualidade dos dados;
* transparência;
* segurança;
* prevenção;
* não discriminação;
* responsabilização.

O sistema deverá aplicar minimização de dados.

Não deverá coletar informação apenas porque tecnicamente é possível.

---

# 66. Controle de acesso a dados pessoais

O acesso administrativo deverá seguir o princípio do menor privilégio.

Um administrador que não necessita acessar determinada informação não deverá possuir permissão para visualizá-la.

Informações especialmente sensíveis deverão possuir controles adicionais.

---

# 67. Logs e LGPD

Logs não deverão armazenar desnecessariamente:

* senhas;
* tokens;
* documentos;
* dados pessoais completos;
* informações sensíveis.

Quando necessário, dados deverão ser mascarados.

Exemplo:

```text
CPF:
***.***.***-42
```

---

# 68. Backup

Deverão existir backups do:

* banco de dados;
* arquivos;
* configurações essenciais.

Os backups deverão possuir:

* controle de acesso;
* proteção contra alteração indevida;
* política de retenção;
* testes de restauração.

Backup que nunca foi restaurado/testado não deverá ser considerado plenamente confiável.

---

# 69. Testes

O sistema deverá possuir testes:

### Unitários

Para regras isoladas.

### Integração

Para:

* banco;
* repositórios;
* segurança;
* casos de uso.

### End-to-End

Para fluxos críticos.

### Segurança

Para:

* SQL Injection;
* XSS;
* CSRF;
* IDOR;
* privilege escalation;
* brute force;
* upload malicioso;
* acesso indevido;
* manipulação de identificadores.

---

# 70. Teste de concorrência

O sistema deverá possuir testes específicos para operações críticas concorrentes.

Principalmente:

```text
Inscrição única
```

Duas requisições simultâneas não poderão criar duas inscrições quando o edital permitir apenas uma.

---

# 71. Testes de autorização

Deverão existir testes verificando:

```text
Candidato A → candidatura A = permitido

Candidato A → candidatura B = negado

Administrador sem permissão → operação administrativa = negado

Administrador com permissão → operação = permitido
```

---

# 72. Docker

O ambiente deverá ser reproduzível.

Os serviços poderão ser organizados inicialmente como:

```text
Frontend
Backend
PostgreSQL
Nginx
Redis (quando necessário)
```

As imagens deverão ser enxutas sempre que possível.

Não deverão ser executados serviços desnecessários.

---

# 73. Controle de dependências

Toda dependência deverá possuir justificativa.

Evitar bibliotecas:

* abandonadas;
* desnecessárias;
* redundantes;
* com vulnerabilidades conhecidas.

As dependências deverão ser atualizadas de maneira controlada.

---

# 74. Migrations

Alterações no banco deverão ser realizadas por migrations versionadas.

Exemplo:

```text
V001__create_users.sql
V002__create_candidates.sql
V003__create_processes.sql
V004__create_applications.sql
```

Após uma migration ter sido aplicada em ambiente compartilhado, ela não deverá ser simplesmente modificada.

Uma nova migration deverá ser criada para alterações posteriores.

---

# 75. Integridade do banco

Regras críticas deverão existir também no banco quando apropriado.

Exemplos:

* CPF único;
* relacionamentos;
* foreign keys;
* campos obrigatórios;
* constraints;
* unicidade de inscrição.

A aplicação não deverá depender exclusivamente de validações JavaScript.

---

# 76. Segurança dos administradores

Contas administrativas deverão possuir segurança superior às contas comuns.

Recomenda-se:

* MFA;
* senha forte;
* sessão mais curta;
* auditoria reforçada;
* rate limit;
* bloqueio contra tentativas repetidas;
* menor privilégio.

A adoção de MFA deverá ser considerada requisito de segurança para o ambiente administrativo.

---

# 77. Princípio de menor privilégio

Todo usuário deverá possuir somente as permissões necessárias para desempenhar sua função.

Isso deverá ser aplicado a:

* usuários;
* administradores;
* banco de dados;
* containers;
* arquivos;
* serviços;
* infraestrutura.

---

# 78. Auditoria imutável

Registros de auditoria não deverão ser facilmente apagados ou alterados pelo administrador comum.

A capacidade de consultar auditoria não deverá implicar capacidade de alterar os registros.

Operações excepcionais relacionadas à auditoria também deverão ser registradas.

---

# 79. Controle de acesso por processo

Quando necessário, administradores poderão ser vinculados a processos específicos.

Exemplo:

```text
Administrador A
 ├── Processo 001/2026
 └── Processo 003/2026

Administrador B
 └── Processo 002/2026
```

Isso deverá ser implementado sem criar regras específicas diretamente no código.

---

# 80. Fluxo administrativo

Fluxo principal:

```text
Administrador
      ↓
Cria processo
      ↓
Configura processo
      ↓
Cadastra cargos
      ↓
Anexa edital
      ↓
Publica
      ↓
Abre inscrições
      ↓
Recebe inscrições
      ↓
Analisa documentos
      ↓
Deferimento/indeferimento
      ↓
Registra pontuações, quando aplicável
      ↓
Exporta dados
      ↓
Classificação externa
      ↓
Publica resultado preliminar
      ↓
Tratamento externo dos recursos
      ↓
Ajustes necessários
      ↓
Publica resultado definitivo
```

---

# 81. Fluxo do candidato

```text
Cadastro
   ↓
Login
   ↓
Processos abertos
   ↓
Seleciona processo
   ↓
Consulta edital
   ↓
Escolhe cargo
   ↓
Confere dados
   ↓
Envia documentos
   ↓
Confirma inscrição
   ↓
Recebe comprovante
   ↓
Recebe confirmação por e-mail
   ↓
Acompanha candidatura
   ↓
Consulta resultado
```

---

# 82. Identificadores e segurança

IDs internos não deverão ser considerados secretos nem utilizados como mecanismo de segurança.

Sempre deverá existir autorização sobre o recurso.

Mesmo que sejam utilizados UUIDs:

```text
UUID ≠ autorização
```

A regra correta será:

```text
Usuário autenticado
       +
Permissão
       +
Propriedade/escopo do recurso
       =
Acesso autorizado
```

---

# 83. Proteção de documentos publicados

Resultados publicados deverão observar a necessidade de proteção de dados.

O sistema deverá evitar publicação desnecessária de:

* CPF completo;
* endereço;
* telefone;
* e-mail;
* documentos pessoais;
* dados sensíveis.

A publicação deverá seguir o edital e as regras administrativas aplicáveis.

---

# 84. Princípios de desenvolvimento com IA

A inteligência artificial poderá auxiliar no desenvolvimento, mas deverá obedecer rigorosamente às regras arquiteturais do projeto.

A IA não poderá:

* desabilitar segurança para resolver erro;
* remover autenticação para facilitar testes;
* remover autorização;
* colocar senha no código;
* colocar secrets no Git;
* concatenar SQL;
* expor entidades diretamente;
* ignorar validações;
* remover testes apenas para fazê-los passar;
* alterar migrations antigas sem justificativa;
* criar código excessivamente complexo;
* introduzir padrões sem necessidade;
* modificar partes não relacionadas ao requisito.

---

# 85. Regra de alterações da IA

Ao receber uma solicitação de desenvolvimento, a IA deverá:

1. entender a arquitetura atual;
2. localizar os arquivos relacionados;
3. verificar dependências;
4. identificar regras existentes;
5. realizar a menor alteração necessária;
6. criar/alterar testes;
7. verificar segurança;
8. verificar impacto no banco;
9. executar testes;
10. informar alterações realizadas.

---

# 86. Regra de segurança da IA

Nenhuma alteração deverá reduzir a segurança existente.

Caso uma implementação entre em conflito com um requisito de segurança, deverá ser adotada a alternativa mais segura.

A IA não deverá aceitar solicitações como:

```text
"Desabilite o CSRF porque está dando erro."
```

sem analisar primeiro a arquitetura de autenticação e apresentar uma solução adequada.

---

# 87. Regra contra complexidade excessiva

O código deverá ser simples.

Evitar:

* métodos gigantes;
* classes com muitas responsabilidades;
* Controllers contendo regras de negócio;
* `if/else` excessivos;
* duplicação;
* abstrações desnecessárias;
* dependências circulares.

Entretanto:

> Um padrão de projeto não deverá ser criado apenas para eliminar um `if`.

A decisão deverá considerar a complexidade real do domínio.

---

# 88. Qualidade do código

O código deverá priorizar:

```text
Legibilidade
Manutenibilidade
Segurança
Testabilidade
Baixo acoplamento
Alta coesão
Performance
```

A otimização de RAM deverá ocorrer principalmente através de boa arquitetura e controle do volume de dados.

---

# 89. Definition of Done

Uma funcionalidade somente deverá ser considerada concluída quando:

* código implementado;
* regras de negócio atendidas;
* validação frontend implementada quando aplicável;
* validação backend implementada;
* autorização implementada;
* testes criados/atualizados;
* tratamento de erros implementado;
* auditoria implementada quando necessária;
* documentação atualizada;
* migrations criadas quando necessárias;
* vulnerabilidades conhecidas avaliadas;
* testes executados com sucesso.

---

# 90. Requisitos de segurança obrigatórios

A versão de produção deverá possuir, no mínimo:

* HTTPS;
* hash seguro de senhas;
* controle de sessão;
* expiração de sessão;
* proteção contra SQL Injection;
* proteção contra XSS;
* proteção contra CSRF quando aplicável;
* proteção contra IDOR;
* RBAC;
* rate limiting;
* proteção contra brute force;
* recuperação segura de senha;
* proteção de uploads;
* armazenamento privado de arquivos;
* validação frontend/backend;
* auditoria;
* logs seguros;
* gestão de secrets;
* backups;
* controle de permissões;
* proteção de dados pessoais;
* headers de segurança;
* controle de acesso administrativo;
* testes de segurança.

---

# 91. Estrutura inicial sugerida do projeto

Uma estrutura possível para o backend:

```text
src/
└── main/
    └── java/
        └── br.gov.pmps.processoseletivo/
            ├── application/
            │   ├── usecase/
            │   ├── dto/
            │   └── service/
            │
            ├── domain/
            │   ├── model/
            │   ├── repository/
            │   ├── service/
            │   └── rule/
            │
            ├── infrastructure/
            │   ├── persistence/
            │   ├── storage/
            │   ├── email/
            │   └── configuration/
            │
            ├── presentation/
            │   ├── controller/
            │   └── mapper/
            │
            ├── security/
            └── shared/
```

A estrutura poderá ser adaptada conforme a evolução do projeto.

---

# 92. Estrutura inicial do frontend

Sugestão:

```text
src/
├── app/
├── components/
├── features/
│   ├── auth/
│   ├── candidate/
│   ├── process/
│   ├── application/
│   ├── documents/
│   ├── results/
│   └── administration/
├── services/
├── hooks/
├── lib/
├── types/
└── utils/
```

O frontend deverá evitar centralizar toda a aplicação em componentes gigantes.

---

# 93. API inicial

Exemplos de endpoints:

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout
POST   /api/auth/forgot-password
POST   /api/auth/reset-password

GET    /api/candidate/me
PUT    /api/candidate/me

GET    /api/processes
GET    /api/processes/{id}

POST   /api/applications
GET    /api/applications
GET    /api/applications/{id}

POST   /api/applications/{id}/documents
GET    /api/applications/{id}/documents/{documentId}

GET    /api/applications/{id}/receipt

GET    /api/admin/processes
POST   /api/admin/processes
PUT    /api/admin/processes/{id}
POST   /api/admin/processes/{id}/publish

GET    /api/admin/applications
POST   /api/admin/applications/{id}/defer
POST   /api/admin/applications/{id}/deny

POST   /api/admin/applications/{id}/score

POST   /api/admin/processes/{id}/exports

POST   /api/admin/processes/{id}/results
```

Os endpoints são apenas referência inicial e deverão ser refinados durante a implementação.

---

# 94. Estratégia de desenvolvimento

O desenvolvimento deverá ser realizado por etapas.

## Fase 1 — Fundação

* criação dos projetos;
* Docker;
* PostgreSQL;
* migrations;
* configuração;
* segurança básica;
* estrutura arquitetural.

## Fase 2 — Autenticação

* cadastro;
* login;
* logout;
* recuperação de senha;
* sessões;
* segurança.

## Fase 3 — Candidato

* dados pessoais;
* edição;
* área do candidato;
* processos disponíveis.

## Fase 4 — Processos

* criação;
* edição;
* cargos;
* vagas;
* regras;
* edital;
* publicação.

## Fase 5 — Inscrições

* inscrição;
* validações;
* snapshot;
* comprovante;
* e-mail.

## Fase 6 — Documentos

* upload;
* armazenamento;
* análise;
* segurança;
* títulos.

## Fase 7 — Administração

* deferimento;
* indeferimento;
* pontuação;
* auditoria.

## Fase 8 — Exportações

* Excel;
* PDF;
* filtros;
* processamento eficiente.

## Fase 9 — Resultados

* resultado preliminar;
* resultado definitivo;
* publicação.

## Fase 10 — Segurança e homologação

* testes de segurança;
* testes de carga;
* testes de concorrência;
* revisão de permissões;
* revisão LGPD;
* backup/restauração;
* homologação.

---

# 95. Critério de arquitetura

A aplicação deverá evitar dois extremos:

### Subengenharia

Código rápido, porém:

* inseguro;
* difícil de testar;
* acoplado;
* difícil de manter.

### Overengineering

Código:

* excessivamente abstrato;
* cheio de padrões;
* difícil de entender;
* com dependências desnecessárias;
* com consumo elevado de recursos.

O objetivo deverá ser:

> Arquitetura suficientemente robusta para a criticidade do sistema, mas simples o bastante para ser mantida pela equipe responsável.

---

# 96. Regra geral de evolução

Novos requisitos deverão ser analisados considerando:

1. impacto no domínio;
2. impacto na segurança;
3. impacto na LGPD;
4. impacto no banco;
5. impacto na API;
6. impacto no frontend;
7. impacto na auditoria;
8. impacto na performance;
9. impacto nos testes;
10. compatibilidade com funcionalidades existentes.

Nenhum novo recurso deverá ser implementado isoladamente sem avaliar seus impactos.

---

# 97. Documento complementar — AI_RULES.md

O projeto deverá possuir um arquivo:

```text
AI_RULES.md
```

Esse arquivo deverá conter as regras destinadas às ferramentas de inteligência artificial utilizadas durante o desenvolvimento.

Conteúdo mínimo:

```text
# AI RULES

1. Preserve a arquitetura existente.
2. Não modifique arquivos não relacionados à tarefa.
3. Não remova mecanismos de segurança.
4. Não desabilite autenticação ou autorização.
5. Não coloque secrets no código.
6. Nunca concatene SQL com entrada do usuário.
7. Não exponha entidades diretamente pela API.
8. Não ignore validações de backend.
9. Não remova testes para corrigir falhas.
10. Toda nova regra de negócio deve possuir testes.
11. Toda alteração de banco deve utilizar migration.
12. Não alterar migrations já aplicadas sem autorização explícita.
13. Não criar padrões de projeto sem necessidade.
14. Evitar complexidade ciclomática desnecessária.
15. Evitar classes e métodos excessivamente grandes.
16. Aplicar SOLID.
17. Garantir autorização em todos os recursos protegidos.
18. Proteger uploads.
19. Não registrar secrets ou dados pessoais desnecessários em logs.
20. Toda alteração crítica deve ser auditável.
21. Preferir processamento paginado/streaming para grandes volumes.
22. Não introduzir dependências sem justificativa.
23. Executar testes após alterações.
24. Corrigir vulnerabilidades sem reduzir outras camadas de segurança.
25. Antes de grandes mudanças arquiteturais, explicar o impacto.
```

---

# 98. Resultado esperado

Ao final da implementação, o sistema deverá fornecer uma plataforma segura e centralizada para gerenciamento dos processos seletivos municipais.

O sistema deverá permitir:

```text
Prefeitura
    ↓
Cria processo
    ↓
Publica edital
    ↓
Abre inscrições
    ↓
Candidatos se inscrevem
    ↓
Documentos são enviados
    ↓
Administração analisa
    ↓
Dados são exportados
    ↓
Classificação externa
    ↓
Resultado oficial é publicado
    ↓
Candidato consulta resultado
```

A solução deverá manter histórico, segurança, rastreabilidade e proteção dos dados durante todo o ciclo de vida do processo.

---

# 99. Princípio final

O sistema deverá ser desenvolvido considerando que os dados tratados possuem caráter pessoal e que parte deles poderá possuir elevado grau de sensibilidade.

Assim, segurança, privacidade, auditoria e integridade não deverão ser funcionalidades adicionais.

Deverão fazer parte da arquitetura desde o início.

A implementação deverá buscar o seguinte equilíbrio:

```text
SEGURANÇA
     +
PRIVACIDADE
     +
SIMPLICIDADE
     +
MANUTENIBILIDADE
     +
PERFORMANCE
     +
AUDITABILIDADE
     =
SISTEMA INSTITUCIONAL ROBUSTO
```

**Fim da Especificação v1.0**
