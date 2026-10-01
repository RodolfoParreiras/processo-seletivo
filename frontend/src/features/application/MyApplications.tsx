"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { formatDateTime } from "@/features/process/types";
import { apiGet } from "@/lib/api";
import { APPLICATION_STATUS_LABELS, type ApplicationSummary } from "./types";

/** "Minhas Candidaturas" (ESPECIFICACAO §26). */
export function MyApplications() {
  const [applications, setApplications] = useState<ApplicationSummary[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    apiGet<ApplicationSummary[]>("/api/candidate/applications")
      .then(setApplications)
      .catch(() => setFailed(true));
  }, []);

  if (failed) return <p className="field-error">Não foi possível carregar suas candidaturas.</p>;
  if (!applications) return <p aria-live="polite">Carregando...</p>;
  if (applications.length === 0) return <p>Você ainda não possui inscrições.</p>;

  return (
    <table>
      <thead>
        <tr>
          <th scope="col">Processo</th>
          <th scope="col">Cargo</th>
          <th scope="col">Inscrição</th>
          <th scope="col">Situação</th>
        </tr>
      </thead>
      <tbody>
        {applications.map((application) => (
          <tr key={application.id}>
            <td>
              {application.processNumber} — {application.processTitle}
            </td>
            <td>{application.positionName}</td>
            <td>
              <Link href={`/candidato/inscricoes/${application.id}`}>
                {application.applicationNumber ?? "Continuar inscrição"}
              </Link>
              {application.confirmedAt && <div className="hint">{formatDateTime(application.confirmedAt)}</div>}
            </td>
            <td>{APPLICATION_STATUS_LABELS[application.status]}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
