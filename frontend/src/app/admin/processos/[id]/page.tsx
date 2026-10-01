"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { type AdminContext, AdminShell } from "@/features/admin/AdminShell";
import { ProcessDetailsForm } from "@/features/admin/process/ProcessDetailsForm";
import {
  ExtendRegistration,
  NoticeUpload,
  PositionsEditor,
  ReasonAction,
  RequirementsEditor,
  StatusHistory,
} from "@/features/admin/process/ProcessEditors";
import { AdminProcessDocuments } from "@/features/process/ProcessDocuments";
import { ProcessInformation } from "@/features/process/ProcessInformation";
import type { ProcessDetail, ProcessStatus } from "@/features/process/types";
import { ApiError, apiGet, apiPost, apiPut } from "@/lib/api";

const SUSPENDABLE: ProcessStatus[] = [
  "PUBLICADO",
  "INSCRICOES_ABERTAS",
  "INSCRICOES_ENCERRADAS",
  "RESULTADO_PRELIMINAR",
  "RESULTADO_DEFINITIVO",
];

function ProcessAdministration({ processId, can }: { processId: string; can: AdminContext["can"] }) {
  const [process, setProcess] = useState<ProcessDetail | null>(null);
  const [version, setVersion] = useState(0);
  const [error, setError] = useState<ApiError | null>(null);

  const reload = useCallback(() => setVersion((current) => current + 1), []);

  useEffect(() => {
    apiGet<ProcessDetail>(`/api/admin/processes/${encodeURIComponent(processId)}`)
      .then(setProcess)
      .catch((caught: unknown) => setError(caught instanceof ApiError ? caught : null));
  }, [processId, version]);

  if (!process) {
    return error ? <ErrorAlert error={error} /> : <p aria-live="polite">Carregando...</p>;
  }

  const base = `/api/admin/processes/${process.id}`;
  const isDraft = process.status === "RASCUNHO";
  const isTerminal = process.status === "ARQUIVADO" || process.status === "CANCELADO";

  async function publish() {
    setError(null);
    try {
      await apiPost(`${base}/publish`);
      reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível publicar."));
    }
  }

  return (
    <>
      <div className="card">
        <h1>
          Processo {process.number}/{process.year}
        </h1>
        <p>{process.title}</p>
        <ErrorAlert error={error} />
        <ProcessInformation process={process} noticeBaseUrl={`${base}/notices`} />
        {can("INSCRICAO_VISUALIZAR") && !isDraft && (
          <p>
            <Link className="button" href={`/admin/processos/${process.id}/inscricoes`}>Ver inscrições</Link>
          </p>
        )}
      </div>

      {isDraft && can("PROCESSO_EDITAR") && (
        <div className="card">
          <h2>Dados do processo</h2>
          <ProcessDetailsForm
            key={version}
            initial={process}
            submitLabel="Salvar dados"
            onSubmit={async (payload) => {
              await apiPut(base, payload);
              reload();
            }}
          />
          <hr />
          <PositionsEditor process={process} onChanged={reload} />
          <hr />
          <RequirementsEditor process={process} onChanged={reload} />
        </div>
      )}

      {!isTerminal && can("PROCESSO_EDITAR") && (
        <div className="card">
          <NoticeUpload process={process} onChanged={reload} />
          {(process.status === "PUBLICADO" || process.status === "INSCRICOES_ABERTAS") && (
            <>
              <hr />
              <ExtendRegistration process={process} onChanged={reload} />
            </>
          )}
        </div>
      )}

      {!isTerminal && (can("PROCESSO_PUBLICAR") || can("PROCESSO_ENCERRAR")) && (
        <div className="card">
          <h2>Situação</h2>
          <div className="actions">
            {isDraft && can("PROCESSO_PUBLICAR") && (
              <button type="button" onClick={publish}>
                Publicar processo
              </button>
            )}
            {can("PROCESSO_ENCERRAR") && SUSPENDABLE.includes(process.status) && (
              <ReasonAction label="Suspender" endpoint={`${base}/suspend`} onDone={reload}
                description="O processo ficará suspenso até ser retomado. Inscrições e transições automáticas param." />
            )}
            {can("PROCESSO_ENCERRAR") && process.status === "SUSPENSO" && (
              <ReasonAction label="Retomar" endpoint={`${base}/resume`} onDone={reload}
                description="O processo volta à situação anterior à suspensão." />
            )}
            {can("PROCESSO_ENCERRAR") && process.status === "RESULTADO_DEFINITIVO" && (
              <ReasonAction label="Arquivar" endpoint={`${base}/archive`} onDone={reload}
                description="O processo será arquivado. Esta ação não pode ser desfeita." />
            )}
            {can("PROCESSO_ENCERRAR") && (
              <ReasonAction label="Cancelar processo" endpoint={`${base}/cancel`} onDone={reload}
                description="O cancelamento é definitivo e não pode ser desfeito." />
            )}
          </div>
        </div>
      )}

      <div className="card">
        <AdminProcessDocuments processId={process.id} canPublish={can("RESULTADO_PUBLICAR")} />
      </div>

      <div className="card">
        <StatusHistory processId={process.id} version={version} />
      </div>
    </>
  );
}

export default function AdminProcessPage() {
  const { id } = useParams<{ id: string }>();
  return <AdminShell>{({ can }) => <ProcessAdministration processId={id} can={can} />}</AdminShell>;
}
