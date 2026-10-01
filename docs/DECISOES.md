# Decisões complementares à especificação

Este documento registra decisões de negócio e técnicas tomadas após a especificação v1.0 (`ESPECIFICACAO.md`).
Em caso de conflito, prevalece a decisão mais recente registrada aqui, desde que não reduza segurança ou proteção de dados (`AI_RULES.md` §97).

Data das decisões: 30/09/2026 e 01/10/2026.

## Autenticação e sessão

| Tema | Decisão |
|---|---|
| Identificador de login | CPF, para candidatos e administradores |
| Mecanismo de sessão | Sessão no servidor, com cookie `HttpOnly`, `Secure`, `SameSite=Strict`; sem JWT |
| Expiração (candidato) | 1 hora de inatividade |
| Expiração (administrador) | 30 minutos de inatividade, com limite absoluto de 8 horas |
| CSRF | Token em cookie lido pelo frontend e reenviado em header |

| Contas | Candidato e administrador são contas separadas. O mesmo CPF pode ter uma conta de candidato e uma conta administrativa, com login, sessão e recuperação de senha independentes |
| E-mail | Único por conta (dentro de cada tipo de conta) |
| MFA de administradores | Implementado na fase 7 (gestão de administradores) |
| Bloqueio por tentativas | 5 falhas consecutivas de login bloqueiam a conta por 15 minutos; a mensagem de erro é sempre genérica |
| Limite de requisições | Por IP no Nginx para os endpoints de autenticação; por conta na aplicação (bloqueio acima) |
| Recuperação de senha | Token aleatório de uso único, armazenado apenas como hash, válido por 30 minutos; enviado no fragmento da URL (`#token=`) para não aparecer em logs de servidor. Após a troca, todas as sessões da conta são encerradas |
| Primeiro Administrador Geral | Criado por provisionamento único, habilitado por variáveis de ambiente e somente quando não existe nenhum administrador. Não há senha inicial: o administrador recebe por e-mail um link de definição de senha válido por 24 horas |

## Cadastro do candidato

Todos os campos do §7 são obrigatórios, exceto o complemento do endereço.

## Política de senha

- Mínimo de 8 caracteres.
- Deve conter letra maiúscula, letra minúscula, número e caractere especial.
- Não pode conter o nome ou sobrenome do usuário. Considera-se cada parte do nome com 3 letras ou mais, sem diferenciar maiúsculas/minúsculas e acentos; partículas como "da", "de", "do", "dos", "das" e "e" são ignoradas.
- Não pode conter a data de nascimento completa (formatos `ddMMyyyy`, `ddMMyy`, `yyyyMMdd`, com ou sem separadores).

## Processo seletivo

- Estados adicionais: `CANCELADO` (definitivo) e `SUSPENSO` (retorna ao estado anterior, com motivo registrado). Ambos bloqueiam novas inscrições.
- `INSCRICOES_ABERTAS` e `INSCRICOES_ENCERRADAS` mudam automaticamente conforme as datas do período de inscrição.
- Retorno de estado é permitido com motivo obrigatório e auditoria, exceto: processos com inscrições encerradas **não podem ser reabertos**.
- Lista de processos, edital e resultados podem ser públicos.

## Inscrição

- Padrão: uma inscrição por processo. Quando o processo permitir mais de uma, o limite é **uma inscrição por cargo**.
- Fluxo com **inscrição em rascunho**: o candidato escolhe o cargo, o sistema cria um rascunho (sem número, não conta como inscrição), os documentos são anexados ao rascunho e a confirmação gera número, snapshot e comprovante. Rascunhos não confirmados são descartados após o encerramento das inscrições.
- Após a confirmação, o candidato **não pode** cancelar a inscrição, trocar de cargo, nem trocar ou adicionar documentos.
- O texto do termo do comprovante será proposto na fase de inscrições e revisado pela Prefeitura.

## Documentos

- Tamanho máximo: 2 MB por arquivo.
- Quantidade máxima: 20 arquivos por inscrição.
- Formatos: PDF, JPG, JPEG e PNG (especificação §22).

## Pessoa com deficiência

- Quando "Pessoa com deficiência" = SIM, o campo "Necessidade de adaptações" é obrigatório.
- Opções: Intérprete de Libras, Auxílio Ledor, Prova Ampliada, Nenhuma.

## Retenção de dados (LGPD)

- Registros de acesso: guardados por no mínimo 6 meses (referência: Marco Civil da Internet, art. 15).
- Inscrições, documentos e auditoria: prazo conforme a tabela de temporalidade de documentos da Prefeitura, configurável no sistema. Nenhum prazo foi definido pela equipe de desenvolvimento.
- Exclusão de conta: permitida somente para candidatos sem inscrições confirmadas. Com inscrições, os dados são mantidos por cumprimento de obrigação legal (LGPD, art. 16, I).

## Infraestrutura e build

- Build e testes executados via Docker (não há dependência de JDK ou Node na máquina do desenvolvedor).
- Backend: Java 21, Spring Boot 4.1. Frontend: Node 24, Next.js 16, React 19.
- PostgreSQL 18 com dois usuários: dono do schema `app` (usado apenas pelo Flyway) e usuário da aplicação (apenas DML).
- Frontend e API servidos na mesma origem pelo Nginx; CORS não é habilitado.
- Redis não é utilizado até haver necessidade concreta (especificação §60).

## Meus Dados (fase 3)

- O candidato pode alterar todos os dados do cadastro, exceto o CPF.
- Alterações não afetam inscrições já realizadas (snapshot, especificação §8).
- Troca de e-mail e troca de senha com sessão ativa exigem a senha atual. Erros de senha atual contam para o bloqueio da conta.
- Após troca de e-mail, o endereço anterior recebe aviso. Após troca ou redefinição de senha, o titular recebe aviso por e-mail.
- Troca de senha com sessão ativa encerra as demais sessões da conta e mantém a sessão atual.
- A auditoria registra apenas os nomes dos campos alterados, nunca os valores.

## Processos seletivos (fase 4)

- Escopo por processo: não há vínculo entre administrador e processo. Todo administrador enxerga todos os processos; o que pode fazer é limitado pelas permissões (RBAC). A especificação (§79) prevê o vínculo apenas "quando necessário".
- Cargo: somente nome e quantidade de vagas. Demais informações ficam no edital (PDF).
- Período de inscrição após a publicação: permitido apenas prorrogar a data final, antes do encerramento, com motivo obrigatório e auditoria. Não é permitido antecipar o fim nem alterar o início.
- Processo publicado não volta a rascunho. Correções são feitas por nova versão do edital (retificação), com motivo, mantendo o histórico das versões anteriores.
- Após a publicação, dados do processo, cargos, vagas e documentos exigidos não são alterados pelo sistema; mudanças ocorrem por retificação do edital. **(Proposta da equipe de desenvolvimento, pendente de confirmação: retificações que alterem vagas ou cargos podem exigir edição desses dados.)**
- Edital: somente PDF, até 10 MB (limite técnico definido pela equipe de desenvolvimento; ajustável em `app.files.notice-max-size`).
- Abertura e encerramento automáticos das inscrições são verificados a cada minuto. Independentemente disso, uma inscrição só é aceita se a data atual estiver dentro do período.
- Horários são armazenados em UTC e exibidos no horário de Brasília.

## Inscrições (fase 5)

- Número da inscrição: gerado pelo sistema, sequencial por processo, no formato `<número do processo>/<ano>-<sequencial de 5 dígitos>` (ex.: 001/2026-00001). Número e ano do processo continuam informados pelo administrador na criação.
- Comprovante em PDF com código de autenticidade. Uma página pública confirma, a partir do código, número da inscrição, processo, cargo e data/hora, sem dados pessoais.
- A inscrição só pode ser confirmada com todos os documentos obrigatórios enviados.
- Documentos: PDF, JPG, JPEG ou PNG; até 2 MB cada; até 20 arquivos por inscrição; mais de um arquivo por documento exigido é permitido (ex.: vários certificados de um título).
- Rascunhos não confirmados são descartados automaticamente quando o processo deixa de aceitar inscrições.
- O e-mail de confirmação usa fila (outbox) no banco: falhas de envio ficam registradas e são tentadas novamente, sem desfazer a inscrição (especificação §28).
- O comprovante contém nome, CPF mascarado, processo, cargo, número, data/hora, necessidade de adaptação declarada, termo de declaração e código de autenticidade.

## Deferimento e indeferimento (fase 6)

Decisão da Prefeitura, que altera o escopo da especificação (§29, §30 e §55):

- Não há análise de documento por documento no sistema. O administrador consulta os documentos enviados e decide sobre a inscrição: **deferida** ou **indeferida**.
- O indeferimento exige justificativa. A justificativa é exibida ao candidato.
- A decisão pode ser alterada depois (ex.: após recurso), sempre com justificativa. Todo o histórico (situação anterior, nova, justificativa, responsável, data/hora) é mantido e não pode ser alterado.
- Não existe a situação "Em análise". Uma inscrição confirmada fica "Recebida" até a decisão.
- Notas de provas e de títulos **não** são registradas no sistema: são divulgadas em PDF externo (publicação de resultados, fase 9).

Regras técnicas adotadas pela equipe de desenvolvimento (ajustáveis):

- Decisões são permitidas com inscrições encerradas e antes da etapa Resultado Final. Ficam bloqueadas na etapa Resultado Final e com o processo arquivado, cancelado ou suspenso.
- Deferir exige a permissão `INSCRICAO_DEFERIR`; indeferir exige `INSCRICAO_INDEFERIR`; consultar exige `INSCRICAO_VISUALIZAR`.
- Dados de pessoa com deficiência e necessidade de adaptações só aparecem para quem tem a permissão `DADOS_PCD_VISUALIZAR` (especificação §9 e §66). O Administrador Geral recebe essa permissão.
- O download de documento de candidato por administrador é registrado na auditoria (especificação §36).
- Fluxo após o encerramento das inscrições: análise (deferimento/indeferimento), período de recurso (tratado fora do sistema) e revisão dos indeferimentos. Todas essas etapas ocorrem com o processo em "inscrições encerradas", em que as decisões são permitidas; revisões exigem justificativa.
- O candidato não recebe e-mail sobre a decisão: a relação é publicada no Diário Oficial e pode ser anexada ao processo em PDF (publicações, fase 9).
- A coluna de situação por documento foi removida (V010), com autorização da Prefeitura.

## Documentos do Processo

- O administrador anexa ao processo qualquer documento referente a ele (ex.: relação de inscrições deferidas e indeferidas, resultado de recursos, resultados, comunicados), informando o nome do documento.
- Formato PDF, até 10 MB (mesmo limite do edital).
- Os documentos são públicos na página do processo, exceto enquanto o processo é rascunho.
- Documento publicado não é apagado: pode ser retirado da página pública com justificativa; o registro e o arquivo continuam disponíveis para a área administrativa e a retirada é auditada (AI_RULES §22 e §69).
- Permissão: `RESULTADO_PUBLICAR`.

## Etapa do processo

- Cada processo publicado tem uma **etapa**, definida manualmente pelo administrador a partir de lista fixa: Edital Disponível, Gabarito Disponível, Resultado Preliminar e Resultado Final. Novas opções exigem ajuste no sistema.
- A etapa é independente da situação das inscrições, que continua automática pelas datas (Publicado, Inscrições abertas, Inscrições encerradas), além de Suspenso, Cancelado e Arquivado.
- As situações internas "Resultado preliminar" e "Resultado definitivo" foram substituídas pela etapa (V012).
- Ao publicar, a etapa inicial é Edital Disponível. O administrador pode escolher qualquer etapa da lista; toda mudança fica em histórico e na auditoria. Permissão: `RESULTADO_PUBLICAR`.
- Arquivar exige inscrições encerradas e etapa Resultado Final.

## Administração (fase 7)

- **MFA obrigatório** para todas as contas administrativas (especificação §76): aplicativo autenticador (TOTP, 6 dígitos, 30 s). No primeiro acesso, o administrador cadastra o aplicativo lendo um QR code. Códigos já usados não são aceitos de novo, e códigos errados contam para o bloqueio da conta.
- O segredo do MFA é cifrado no banco (AES-256-GCM) com a chave `MFA_ENCRYPTION_KEY`, mantida fora do banco e do código.
- Se o administrador perder o celular, quem tem `USUARIO_GERENCIAR` redefine o segundo fator; no próximo acesso ele cadastra o aplicativo de novo.
- Novos administradores são criados sem senha: recebem por e-mail um link de definição de senha válido por 24 horas.
- Ninguém altera os próprios perfis, desativa a própria conta ou redefine o próprio segundo fator. Deve existir sempre ao menos um Administrador Geral ativo.
- Ao mudar perfis ou permissões, as sessões dos administradores afetados são encerradas para que as novas permissões valham no próximo acesso.
- O perfil Administrador Geral é do sistema e não pode ser alterado. Perfis personalizados só podem ser excluídos sem administradores vinculados.
- A auditoria é somente consulta (`AUDITORIA_VISUALIZAR`). Para ações de candidatos, a consulta mostra apenas o tipo de conta, não o nome (minimização).
