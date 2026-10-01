import { ProcessAccordion } from "@/features/process/ProcessAccordion";

export default function HomePage() {
  return (
    <>
      <section className="hero">
        <div className="container">
          <div className="eyebrow">Prefeitura Municipal de Paraíba do Sul</div>
          <h1>Processos Seletivos</h1>
          <p>Consulte editais, inscreva-se e acompanhe suas candidaturas.</p>
        </div>
      </section>
      <main>
        <h2 className="section-title">Processos seletivos</h2>
        <ProcessAccordion emptyMessage="Nenhum processo seletivo publicado no momento." />
      </main>
    </>
  );
}
