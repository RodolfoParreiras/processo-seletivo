import Link from "next/link";

interface HeroProps {
  title: string;
  subtitle?: string;
  /** Link de volta exibido acima do título. */
  back?: { href: string; label: string };
}

/** Faixa azul de abertura das páginas, com o título e uma frase de apoio. */
export function Hero({ title, subtitle, back }: HeroProps) {
  return (
    <section className="hero">
      <div className="container">
        {back && (
          <Link className="hero-back" href={back.href}>
            <i className="ti ti-arrow-left" aria-hidden="true" /> {back.label}
          </Link>
        )}
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
    </section>
  );
}
