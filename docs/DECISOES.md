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
