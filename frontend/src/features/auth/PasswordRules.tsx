/** Regras exibidas como lista; cada uma é atendida quando nenhuma das mensagens associadas aparece. */
const BASIC_RULES = [
  { label: "Mínimo de 8 caracteres", problems: ["No mínimo 8 caracteres."] },
  { label: "Letra maiúscula", problems: ["Letra maiúscula."] },
  { label: "Letra minúscula", problems: ["Letra minúscula."] },
  { label: "Número", problems: ["Número."] },
  { label: "Caractere especial", problems: ["Caractere especial."] },
];

const PERSONAL_DATA_RULE = {
  label: "Sem nome, sobrenome ou data de nascimento",
  problems: ["Não pode conter seu nome ou sobrenome.", "Não pode conter sua data de nascimento."],
};

interface PasswordRulesProps {
  password: string;
  /** Resultado de passwordProblems para a senha digitada. */
  problems: string[];
  /** Só é possível conferir nome e data de nascimento quando o formulário os conhece. */
  checksPersonalData: boolean;
}

export function PasswordRules({ password, problems, checksPersonalData }: PasswordRulesProps) {
  const rules = checksPersonalData ? [...BASIC_RULES, PERSONAL_DATA_RULE] : BASIC_RULES;
  return (
    <>
      <ul className="password-rules" aria-label="Requisitos da senha">
        {rules.map((rule) => {
          const met = password.length > 0 && !rule.problems.some((problem) => problems.includes(problem));
          return (
            <li key={rule.label} className={met ? "met" : undefined}>
              <i className={met ? "ti ti-circle-check" : "ti ti-circle"} aria-hidden="true" />
              {rule.label}
              <span className="sr-only">{met ? " (atendido)" : " (pendente)"}</span>
            </li>
          );
        })}
      </ul>
      {!checksPersonalData && <p className="hint">Não use seu nome, sobrenome ou data de nascimento.</p>}
    </>
  );
}
