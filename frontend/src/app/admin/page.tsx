"use client";

import { ChangePasswordForm } from "@/features/auth/ChangePasswordForm";
import { SessionGate } from "@/features/auth/SessionGate";

// Área provisória: as funcionalidades administrativas entram a partir da fase 4.
export default function AdminAreaPage() {
  return (
    <SessionGate accountType="ADMIN" loginHref="/admin/entrar">
      {(session, logout) => (
        <main>
          <div className="card">
            <h1>Área Administrativa</h1>
            <p>Olá, {session.displayName}.</p>
            <p className="hint">Permissões: {session.permissions.length}</p>
            <div className="actions">
              <button type="button" className="secondary" onClick={logout}>
                Sair
              </button>
            </div>
            <hr />
            <ChangePasswordForm fullName={session.displayName} />
          </div>
        </main>
      )}
    </SessionGate>
  );
}
