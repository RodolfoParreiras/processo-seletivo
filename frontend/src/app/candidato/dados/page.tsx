"use client";

import { Hero } from "@/components/Hero";
import { SessionGate } from "@/features/auth/SessionGate";
import { ProfileForm } from "@/features/candidate/ProfileForm";

export default function MyDataPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => (
        <>
          <Hero title="Meus Dados" subtitle="Mantenha seus dados, e-mail e senha atualizados." />
          <main>
            <div className="card">
              <ProfileForm />
            </div>
          </main>
        </>
      )}
    </SessionGate>
  );
}
