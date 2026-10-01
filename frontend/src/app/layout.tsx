import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "Processos Seletivos — Prefeitura de Paraíba do Sul",
  description: "Sistema de Gestão de Processos Seletivos da Prefeitura Municipal de Paraíba do Sul.",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Páginas pré-renderizadas não recebem o nonce do CSP (src/proxy.ts); a renderização precisa ser por requisição.
  await connection();

  return (
    <html lang="pt-BR">
      <body>
        <header className="site-header">
          <Link href="/">Processos Seletivos — Prefeitura de Paraíba do Sul</Link>
        </header>
        {children}
      </body>
    </html>
  );
}
