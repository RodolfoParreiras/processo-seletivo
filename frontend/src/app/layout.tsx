import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Link from "next/link";
import { connection } from "next/server";
import type { ReactNode } from "react";
import "@tabler/icons-webfont/dist/tabler-icons.min.css";
import "./globals.css";

// A fonte é baixada no build e servida pela própria aplicação (sem requisição a terceiros em tempo de uso).
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });

export const metadata: Metadata = {
  title: "Processos Seletivos — Prefeitura de Paraíba do Sul",
  description: "Sistema de Gestão de Processos Seletivos da Prefeitura Municipal de Paraíba do Sul.",
};

const PORTAL_URL = "https://paraibadosul.rj.gov.br";

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Páginas pré-renderizadas não recebem o nonce do CSP (src/proxy.ts); a renderização precisa ser por requisição.
  await connection();

  return (
    <html lang="pt-BR" className={inter.variable}>
      <body>
        <div className="top-bar">
          <div className="container">
            <a href={PORTAL_URL} rel="noopener noreferrer">
              <i className="ti ti-building-community" aria-hidden="true" /> Portal da Prefeitura
            </a>
            <Link href="/verificar-comprovante">
              <i className="ti ti-file-check" aria-hidden="true" /> Validar documentos
            </Link>
          </div>
        </div>
        <header className="site-header">
          <div className="container">
            <Link href="/" className="brand">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src="/logo-prefeitura.png" alt="Prefeitura Municipal de Paraíba do Sul" width={220} height={44} />
            </Link>
            <nav className="header-actions" aria-label="Acesso do candidato">
              <Link href="/cadastro" className="button secondary">Criar conta</Link>
              <Link href="/entrar" className="button">Área do Candidato</Link>
            </nav>
          </div>
        </header>
        {children}
        <footer className="site-footer">
          <div className="container">
            <div>
              <strong>Prefeitura Municipal de Paraíba do Sul</strong>
              <div className="muted">R. Visconde da Paraíba, 11 · Centro · Paraíba do Sul/RJ · 25850-000</div>
            </div>
            <div className="muted">
              <a href={PORTAL_URL} rel="noopener noreferrer">Portal da Prefeitura</a>
              <br />
              <Link href="/verificar-comprovante">Validar documentos</Link>
            </div>
            <div className="muted copyright">© {new Date().getFullYear()} Prefeitura Municipal de Paraíba do Sul</div>
          </div>
        </footer>
      </body>
    </html>
  );
}
