"use client";

import { AdminShell } from "@/features/admin/AdminShell";
import { ChangePasswordForm } from "@/features/auth/ChangePasswordForm";

export default function AdminHomePage() {
  return (
    <AdminShell>
      {({ session }) => (
        <div className="card">
          <h1>Área Administrativa</h1>
          <p>Olá, {session.displayName}.</p>
          <hr />
          <ChangePasswordForm fullName={session.displayName} />
        </div>
      )}
    </AdminShell>
  );
}
