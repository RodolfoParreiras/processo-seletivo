import Link from "next/link";

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
    </main>
  );
}
