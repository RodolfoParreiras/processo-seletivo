import Link from "next/link";
import { PublicProcessList } from "@/features/process/PublicProcessList";

export default function HomePage() {
  return (
    <main>
      <div className="card">
        <h1>Sistema de Gestão de Processos Seletivos</h1>
        <p>Prefeitura Municipal de Paraíba do Sul</p>
        <div className="actions">
          <Link className="button" href="/entrar">Entrar</Link>
          <Link href="/cadastro">Criar conta de candidato</Link>
        </div>
      </div>
      <div className="card">
        <h2>Processos seletivos</h2>
        <PublicProcessList emptyMessage="Nenhum processo seletivo publicado." />
      </div>
    </main>
  );
}
