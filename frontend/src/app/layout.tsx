import type { Metadata } from "next";
import { connection } from "next/server";
import type { ReactNode } from "react";

export const metadata: Metadata = {
  title: "Processos Seletivos — Prefeitura de Paraíba do Sul",
  description: "Sistema de Gestão de Processos Seletivos da Prefeitura Municipal de Paraíba do Sul.",
};

export default async function RootLayout({ children }: { children: ReactNode }) {
  // Páginas pré-renderizadas não recebem o nonce do CSP (src/proxy.ts); a renderização precisa ser por requisição.
  await connection();

  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
