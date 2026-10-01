/** Faixa azul de abertura das páginas, com o título e uma frase de apoio. */
export function Hero({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <section className="hero">
      <div className="container">
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
    </section>
  );
}
