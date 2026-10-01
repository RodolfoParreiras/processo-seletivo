"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { type AdminContext, AdminShell } from "@/features/admin/AdminShell";
import { APPLICATION_STATUS_LABELS, type ApplicationStatus } from "@/features/application/types";
import { formatDateTime } from "@/features/process/types";
import { ApiError, apiGet, apiPost } from "@/lib/api";
import { ADAPTATIONS, maskCpf, maskPhone } from "@/lib/validation";

interface AdminApplicationDetail {
  id: string;
  processId: string;
  processNumber: string;
  processTitle: string;
  positionName: string;
  status: ApplicationStatus;
  applicationNumber: string;
  confirmedAt: string;
  decisionReason: string | null;
  decisionsAllowed: boolean;
  candidate: {
    fullName: string;
    cpf: string;
    birthDate: string;
    motherName: string;
    email: string;
    phone: string;
    city: string;
    uf: string;
    hasDisability: boolean | null;
    adaptations: string[] | null;
  };
  documents: { id: string; requirementName: string; title: boolean; originalName: string; sizeBytes: number; uploadedAt: string }[];
  decisions: { fromStatus: ApplicationStatus; toStatus: ApplicationStatus; reason: string | null; decidedBy: string | null; decidedAt: string }[];
}

const adaptationLabel = (value: string) => ADAPTATIONS.find((option) => option.value === value)?.label ?? value;

function DecisionPanel({ application, can, onDecided }: {
  application: AdminApplicationDetail;
  can: AdminContext["can"];
  onDecided: (message: string) => void;
}) {
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const isRevision = application.status !== "RECEBIDA";

  async function decide(operation: "defer" | "deny") {
    setError(null);
    try {
      await apiPost(`/api/admin/applications/${application.id}/${operation}`, { reason: reason.trim() || null });
      setReason("");
      onDecided(operation === "defer" ? "Inscrição deferida." : "Inscrição indeferida.");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível registrar a decisão."));
    }
  }

  if (!application.decisionsAllowed) {
    return (
      <p className="hint">
        Deferimento e indeferimento ficam disponíveis com inscrições encerradas ou resultado preliminar.
      </p>
    );
  }
  return (
    <section>
      <h2>{isRevision ? "Alterar decisão" : "Decisão"}</h2>
      <ErrorAlert error={error} />
      <div className="field">
        <label htmlFor="decision-reason">
          Justificativa {isRevision ? "(obrigatória para alterar a decisão)" : "(obrigatória para indeferir)"}
        </label>
        <textarea id="decision-reason" rows={3} maxLength={2000} value={reason}
          onChange={(event) => setReason(event.target.value)} />
        <p className="hint">A justificativa fica visível para o candidato.</p>
      </div>
      <div className="actions">
        {can("INSCRICAO_DEFERIR") && application.status !== "DEFERIDA" && (
          <button type="button" disabled={isRevision && !reason.trim()} onClick={() => decide("defer")}>
            Deferir
          </button>
        )}
        {can("INSCRICAO_INDEFERIR") && application.status !== "INDEFERIDA" && (
          <button type="button" className="secondary" disabled={!reason.trim()} onClick={() => decide("deny")}>
            Indeferir
          </button>
        )}
      </div>
    </section>
  );
}

function ApplicationAdministration({ applicationId, can }: { applicationId: string; can: AdminContext["can"] }) {
  const [application, setApplication] = useState<AdminApplicationDetail | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((current) => current + 1), []);

  useEffect(() => {
    apiGet<AdminApplicationDetail>(`/api/admin/applications/${encodeURIComponent(applicationId)}`)
      .then(setApplication)
      .catch((caught: unknown) => setError(caught instanceof ApiError ? caught : null));
  }, [applicationId, version]);

  if (!application) {
    return error ? <ErrorAlert error={error} /> : <p aria-live="polite">Carregando...</p>;
  }
  const { candidate } = application;
  const base = `/api/admin/applications/${application.id}`;

  return (
    <>
      <div className="card">
        <h1>Inscrição {application.applicationNumber}</h1>
        <p>
          <Link href={`/admin/processos/${application.processId}/inscricoes`}>
            Processo {application.processNumber} — {application.processTitle}
          </Link>
          <br />
          <strong>Cargo:</strong> {application.positionName}
          <br />
          <strong>Data/hora:</strong> {formatDateTime(application.confirmedAt)}
          <br />
          <strong>Situação:</strong> {APPLICATION_STATUS_LABELS[application.status]}
          {application.decisionReason && (
            <>
              <br />
              <strong>Justificativa:</strong> {application.decisionReason}
            </>
          )}
        </p>
        <SuccessAlert message={message} />

        <h2>Dados do candidato na inscrição</h2>
        <p>
          {candidate.fullName} · CPF {maskCpf(candidate.cpf)} · nascimento{" "}
          {candidate.birthDate.split("-").reverse().join("/")}
          <br />
          Mãe: {candidate.motherName}
          <br />
          {candidate.email} · {maskPhone(candidate.phone)} · {candidate.city}/{candidate.uf}
          {candidate.hasDisability !== null && (
            <>
              <br />
              Pessoa com deficiência: {candidate.hasDisability ? "sim" : "não"}
              {candidate.hasDisability && candidate.adaptations &&
                ` · adaptações: ${candidate.adaptations.map(adaptationLabel).join(", ")}`}
            </>
          )}
        </p>

        <h2>Documentos enviados</h2>
        {application.documents.length === 0 ? (
          <p className="hint">Nenhum documento enviado.</p>
        ) : (
          <ul>
            {application.documents.map((document) => (
              <li key={document.id}>
                {document.requirementName}{document.title && " (título)"}:{" "}
                <a href={`${base}/documents/${document.id}/file`}>{document.originalName}</a>{" "}
                <span className="hint">({Math.ceil(document.sizeBytes / 1024)} KB)</span>
              </li>
            ))}
          </ul>
        )}
        <p className="hint">O acesso aos documentos é registrado na auditoria.</p>
      </div>

      {(can("INSCRICAO_DEFERIR") || can("INSCRICAO_INDEFERIR")) && (
        <div className="card">
          <DecisionPanel application={application} can={can}
            onDecided={(text) => { setMessage(text); reload(); }} />
        </div>
      )}

      <div className="card">
        <h2>Histórico de decisões</h2>
        {application.decisions.length === 0 ? (
          <p className="hint">Nenhuma decisão registrada.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">De</th>
                <th scope="col">Para</th>
                <th scope="col">Responsável</th>
                <th scope="col">Justificativa</th>
              </tr>
            </thead>
            <tbody>
              {application.decisions.map((decision, index) => (
                <tr key={index}>
                  <td>{formatDateTime(decision.decidedAt)}</td>
                  <td>{APPLICATION_STATUS_LABELS[decision.fromStatus]}</td>
                  <td>{APPLICATION_STATUS_LABELS[decision.toStatus]}</td>
                  <td>{decision.decidedBy ?? "—"}</td>
                  <td>{decision.reason ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}

export default function AdminApplicationPage() {
  const { id } = useParams<{ id: string }>();
  return <AdminShell>{({ can }) => <ApplicationAdministration applicationId={id} can={can} />}</AdminShell>;
}
