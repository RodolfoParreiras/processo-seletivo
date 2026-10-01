"use client";

import { useState } from "react";
import { ProcessList } from "./ProcessList";
import { type ProcessStatus, STATUS_LABELS } from "./types";

const FILTERS: (ProcessStatus | null)[] = [
  null,
  "INSCRICOES_ABERTAS",
  "PUBLICADO",
  "INSCRICOES_ENCERRADAS",
  "SUSPENSO",
  "CANCELADO",
];

/** Lista pública de processos com filtro por situação. */
export function ProcessBrowser() {
  const [status, setStatus] = useState<ProcessStatus | null>(null);

  return (
    <>
      <div className="filter-bar" role="group" aria-label="Filtrar por situação">
        {FILTERS.map((filter) => (
          <button
            key={filter ?? "todos"}
            type="button"
            className="filter-chip"
            aria-pressed={status === filter}
            onClick={() => setStatus(filter)}
          >
            {filter ? STATUS_LABELS[filter] : "Todos"}
          </button>
        ))}
      </div>
      <ProcessList
        key={status ?? "todos"}
        status={status ?? undefined}
        emptyMessage={status ? "Nenhum processo nesta situação." : "Nenhum processo seletivo publicado no momento."}
      />
    </>
  );
}
