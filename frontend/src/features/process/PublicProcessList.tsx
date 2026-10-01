"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiGet } from "@/lib/api";
import { formatDateTime, type Page, type ProcessStatus, type ProcessSummary, STAGE_LABELS, STATUS_LABELS } from "./types";

interface PublicProcessListProps {
  status?: ProcessStatus;
  emptyMessage: string;
}

export function PublicProcessList({ status, emptyMessage }: PublicProcessListProps) {
  const [page, setPage] = useState<Page<ProcessSummary> | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams({ size: "50" });
    if (status) query.set("status", status);
    apiGet<Page<ProcessSummary>>(`/api/processes?${query}`)
      .then(setPage)
      .catch(() => setFailed(true));
  }, [status]);

  if (failed) return <p className="field-error">Não foi possível carregar os processos.</p>;
  if (!page) return <p aria-live="polite">Carregando...</p>;
  if (page.content.length === 0) return <p>{emptyMessage}</p>;

  return (
    <ul className="process-list">
      {page.content.map((process) => (
        <li key={process.id}>
          <Link href={`/processos/${process.id}`}>
            <strong>
              {process.number}/{process.year}
            </strong>{" "}
            — {process.title}
          </Link>
          <br />
          <span className="hint">
            {process.stage ? `${STAGE_LABELS[process.stage]} · ` : ""}
            {STATUS_LABELS[process.status]} · {process.department} · Inscrições de{" "}
            {formatDateTime(process.registrationStart)} a {formatDateTime(process.registrationEnd)}
          </span>
        </li>
      ))}
    </ul>
  );
}
