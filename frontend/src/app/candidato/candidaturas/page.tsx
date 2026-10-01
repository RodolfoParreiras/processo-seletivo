"use client";

import { Hero } from "@/components/Hero";
import { MyApplications } from "@/features/application/MyApplications";
import { SessionGate } from "@/features/auth/SessionGate";

export default function MyApplicationsPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => (
        <>
          <Hero title="Minhas Candidaturas" subtitle="Acompanhe suas inscrições e baixe os comprovantes." />
          <main>
            <div className="card">
              <MyApplications />
            </div>
          </main>
        </>
      )}
    </SessionGate>
  );
}
