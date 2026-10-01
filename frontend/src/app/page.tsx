import { HomeHero } from "@/components/HomeHero";
import { ProcessBrowser } from "@/features/process/ProcessBrowser";

export default function HomePage() {
  return (
    <>
      <HomeHero />
      <main>
        <h2 className="section-title">Processos seletivos</h2>
        <ProcessBrowser />
      </main>
    </>
  );
}
