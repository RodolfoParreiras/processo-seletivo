"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { SessionGate } from "@/features/auth/SessionGate";
import { ApiError, apiPost } from "@/lib/api";

/**
 * Destino do botão "Inscrever-se": exige sessão de candidato (o login volta para cá) e então inicia a
 * inscrição no cargo escolhido. O backend valida processo, cargo e período.
 */
function StartApplication() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const params = new URLSearchParams(window.location.search);
    const processId = params.get("processo");
    const positionId = params.get("cargo");
    if (!processId || !positionId) {
      setError("Link de inscrição inválido.");
      return;
    }
    apiPost<{ id: string }>("/api/candidate/applications", { processId, positionId })
      .then((created) => router.replace(`/candidato/inscricoes/${created.id}`))
      .catch((caught: unknown) =>
        setError(caught instanceof ApiError ? caught.message : "Não foi possível iniciar a inscrição."));
  }, [router]);

  return (
    <main>
      <div className="card">
        <h1>Inscrição</h1>
        {error ? (
          <>
            <div className="alert alert-error" role="alert">{error}</div>
            <div className="actions">
              <Link className="button" href="/candidato/candidaturas">Minhas candidaturas</Link>
              <Link className="button secondary" href="/">Voltar aos processos</Link>
            </div>
          </>
        ) : (
          <p aria-live="polite">Preparando sua inscrição...</p>
        )}
      </div>
    </main>
  );
}

export default function StartApplicationPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => <StartApplication />}
    </SessionGate>
  );
}
