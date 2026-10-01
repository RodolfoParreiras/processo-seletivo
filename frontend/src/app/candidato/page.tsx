"use client";

import { Hero } from "@/components/Hero";
import { SessionGate } from "@/features/auth/SessionGate";
import { ProcessAccordion } from "@/features/process/ProcessAccordion";

function firstName(fullName: string): string {
  return fullName.trim().split(/\s+/)[0];
}

export default function OpenProcessesPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {(session) => (
        <>
          <Hero
            title={`Olá, ${firstName(session.displayName)}!`}
            subtitle="Veja os processos com inscrições abertas e inscreva-se."
          />
          <main>
            <h2 className="section-title">Processos abertos</h2>
            <ProcessAccordion status="INSCRICOES_ABERTAS" emptyMessage="Nenhum processo com inscrições abertas no momento." />
          </main>
        </>
      )}
    </SessionGate>
  );
}
