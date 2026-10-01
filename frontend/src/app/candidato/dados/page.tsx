"use client";

import { SessionGate } from "@/features/auth/SessionGate";
import { ProfileForm } from "@/features/candidate/ProfileForm";

export default function MyDataPage() {
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => (
        <main>
          <div className="card">
            <ProfileForm />
          </div>
        </main>
      )}
    </SessionGate>
  );
}
