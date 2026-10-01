"use client";

import { MyApplications } from "@/features/application/MyApplications";
import { SessionGate } from "@/features/auth/SessionGate";

export default function MyApplicationsPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => (
        <main>
          <h1 className="page-title">Minhas Candidaturas</h1>
          <div className="card">
            <MyApplications />
          </div>
        </main>
      )}
    </SessionGate>
  );
}
