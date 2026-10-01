"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
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
    <div className="table-scroll">
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Processo</th>
            <th scope="col">Cargo</th>
            <th scope="col">Situação</th>
            <th scope="col"><span className="sr-only">Ações</span></th>
          </tr>
        </thead>
        <tbody>
          {applications.map((application) => (
            <tr key={application.id}>
              <td>
                <strong className="cell-title">{application.processNumber}</strong>
                <div className="hint">{application.processTitle}</div>
              </td>
              <td>{application.positionName}</td>
              <td>{APPLICATION_STATUS_LABELS[application.status]}</td>
              <td className="cell-action">
                <Link
                  className="button secondary"
                  href={`/candidato/inscricoes/${application.id}`}
                  aria-label={`Detalhar inscrição em ${application.positionName}, processo ${application.processNumber}`}
                >
                  Detalhar
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
