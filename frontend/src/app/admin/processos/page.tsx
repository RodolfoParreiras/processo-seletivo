"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AdminShell } from "@/features/admin/AdminShell";
import {
  formatDateTime,
  type Page,
  type ProcessStatus,
  type ProcessSummary,
  STATUS_LABELS,
} from "@/features/process/types";
import { apiGet } from "@/lib/api";

function ProcessTable() {
  const [status, setStatus] = useState<ProcessStatus | "">("");
  const [page, setPage] = useState<Page<ProcessSummary> | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams({ size: "50" });
    if (status) query.set("status", status);
    apiGet<Page<ProcessSummary>>(`/api/admin/processes?${query}`)
      .then(setPage)
      .catch(() => setFailed(true));
  }, [status]);

  return (
    <>
      <div className="field">
        <label htmlFor="status-filter">Situação</label>
        <select id="status-filter" value={status} onChange={(event) => setStatus(event.target.value as ProcessStatus | "")}>
          <option value="">Todas</option>
          {Object.entries(STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </div>
      {failed && <p className="field-error">Não foi possível carregar os processos.</p>}
      {page && page.content.length === 0 && <p>Nenhum processo encontrado.</p>}
      {page && page.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th scope="col">Número</th>
              <th scope="col">Título</th>
              <th scope="col">Situação</th>
              <th scope="col">Inscrições</th>
            </tr>
          </thead>
          <tbody>
            {page.content.map((process) => (
              <tr key={process.id}>
                <td>
                  <Link href={`/admin/processos/${process.id}`}>
                    {process.number}/{process.year}
                  </Link>
                </td>
                <td>{process.title}</td>
                <td>{STATUS_LABELS[process.status]}</td>
                <td>
                  {formatDateTime(process.registrationStart)} a {formatDateTime(process.registrationEnd)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}

export default function AdminProcessesPage() {
  return (
    <AdminShell>
      {({ can }) => (
        <div className="card">
          <h1>Processos seletivos</h1>
          {can("PROCESSO_CRIAR") && (
            <p>
              <Link className="button" href="/admin/processos/novo">
                Novo processo
              </Link>
            </p>
          )}
          <ProcessTable />
        </div>
      )}
    </AdminShell>
  );
}
