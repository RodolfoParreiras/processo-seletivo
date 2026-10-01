"use client";

import { type FormEvent, useEffect, useState } from "react";
import { AdminShell } from "@/features/admin/AdminShell";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { formatDateTime, localInputToIso, type Page } from "@/features/process/types";
import { apiGet } from "@/lib/api";

interface AuditEntry {
  id: number;
  occurredAt: string;
  action: string;
  outcome: "SUCCESS" | "FAILURE";
  actorAccountId: string | null;
  actorType: string | null;
  actorName: string | null;
  targetType: string | null;
  targetId: string | null;
  ipAddress: string | null;
  details: string | null;
}

const EMPTY_FILTERS = { action: "", outcome: "", targetId: "", from: "", to: "" };

function actorLabel(entry: AuditEntry): string {
  if (entry.actorName) return entry.actorName;
  if (entry.actorType === "CANDIDATE") return "Candidato";
  return entry.actorAccountId ? "Conta" : "Sistema";
}

function AuditLog() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [applied, setApplied] = useState(EMPTY_FILTERS);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<AuditEntry> | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams({ page: String(pageNumber), size: "50" });
    if (applied.action) query.set("action", applied.action.trim().toUpperCase());
    if (applied.outcome) query.set("outcome", applied.outcome);
    if (applied.targetId) query.set("targetId", applied.targetId.trim());
    if (applied.from) query.set("from", localInputToIso(applied.from));
    if (applied.to) query.set("to", localInputToIso(applied.to));
    apiGet<Page<AuditEntry>>(`/api/admin/audit?${query}`)
      .then((result) => { setPage(result); setFailed(false); })
      .catch(() => setFailed(true));
  }, [applied, pageNumber]);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPageNumber(0);
    setApplied(filters);
  }

  return (
    <div className="card">
      <h1>Auditoria</h1>
      <p className="hint">Registros somente para consulta; não podem ser alterados ou excluídos.</p>
      <form onSubmit={search} noValidate>
        <div className="grid">
          <TextField name="action" label="Ação (ex.: LOGIN, APPLICATION_DENIED)" value={filters.action}
            onChange={(event) => setFilters({ ...filters, action: event.target.value })} />
          <div className="field">
            <label htmlFor="outcome">Resultado</label>
            <select id="outcome" value={filters.outcome} onChange={(event) => setFilters({ ...filters, outcome: event.target.value })}>
              <option value="">Todos</option>
              <option value="SUCCESS">Sucesso</option>
              <option value="FAILURE">Falha</option>
            </select>
          </div>
          <TextField name="from" label="De" type="datetime-local" value={filters.from}
            onChange={(event) => setFilters({ ...filters, from: event.target.value })} />
          <TextField name="to" label="Até" type="datetime-local" value={filters.to}
            onChange={(event) => setFilters({ ...filters, to: event.target.value })} />
        </div>
        <TextField name="targetId" label="Identificador do alvo" value={filters.targetId}
          onChange={(event) => setFilters({ ...filters, targetId: event.target.value })} />
        <div className="actions">
          <button type="submit">Pesquisar</button>
          <button type="button" className="secondary" onClick={() => { setFilters(EMPTY_FILTERS); setApplied(EMPTY_FILTERS); }}>
            Limpar
          </button>
        </div>
      </form>

      {failed && <p className="field-error">Não foi possível carregar a auditoria.</p>}
      {page && (
        <>
          <p className="hint">{page.totalElements} registro(s)</p>
          <table>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Ação</th>
                <th scope="col">Resultado</th>
                <th scope="col">Autor</th>
                <th scope="col">Alvo</th>
                <th scope="col">IP</th>
                <th scope="col">Detalhes</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((entry) => (
                <tr key={entry.id}>
                  <td>{formatDateTime(entry.occurredAt)}</td>
                  <td>{entry.action}</td>
                  <td>{entry.outcome === "SUCCESS" ? "Sucesso" : "Falha"}</td>
                  <td>{actorLabel(entry)}</td>
                  <td className="hint">{entry.targetType ? `${entry.targetType} ${entry.targetId ?? ""}` : "—"}</td>
                  <td className="hint">{entry.ipAddress ?? "—"}</td>
                  <td className="hint">{entry.details ?? "—"}</td>
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
    </div>
  );
}

export default function AuditPage() {
  return <AdminShell>{() => <AuditLog />}</AdminShell>;
}
