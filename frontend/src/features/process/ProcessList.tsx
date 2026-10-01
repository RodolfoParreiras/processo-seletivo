"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiGet } from "@/lib/api";
import {
  type Page,
  type ProcessDetail,
  type ProcessStatus,
  type ProcessSummary,
  STAGE_LABELS,
  STATUS_LABELS,
} from "./types";

/** Endereço que inicia a inscrição: exige login de candidato e volta para cá depois dele. */
export function enrollHref(processId: string, positionId: string): string {
  return `/candidato/inscrever?processo=${encodeURIComponent(processId)}&cargo=${encodeURIComponent(positionId)}`;
}

/** Cargos com número de vagas em destaque e, com inscrições abertas, o botão de inscrição. */
export function PositionList({ process }: { process: ProcessDetail }) {
  const open = process.status === "INSCRICOES_ABERTAS";
  return (
    <div className="position-list">
      {process.positions.map((position) => (
        <div key={position.id} className="position-row">
          <div className="vacancy-count">
            <strong>{position.vacancies}</strong>
            <span>{position.vacancies === 1 ? "vaga" : "vagas"}</span>
          </div>
          <span className="position-name">{position.name}</span>
          {open ? (
            <Link className="button action" href={enrollHref(process.id, position.id)}>Inscrever-se</Link>
          ) : (
            <span className="hint">
              {process.status === "PUBLICADO" ? "Inscrições ainda não abertas" : "Inscrições encerradas"}
            </span>
          )}
        </div>
      ))}
    </div>
  );
}

interface ProcessListProps {
  status?: ProcessStatus;
  emptyMessage: string;
}

/** Lista de processos em cartões, cada um com o botão para a página do processo. */
export function ProcessList({ status, emptyMessage }: ProcessListProps) {
  const [page, setPage] = useState<Page<ProcessSummary> | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const query = new URLSearchParams({ size: "50" });
    if (status) query.set("status", status);
    apiGet<Page<ProcessSummary>>(`/api/processes?${query}`).then(setPage).catch(() => setFailed(true));
  }, [status]);

  if (failed) return <p className="field-error">Não foi possível carregar os processos.</p>;
  if (!page) return <p aria-live="polite">Carregando...</p>;
  if (page.content.length === 0) return <p>{emptyMessage}</p>;

  return (
    <ul className="process-list">
      {page.content.map((process) => (
        <li key={process.id} className="process-card">
          <div className="grow">
            <div className="tags">
              <span className={process.status === "INSCRICOES_ABERTAS" ? "tag tag-open" : "tag"}>
                {STATUS_LABELS[process.status]}
              </span>
              {process.stage && <span className="tag">{STAGE_LABELS[process.stage]}</span>}
            </div>
            <div className="process-title">{process.number}/{process.year} · {process.title}</div>
            <div className="process-meta">{process.department}</div>
          </div>
          <Link
            className="button secondary"
            href={`/processos/${process.id}`}
            aria-label={`Detalhar processo ${process.number}/${process.year}, ${process.title}`}
          >
            Detalhar
          </Link>
        </li>
      ))}
    </ul>
  );
}
