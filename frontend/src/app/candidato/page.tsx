"use client";

import { SessionGate } from "@/features/auth/SessionGate";

// Área provisória: "Meus Dados", "Processos Abertos" e "Minhas Candidaturas" entram nas fases 3 a 5.
export default function CandidateAreaPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {(session, logout) => (
        <main>
          <div className="card">
            <h1>Área do Candidato</h1>
            <p>Olá, {session.displayName}.</p>
            <div className="actions">
              <button type="button" className="secondary" onClick={logout}>
                Sair
              </button>
            </div>
          </div>
        </main>
      )}
    </SessionGate>
  );
}
