/**
 * Validações e máscaras para usabilidade. O backend repete todas as validações (ESPECIFICACAO §62).
 */

export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

export function isValidCpf(value: string): boolean {
  const digits = onlyDigits(value);
  if (digits.length !== 11 || /^(\d)\1{10}$/.test(digits)) {
    return false;
  }
  const checkDigit = (length: number) => {
    let sum = 0;
    for (let i = 0; i < length; i++) {
      sum += Number(digits[i]) * (length + 1 - i);
    }
    const remainder = (sum * 10) % 11;
    return remainder === 10 ? 0 : remainder;
  };
  return checkDigit(9) === Number(digits[9]) && checkDigit(10) === Number(digits[10]);
}

export function maskCpf(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  return digits
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d{1,2})$/, ".$1-$2");
}

export function maskPhone(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  if (digits.length <= 10) {
    return digits.replace(/^(\d{2})(\d)/, "($1) $2").replace(/(\d{4})(\d{1,4})$/, "$1-$2");
  }
  return digits.replace(/^(\d{2})(\d{5})(\d{1,4})$/, "($1) $2-$3");
}

export function maskCep(value: string): string {
  return onlyDigits(value).slice(0, 8).replace(/^(\d{5})(\d{1,3})$/, "$1-$2");
}

/** Espelha docs/DECISOES.md; a verificação definitiva é a do backend. */
export function passwordProblems(password: string, fullName: string, birthDate: string): string[] {
  const problems: string[] = [];
  if (password.length < 8) problems.push("No mínimo 8 caracteres.");
  if (!/\p{Lu}/u.test(password)) problems.push("Letra maiúscula.");
  if (!/\p{Ll}/u.test(password)) problems.push("Letra minúscula.");
  if (!/\d/.test(password)) problems.push("Número.");
  if (!/[^\p{L}\p{N}\s]/u.test(password)) problems.push("Caractere especial.");

  const normalize = (text: string) => text.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
  const particles = new Set(["da", "de", "do", "das", "dos", "e"]);
  const normalizedPassword = normalize(password);
  const nameParts = normalize(fullName)
    .split(/[\s\-']+/)
    .filter((part) => part.length >= 3 && !particles.has(part));
  if (nameParts.some((part) => normalizedPassword.includes(part))) {
    problems.push("Não pode conter seu nome ou sobrenome.");
  }

  const [year, month, day] = birthDate.split("-");
  if (year && month && day) {
    const passwordDigits = onlyDigits(password);
    const formats = [`${day}${month}${year}`, `${day}${month}${year.slice(2)}`, `${year}${month}${day}`];
    if (formats.some((format) => passwordDigits.includes(format))) {
      problems.push("Não pode conter sua data de nascimento.");
    }
  }
  return problems;
}

export const UFS = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
  "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
] as const;

export const ADAPTATIONS = [
  { value: "LIBRAS_INTERPRETER", label: "Intérprete de Libras" },
  { value: "READING_ASSISTANCE", label: "Auxílio Ledor" },
  { value: "ENLARGED_TEST", label: "Prova Ampliada" },
  { value: "NONE", label: "Nenhuma" },
] as const;
