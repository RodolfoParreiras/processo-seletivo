"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminShell } from "@/features/admin/AdminShell";
import { APPLICATION_STATUS_LABELS, type ApplicationStatus } from "@/features/application/types";
import { formatDateTime, type Page } from "@/features/process/types";
import { apiGet } from "@/lib/api";

interface AdminApplicationRow {
  id: string;
  applicationNumber: string;
  candidateName: string;
  positionName: string;
  status: ApplicationStatus;
  confirmedAt: string;
}

const FILTERS: (ApplicationStatus | "")[] = ["", "RECEBIDA", "DEFERIDA", "INDEFERIDA"];

function ApplicationsTable({ processId }: { processId: string }) {
  const [status, setStatus] = useState<ApplicationStatus | "">("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AdminApplicationRow> | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams({ page: String(pageNumber), size: "50" });
    if (status) query.set("status", status);
    apiGet<Page<AdminApplicationRow>>(`/api/admin/processes/${encodeURIComponent(processId)}/applications?${query}`)
      .then(setPage)
      .catch(() => setFailed(true));
  }, [processId, status, pageNumber]);

  return (
    <>
      <div className="field">
        <label htmlFor="application-status">Situação</label>
        <select id="application-status" value={status}
          onChange={(event) => { setStatus(event.target.value as ApplicationStatus | ""); setPageNumber(0); }}>
          {FILTERS.map((value) => (
            <option key={value} value={value}>{value ? APPLICATION_STATUS_LABELS[value] : "Todas"}</option>
          ))}
        </select>
      </div>
      {failed && <p className="field-error">Não foi possível carregar as inscrições.</p>}
      {page && (
        <>
          <p className="hint">{page.totalElements} inscrição(ões)</p>
          <table>
            <thead>
              <tr>
                <th scope="col">Inscrição</th>
                <th scope="col">Candidato(a)</th>
                <th scope="col">Cargo</th>
                <th scope="col">Situação</th>
                <th scope="col">Data</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((row) => (
                <tr key={row.id}>
                  <td><Link href={`/admin/inscricoes/${row.id}`}>{row.applicationNumber}</Link></td>
                  <td>{row.candidateName}</td>
                  <td>{row.positionName}</td>
                  <td>{APPLICATION_STATUS_LABELS[row.status]}</td>
                  <td>{formatDateTime(row.confirmedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="actions">
            <button type="button" className="secondary" disabled={pageNumber === 0}
              onClick={() => setPageNumber(pageNumber - 1)}>Anterior</button>
            <span className="hint">Página {page.totalPages === 0 ? 0 : pageNumber + 1} de {page.totalPages}</span>
            <button type="button" className="secondary" disabled={pageNumber + 1 >= page.totalPages}
              onClick={() => setPageNumber(pageNumber + 1)}>Próxima</button>
          </div>
        </>
      )}
    </>
  );
}

export default function AdminProcessApplicationsPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <AdminShell>
      {() => (
        <div className="card">
          <h1>Inscrições do processo</h1>
          <p><Link href={`/admin/processos/${id}`}>Voltar para o processo</Link></p>
          <ApplicationsTable processId={id} />
        </div>
      )}
    </AdminShell>
  );
}
