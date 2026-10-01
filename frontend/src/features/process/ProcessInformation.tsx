import type { ReactNode } from "react";
import { formatDateTime, type ProcessDetail, STATUS_LABELS } from "./types";

interface ProcessInformationProps {
  process: ProcessDetail;
  /** Prefixo da URL de download do edital (pública ou administrativa). */
  noticeBaseUrl: string;
  /** Ação exibida ao lado de cada cargo (ex.: botão de inscrição). */
  positionAction?: (positionId: string) => ReactNode;
}

/** Dados do processo, cargos, documentos exigidos e versões do edital. */
export function ProcessInformation({ process, noticeBaseUrl, positionAction }: ProcessInformationProps) {
  return (
    <>
      <p>
        <strong>Situação:</strong> {STATUS_LABELS[process.status]}
        <br />
        <strong>Secretaria:</strong> {process.department}
        <br />
        <strong>Inscrições:</strong> {formatDateTime(process.registrationStart)} a{" "}
        {formatDateTime(process.registrationEnd)} (horário de Brasília)
        <br />
        <strong>Inscrição:</strong>{" "}
        {process.multipleApplicationsAllowed ? "permitida uma por cargo" : "uma por candidato"}
        <br />
        <strong>Avaliação de títulos:</strong> {process.titleEvaluationEnabled ? "sim" : "não"}
      </p>

      <h2>Cargos</h2>
      {process.positions.length === 0 ? (
        <p className="hint">Nenhum cargo cadastrado.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th scope="col">Cargo</th>
              <th scope="col">Vagas</th>
              {positionAction && <th scope="col"><span className="hint">Ação</span></th>}
            </tr>
          </thead>
          <tbody>
            {process.positions.map((position) => (
              <tr key={position.id}>
                <td>{position.name}</td>
                <td>{position.vacancies}</td>
                {positionAction && <td>{positionAction(position.id)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Documentos exigidos</h2>
      {process.documentRequirements.length === 0 ? (
        <p className="hint">Nenhum documento exigido.</p>
      ) : (
        <ul>
          {process.documentRequirements.map((requirement) => (
            <li key={requirement.id}>
              {requirement.name} — {requirement.mandatory ? "obrigatório" : "opcional"}
              {requirement.title && " (título)"}
              {requirement.description && <span className="hint"> · {requirement.description}</span>}
            </li>
          ))}
        </ul>
      )}

      <h2>Edital</h2>
      {process.notices.length === 0 ? (
        <p className="hint">Nenhum edital anexado.</p>
      ) : (
        <ul>
          {process.notices.map((notice) => (
            <li key={notice.version}>
              <a href={`${noticeBaseUrl}/${notice.version}/file`}>Versão {notice.version} (PDF)</a>
              {notice.status === "CURRENT" && " — vigente"}
              {notice.status === "SUPERSEDED" && " — substituída"}
              {notice.status === "DRAFT" && " — rascunho (não publicado)"}
              {notice.publishedAt && <span className="hint"> · publicada em {formatDateTime(notice.publishedAt)}</span>}
              {notice.changeReason && <span className="hint"> · motivo: {notice.changeReason}</span>}
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
