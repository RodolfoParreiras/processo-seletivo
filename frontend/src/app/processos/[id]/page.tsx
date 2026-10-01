"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Hero } from "@/components/Hero";
import { PositionList } from "@/features/process/ProcessList";
import { formatDateTime, type ProcessDetail, STAGE_LABELS, STATUS_LABELS } from "@/features/process/types";
import { ApiError, apiGet } from "@/lib/api";

interface PublicDocument {
  id: string;
  name: string;
  publishedAt: string;
}

const NOTICE_STATUS: Record<ProcessDetail["notices"][number]["status"], string> = {
  CURRENT: "vigente",
  SUPERSEDED: "substituída",
  DRAFT: "rascunho",
};

function DownloadRow({ href, title, details }: { href: string; title: string; details: string }) {
  return (
    <li className="download-row">
      <i className="ti ti-file-type-pdf" aria-hidden="true" />
      <span className="grow">
        <strong>{title}</strong> <span className="hint">· {details}</span>
      </span>
      <a className="button secondary" href={href} aria-label={`Baixar ${title} (PDF)`}>
        <i className="ti ti-download" aria-hidden="true" /> Baixar PDF
      </a>
    </li>
  );
}

export default function PublicProcessPage() {
  const { id } = useParams<{ id: string }>();
  const [process, setProcess] = useState<ProcessDetail | null>(null);
  const [documents, setDocuments] = useState<PublicDocument[]>([]);
  const [error, setError] = useState<string | null>(null);
  const base = `/api/processes/${encodeURIComponent(id)}`;

  useEffect(() => {
    apiGet<ProcessDetail>(base)
      .then(setProcess)
      .catch((caught: unknown) => setError(caught instanceof ApiError ? caught.message : "Erro ao carregar."));
    apiGet<PublicDocument[]>(`${base}/documents`).then(setDocuments).catch(() => setDocuments([]));
  }, [base]);

  if (!process) {
    return (
      <main>
        {error ? <p className="field-error">{error}</p> : <p aria-live="polite">Carregando...</p>}
      </main>
    );
  }

  const notices = process.notices.filter((notice) => notice.publishedAt);

  return (
    <>
      <Hero
        title={`${process.number}/${process.year} · ${process.title}`}
        subtitle={process.department}
        back={{ href: "/", label: "Processos seletivos" }}
      />
      <main className="stack">
        <section className="card" aria-label="Período de inscrição">
          <dl className="info-grid">
            <div className="info-item">
              <dt>Início das inscrições</dt>
              <dd>{formatDateTime(process.registrationStart)}</dd>
            </div>
            <div className="info-item">
              <dt>Fim das inscrições</dt>
              <dd>{formatDateTime(process.registrationEnd)}</dd>
            </div>
            <div className="info-item">
              <dt>Situação</dt>
              <dd>{STATUS_LABELS[process.status]}</dd>
            </div>
            {process.stage && (
              <div className="info-item">
                <dt>Etapa atual</dt>
                <dd>{STAGE_LABELS[process.stage]}</dd>
              </div>
            )}
          </dl>
        </section>

        <section className="card">
          <h2 className="step-title"><i className="ti ti-briefcase" aria-hidden="true" />Cargos</h2>
          {process.positions.length === 0 ? (
            <p className="hint">Nenhum cargo cadastrado.</p>
          ) : (
            <PositionList process={process} />
          )}
        </section>

        <section className="card">
          <h2 className="step-title"><i className="ti ti-files" aria-hidden="true" />Documentos</h2>
          {notices.length === 0 && documents.length === 0 ? (
            <p className="hint">Nenhum documento publicado.</p>
          ) : (
            <ul className="download-list">
              {notices.map((notice) => (
                <DownloadRow
                  key={`edital-${notice.version}`}
                  href={`${base}/notices/${notice.version}/file`}
                  title="Edital"
                  details={`versão ${notice.version}, ${NOTICE_STATUS[notice.status]} · ${formatDateTime(notice.publishedAt)}`}
                />
              ))}
              {documents.map((document) => (
                <DownloadRow
                  key={document.id}
                  href={`${base}/documents/${document.id}/file`}
                  title={document.name}
                  details={formatDateTime(document.publishedAt)}
                />
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h2 className="step-title"><i className="ti ti-checklist" aria-hidden="true" />Documentos exigidos na inscrição</h2>
          {process.documentRequirements.length === 0 ? (
            <p className="hint">Nenhum documento exigido.</p>
          ) : (
            <ul className="requirement-summary">
              {process.documentRequirements.map((requirement) => (
                <li key={requirement.id}>
                  <strong>{requirement.name}</strong>
                  {requirement.mandatory ? (
                    <span className="required-mark" aria-label="obrigatório"> *</span>
                  ) : (
                    <span className="hint"> (opcional)</span>
                  )}
                  {requirement.title && <span className="hint"> · título</span>}
                  {requirement.description && <div className="hint">{requirement.description}</div>}
                </li>
              ))}
            </ul>
          )}
        </section>
      </main>
    </>
  );
}
