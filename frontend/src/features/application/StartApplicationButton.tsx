"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { ApiError, apiPost } from "@/lib/api";

interface StartApplicationButtonProps {
  processId: string;
  positionId: string;
}

/** Inicia o rascunho da inscrição; quem não está autenticado como candidato é levado ao login. */
export function StartApplicationButton({ processId, positionId }: StartApplicationButtonProps) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function start() {
    setError(null);
    setSubmitting(true);
    try {
      const created = await apiPost<{ id: string }>("/api/candidate/applications", { processId, positionId });
      router.push(`/candidato/inscricoes/${created.id}`);
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 401) {
        router.push("/entrar");
        return;
      }
      if (caught instanceof ApiError && caught.status === 403) {
        setError("Entre com uma conta de candidato para se inscrever.");
        return;
      }
      setError(caught instanceof ApiError ? caught.message : "Não foi possível iniciar a inscrição.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button type="button" onClick={start} disabled={submitting}>
        {submitting ? "Aguarde..." : "Inscrever-se"}
      </button>
      {error && <span className="field-error"> {error}</span>}
    </>
  );
}
