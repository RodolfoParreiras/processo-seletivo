"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { StartApplicationButton } from "@/features/application/StartApplicationButton";
import { PublicProcessDocuments } from "@/features/process/ProcessDocuments";
import { ProcessInformation } from "@/features/process/ProcessInformation";
import type { ProcessDetail } from "@/features/process/types";
import { ApiError, apiGet } from "@/lib/api";

export default function PublicProcessPage() {
  const { id } = useParams<{ id: string }>();
  const [process, setProcess] = useState<ProcessDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<ProcessDetail>(`/api/processes/${encodeURIComponent(id)}`)
      .then(setProcess)
      .catch((caught: unknown) => setError(caught instanceof ApiError ? caught.message : "Erro ao carregar."));
  }, [id]);

  return (
    <main>
      <div className="card">
        {error && <p className="field-error">{error}</p>}
        {!process && !error && <p aria-live="polite">Carregando...</p>}
        {process && (
          <>
            <h1>
              Processo Seletivo {process.number}/{process.year}
            </h1>
            <p>{process.title}</p>
            <ProcessInformation
              process={process}
              noticeBaseUrl={`/api/processes/${encodeURIComponent(process.id)}/notices`}
              positionAction={
                process.status === "INSCRICOES_ABERTAS"
                  ? (positionId) => <StartApplicationButton processId={process.id} positionId={positionId} />
                  : undefined
              }
            />
            <PublicProcessDocuments processId={process.id} />
          </>
        )}
        <div className="actions">
          <Link href="/">Voltar</Link>
        </div>
      </div>
    </main>
  );
}
