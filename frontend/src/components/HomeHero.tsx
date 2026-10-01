"use client";

import { useEffect, useState } from "react";
import { apiGet, type SessionInfo } from "@/lib/api";
import { Hero } from "./Hero";

const SUBTITLE = "Consulte editais, inscreva-se e acompanhe suas candidaturas.";

/** Faixa da página inicial; com candidato logado, cumprimenta pelo primeiro nome. */
export function HomeHero() {
  const [name, setName] = useState<string | null>(null);

  useEffect(() => {
    apiGet<SessionInfo>("/api/auth/session")
      .then((session) => setName(session.accountType === "CANDIDATE" ? session.displayName : null))
      .catch(() => setName(null));
  }, []);

  const title = name ? `Olá, ${name.trim().split(/\s+/)[0]}!` : "Processos Seletivos";
  return <Hero title={title} subtitle={SUBTITLE} />;
}
