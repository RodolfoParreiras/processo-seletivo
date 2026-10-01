import { Hero } from "@/components/Hero";
import { ProcessAccordion } from "@/features/process/ProcessAccordion";

export default function HomePage() {
  return (
    <>
      <Hero title="Processos Seletivos" subtitle="Consulte editais, inscreva-se e acompanhe suas candidaturas." />
      <main>
        <h2 className="section-title">Processos seletivos</h2>
        <ProcessAccordion emptyMessage="Nenhum processo seletivo publicado no momento." />
      </main>
    </>
  );
}
