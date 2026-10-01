export type ProcessStatus =
  | "RASCUNHO"
  | "PUBLICADO"
  | "INSCRICOES_ABERTAS"
  | "INSCRICOES_ENCERRADAS"
  | "ARQUIVADO"
  | "SUSPENSO"
  | "CANCELADO";

export const STATUS_LABELS: Record<ProcessStatus, string> = {
  RASCUNHO: "Rascunho",
  PUBLICADO: "Publicado",
  INSCRICOES_ABERTAS: "Inscrições abertas",
  INSCRICOES_ENCERRADAS: "Inscrições encerradas",
  ARQUIVADO: "Arquivado",
  SUSPENSO: "Suspenso",
  CANCELADO: "Cancelado",
};

export type ProcessStage = "EDITAL_DISPONIVEL" | "GABARITO_DISPONIVEL" | "RESULTADO_PRELIMINAR" | "RESULTADO_FINAL";

/** Etapa de divulgação, definida manualmente pelo administrador. */
export const STAGE_LABELS: Record<ProcessStage, string> = {
  EDITAL_DISPONIVEL: "Edital Disponível",
  GABARITO_DISPONIVEL: "Gabarito Disponível",
  RESULTADO_PRELIMINAR: "Resultado Preliminar",
  RESULTADO_FINAL: "Resultado Final",
};

export interface ProcessSummary {
  id: string;
  number: string;
  year: number;
  title: string;
  department: string;
  status: ProcessStatus;
  stage: ProcessStage | null;
  registrationStart: string;
  registrationEnd: string;
}

export interface ProcessDetail extends ProcessSummary {
  multipleApplicationsAllowed: boolean;
  titleEvaluationEnabled: boolean;
  publishedAt: string | null;
  positions: { id: string; name: string; vacancies: number }[];
  documentRequirements: { id: string; name: string; description: string | null; mandatory: boolean; title: boolean }[];
  notices: { version: number; status: "DRAFT" | "CURRENT" | "SUPERSEDED"; changeReason: string | null; publishedAt: string | null }[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

const DATE_TIME = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
  timeZone: "America/Sao_Paulo",
});

/** Datas exibidas sempre no horário de Brasília, independentemente do fuso do navegador. */
export function formatDateTime(value: string | null): string {
  return value ? DATE_TIME.format(new Date(value)) : "—";
}

/** Converte o valor de um campo datetime-local (interpretado no fuso do navegador) para ISO-8601 UTC. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString();
}

export function isoToLocalInput(value: string): string {
  const date = new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}
