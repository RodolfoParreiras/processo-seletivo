"use client";

import { SessionGate } from "@/features/auth/SessionGate";
import { ProcessAccordion } from "@/features/process/ProcessAccordion";

function firstName(fullName: string): string {
  return fullName.trim().split(/\s+/)[0];
}

export default function OpenProcessesPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {(session) => (
        <main>
          <h1 className="greeting">Olá, {firstName(session.displayName)}!</h1>
          <p className="hint">Veja os processos com inscrições abertas e inscreva-se.</p>
          <ProcessAccordion status="INSCRICOES_ABERTAS" emptyMessage="Nenhum processo com inscrições abertas no momento." />
        </main>
      )}
    </SessionGate>
  );
}
