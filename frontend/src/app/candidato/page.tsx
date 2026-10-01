"use client";

import { useCallback, useState } from "react";
import { ChangePasswordForm } from "@/features/auth/ChangePasswordForm";
import { SessionGate } from "@/features/auth/SessionGate";
import { ChangeEmailForm } from "@/features/candidate/ChangeEmailForm";
import { type CandidateProfile, ProfileForm } from "@/features/candidate/ProfileForm";
import { MyApplications } from "@/features/application/MyApplications";
import { PublicProcessList } from "@/features/process/PublicProcessList";

type Section = "candidaturas" | "processos" | "dados" | "email" | "senha";

const SECTIONS: { id: Section; label: string }[] = [
  { id: "candidaturas", label: "Minhas Candidaturas" },
  { id: "processos", label: "Processos abertos" },
  { id: "dados", label: "Meus Dados" },
  { id: "email", label: "Alterar e-mail" },
  { id: "senha", label: "Alterar senha" },
];

export default function CandidateAreaPage() {
  const [section, setSection] = useState<Section>("candidaturas");
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [profileVersion, setProfileVersion] = useState(0);
  const handleProfileLoaded = useCallback((loaded: CandidateProfile) => setProfile(loaded), []);

  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {(session, logout) => (
        <main>
          <div className="card">
            <h1>Área do Candidato</h1>
            <p>Olá, {profile?.fullName ?? session.displayName}.</p>
            <nav className="actions" aria-label="Seções da área do candidato">
              {SECTIONS.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className={section === item.id ? undefined : "secondary"}
                  aria-current={section === item.id ? "page" : undefined}
                  onClick={() => setSection(item.id)}
                >
                  {item.label}
                </button>
              ))}
              <button type="button" className="secondary" onClick={logout}>
                Sair
              </button>
            </nav>
            <hr />
            {section === "candidaturas" && <MyApplications />}
            {section === "processos" && (
              <PublicProcessList status="INSCRICOES_ABERTAS" emptyMessage="Nenhum processo com inscrições abertas no momento." />
            )}
            {section === "dados" && <ProfileForm key={profileVersion} onProfileLoaded={handleProfileLoaded} />}
            {section === "email" && <ChangeEmailForm onChanged={() => setProfileVersion((version) => version + 1)} />}
            {section === "senha" && (
              <ChangePasswordForm fullName={profile?.fullName} birthDate={profile?.birthDate} />
            )}
          </div>
        </main>
      )}
    </SessionGate>
  );
}
