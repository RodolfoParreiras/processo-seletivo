"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { apiGet, apiPost, type SessionInfo } from "@/lib/api";

/** Evento disparado por "Meus Dados" quando o nome muda, para atualizar o cabeçalho sem novo login. */
export const NAME_CHANGED_EVENT = "candidate-name-changed";

const CANDIDATE_LINKS = [
  { href: "/candidato", label: "Processos Abertos" },
  { href: "/candidato/candidaturas", label: "Minhas Candidaturas" },
];

/** Nome curto para o botão do menu: primeiro e último nome. */
function shortName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/);
  return parts.length > 1 ? `${parts[0]} ${parts[parts.length - 1]}` : parts[0];
}

function isActive(pathname: string, href: string): boolean {
  if (href === "/candidato/candidaturas") {
    return pathname.startsWith(href) || pathname.startsWith("/candidato/inscricoes");
  }
  return pathname === href;
}

function UserMenu({ name, onLogout }: { name: string; onLogout: () => void }) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const pathname = usePathname();

  useEffect(() => setOpen(false), [pathname]);

  useEffect(() => {
    if (!open) return;
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  return (
    <div className="user-menu" ref={containerRef}>
      <button
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls="user-menu-options"
        onClick={() => setOpen((current) => !current)}
      >
        <i className="ti ti-user-circle" aria-hidden="true" />
        {shortName(name)}
        <i className={open ? "ti ti-chevron-up" : "ti ti-chevron-down"} aria-hidden="true" />
      </button>
      {open && (
        <ul id="user-menu-options" className="user-menu-options">
          <li>
            <Link href="/candidato/dados">
              <i className="ti ti-id-badge-2" aria-hidden="true" /> Meus Dados
            </Link>
          </li>
          <li>
            <button type="button" className="logout" onClick={onLogout}>
              <i className="ti ti-logout" aria-hidden="true" /> Sair
            </button>
          </li>
        </ul>
      )}
    </div>
  );
}

/**
 * Menu do cabeçalho: com sessão de candidato, mostra a navegação da área logada e o menu do usuário;
 * sem ela, o botão "Área do Candidato".
 */
export function SiteHeaderNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState<SessionInfo | null>(null);

  // Revalida a cada navegação para refletir login e logout.
  useEffect(() => {
    apiGet<SessionInfo>("/api/auth/session")
      .then(setSession)
      .catch(() => setSession(null));
  }, [pathname]);

  useEffect(() => {
    const updateName = (event: Event) => {
      const name = (event as CustomEvent<string>).detail;
      setSession((current) => (current ? { ...current, displayName: name } : current));
    };
    window.addEventListener(NAME_CHANGED_EVENT, updateName);
    return () => window.removeEventListener(NAME_CHANGED_EVENT, updateName);
  }, []);

  async function logout() {
    try {
      await apiPost("/api/auth/logout");
    } finally {
      setSession(null);
      router.replace("/");
    }
  }

  if (session?.accountType !== "CANDIDATE") {
    return (
      <nav className="header-actions" aria-label="Acesso do candidato">
        <Link href="/entrar" className="button">Área do Candidato</Link>
      </nav>
    );
  }

  return (
    <>
      <nav className="main-nav" aria-label="Área do candidato">
        {CANDIDATE_LINKS.map((link) => (
          <Link key={link.href} href={link.href} aria-current={isActive(pathname, link.href) ? "page" : undefined}>
            {link.label}
          </Link>
        ))}
      </nav>
      <div className="header-actions">
        <UserMenu name={session.displayName} onLogout={logout} />
      </div>
    </>
  );
}
