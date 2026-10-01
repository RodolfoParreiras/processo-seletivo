"use client";

import { useRouter } from "next/navigation";
import { type ReactNode, useEffect, useState } from "react";
import { type AccountType, ApiError, apiGet, apiPost, type SessionInfo } from "@/lib/api";

interface SessionGateProps {
  accountType: AccountType;
  loginHref: string;
  children: (session: SessionInfo, logout: () => Promise<void>) => ReactNode;
}

/**
 * Exibe a área somente com sessão do tipo esperado. É apenas conveniência de navegação:
 * cada endpoint da API faz a própria verificação de autenticação e autorização.
 */
export function SessionGate({ accountType, loginHref, children }: SessionGateProps) {
  const router = useRouter();
  const [session, setSession] = useState<SessionInfo | null>(null);

  useEffect(() => {
    apiGet<SessionInfo>("/api/auth/session")
      .then((current) => {
        if (current.accountType === accountType) {
          setSession(current);
        } else {
          router.replace(loginHref);
        }
      })
      .catch((error: unknown) => {
        if (error instanceof ApiError && error.status === 401) {
          router.replace(loginHref);
        }
      });
  }, [accountType, loginHref, router]);

  async function logout() {
    try {
      await apiPost("/api/auth/logout");
    } finally {
      router.replace(loginHref);
    }
  }

  if (!session) {
    return (
      <main>
        <p aria-live="polite">Carregando...</p>
      </main>
    );
  }
  return <>{children(session, logout)}</>;
}
