"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiGet } from "@/lib/api";
import {
  formatDateTime,
  type Page,
  type ProcessDetail,
  type ProcessStatus,
  type ProcessSummary,
  STAGE_LABELS,
  STATUS_LABELS,
} from "./types";

type PanelKind = "cargos" | "documentos";

interface OpenPanel {
  processId: string;
  kind: PanelKind;
}

interface PublicDocument {
  id: string;
  name: string;
  publishedAt: string;
}

/** Endereço que inicia a inscrição: exige login de candidato e volta para cá depois dele. */
export function enrollHref(processId: string, positionId: string): string {
  return `/candidato/inscrever?processo=${encodeURIComponent(processId)}&cargo=${encodeURIComponent(positionId)}`;
}

function PositionsPanel({ processId }: { processId: string }) {
  const [process, setProcess] = useState<ProcessDetail | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    apiGet<ProcessDetail>(`/api/processes/${encodeURIComponent(processId)}`).then(setProcess).catch(() => setFailed(true));
  }, [processId]);

  if (failed) return <p className="field-error">Não foi possível carregar os cargos.</p>;
  if (!process) return <p className="hint" aria-live="polite">Carregando...</p>;
  if (process.positions.length === 0) return <p className="hint">Nenhum cargo cadastrado.</p>;

  const open = process.status === "INSCRICOES_ABERTAS";
  return (
    <>
      {process.positions.map((position) => (
        <div key={position.id} className="panel-row">
          <span className="grow">{position.name}</span>
          <span className="hint">{position.vacancies} vaga(s)</span>
          {open ? (
            <Link className="button action" href={enrollHref(process.id, position.id)}>Inscrever-se</Link>
          ) : (
            <span className="hint">
              {process.status === "PUBLICADO" ? "Inscrições ainda não abertas" : "Inscrições encerradas"}
            </span>
          )}
        </div>
      ))}
    </>
  );
}

function DocumentsPanel({ processId }: { processId: string }) {
  const [notices, setNotices] = useState<ProcessDetail["notices"] | null>(null);
  const [documents, setDocuments] = useState<PublicDocument[] | null>(null);
  const [failed, setFailed] = useState(false);
  const base = `/api/processes/${encodeURIComponent(processId)}`;

  useEffect(() => {
    Promise.all([apiGet<ProcessDetail>(base), apiGet<PublicDocument[]>(`${base}/documents`)])
      .then(([process, published]) => {
        setNotices(process.notices);
        setDocuments(published);
      })
      .catch(() => setFailed(true));
  }, [base]);

  if (failed) return <p className="field-error">Não foi possível carregar os documentos.</p>;
  if (!notices || !documents) return <p className="hint" aria-live="polite">Carregando...</p>;
  if (notices.length === 0 && documents.length === 0) return <p className="hint">Nenhum documento publicado.</p>;

  return (
    <>
      {notices.map((notice) => (
        <div key={`notice-${notice.version}`} className="panel-row">
          <span className="grow">
            Edital{notices.length > 1 ? ` (versão ${notice.version}${notice.status === "CURRENT" ? ", vigente" : ""})` : ""}
          </span>
          <span className="hint">{formatDateTime(notice.publishedAt)}</span>
          <a className="button secondary" href={`${base}/notices/${notice.version}/file`}>Baixar PDF</a>
        </div>
      ))}
      {documents.map((document) => (
        <div key={document.id} className="panel-row">
          <span className="grow">{document.name}</span>
          <span className="hint">{formatDateTime(document.publishedAt)}</span>
          <a className="button secondary" href={`${base}/documents/${document.id}/file`}>Baixar PDF</a>
        </div>
      ))}
    </>
  );
}

interface ProcessAccordionProps {
  status?: ProcessStatus;
  emptyMessage: string;
}

/**
 * Lista de processos com os botões "Cargos" e "Documentos". Só um painel fica aberto por vez na página:
 * abrir outro fecha o anterior, inclusive de outro processo.
 */
export function ProcessAccordion({ status, emptyMessage }: ProcessAccordionProps) {
  const [page, setPage] = useState<Page<ProcessSummary> | null>(null);
  const [failed, setFailed] = useState(false);
  const [openPanel, setOpenPanel] = useState<OpenPanel | null>(null);

  useEffect(() => {
    const query = new URLSearchParams({ size: "50" });
    if (status) query.set("status", status);
    apiGet<Page<ProcessSummary>>(`/api/processes?${query}`).then(setPage).catch(() => setFailed(true));
  }, [status]);

  if (failed) return <p className="field-error">Não foi possível carregar os processos.</p>;
  if (!page) return <p aria-live="polite">Carregando...</p>;
  if (page.content.length === 0) return <p>{emptyMessage}</p>;

  const toggle = (processId: string, kind: PanelKind) =>
    setOpenPanel((current) =>
      current?.processId === processId && current.kind === kind ? null : { processId, kind });
  const isOpen = (processId: string, kind: PanelKind) =>
    openPanel?.processId === processId && openPanel.kind === kind;

  return (
    <ul className="process-list">
      {page.content.map((process) => {
        const panelId = (kind: PanelKind) => `painel-${kind}-${process.id}`;
        return (
          <li key={process.id} className="process-card">
            <div className="tags">
              <span className={process.status === "INSCRICOES_ABERTAS" ? "tag tag-open" : "tag"}>
                {STATUS_LABELS[process.status]}
              </span>
              {process.stage && <span className="tag">{STAGE_LABELS[process.stage]}</span>}
            </div>
            <Link className="process-title" href={`/processos/${process.id}`}>
              {process.number}/{process.year} · {process.title}
            </Link>
            <div className="process-meta">
              {process.department} · Inscrições de {formatDateTime(process.registrationStart)} a{" "}
              {formatDateTime(process.registrationEnd)}
            </div>
            <div className="actions compact">
              {(["cargos", "documentos"] as const).map((kind) => (
                <button
                  key={kind}
                  type="button"
                  className="secondary"
                  aria-expanded={isOpen(process.id, kind)}
                  aria-controls={panelId(kind)}
                  onClick={() => toggle(process.id, kind)}
                >
                  {kind === "cargos" ? "Cargos" : "Documentos"} {isOpen(process.id, kind) ? "▴" : "▾"}
                </button>
              ))}
            </div>
            {isOpen(process.id, "cargos") && (
              <div id={panelId("cargos")} className="process-panel">
                <PositionsPanel processId={process.id} />
              </div>
            )}
            {isOpen(process.id, "documentos") && (
              <div id={panelId("documentos")} className="process-panel">
                <DocumentsPanel processId={process.id} />
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );
}
