"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { SessionGate } from "@/features/auth/SessionGate";
import type { SessionInfo } from "@/lib/api";

export interface AdminContext {
  session: SessionInfo;
  /** Apenas para exibir ou ocultar ações; o backend verifica a permissão em cada requisição. */
  can: (permission: string) => boolean;
}

export function AdminShell({ children }: { children: (context: AdminContext) => ReactNode }) {
  return (
    <SessionGate accountType="ADMIN" loginHref="/admin/entrar">
      {(session, logout) => {
        const can = (permission: string) => session.permissions.includes(permission);
        return (
          <main>
            <nav className="actions" aria-label="Área administrativa">
              <Link href="/admin">Início</Link>
              {can("PROCESSO_VISUALIZAR") && <Link href="/admin/processos">Processos</Link>}
              <span className="hint">{session.displayName}</span>
              <button type="button" className="secondary" onClick={logout}>
                Sair
              </button>
            </nav>
            {children({ session, can })}
          </main>
        );
      }}
    </SessionGate>
  );
}
