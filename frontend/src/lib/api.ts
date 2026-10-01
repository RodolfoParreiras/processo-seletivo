/**
 * Cliente da API (mesma origem, via Nginx). A sessão fica em cookie HttpOnly;
 * requisições que alteram estado enviam o token CSRF lido do cookie XSRF-TOKEN.
 */

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
    readonly violations: string[] = [],
  ) {
    super(message);
  }
}

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const GENERIC_ERROR = "Não foi possível concluir a operação. Tente novamente.";

function readCookie(name: string): string | undefined {
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`))
    ?.substring(name.length + 1);
}

async function csrfToken(): Promise<string> {
  let token = readCookie(CSRF_COOKIE);
  if (!token) {
    await fetch("/api/auth/csrf", { credentials: "same-origin" });
    token = readCookie(CSRF_COOKIE);
  }
  if (!token) {
    throw new ApiError(0, GENERIC_ERROR);
  }
  return decodeURIComponent(token);
}

async function toApiError(response: Response): Promise<ApiError> {
  if (response.status === 429) {
    return new ApiError(429, "Muitas tentativas em pouco tempo. Aguarde alguns minutos e tente novamente.");
  }
  try {
    const problem = await response.json();
    return new ApiError(
      response.status,
      typeof problem.detail === "string" ? problem.detail : GENERIC_ERROR,
      problem.fieldErrors ?? {},
      problem.violations ?? [],
    );
  } catch {
    return new ApiError(response.status, GENERIC_ERROR);
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(path, { credentials: "same-origin", headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw await toApiError(response);
  }
  return response.json() as Promise<T>;
}

export function apiPost<T = void>(path: string, body?: unknown): Promise<T> {
  return sendWithCsrf<T>("POST", path, body);
}

export function apiPut<T = void>(path: string, body?: unknown): Promise<T> {
  return sendWithCsrf<T>("PUT", path, body);
}

async function sendWithCsrf<T>(method: "POST" | "PUT", path: string, body?: unknown): Promise<T> {
  const send = async () =>
    fetch(path, {
      method,
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        [CSRF_HEADER]: await csrfToken(),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });

  let response = await send();
  if (response.status === 403) {
    // Token CSRF pode ter sido renovado (ex.: após login/logout): obtém outro e tenta uma única vez.
    document.cookie = `${CSRF_COOKIE}=; Max-Age=0; path=/`;
    response = await send();
  }
  if (!response.ok) {
    throw await toApiError(response);
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export type AccountType = "CANDIDATE" | "ADMIN";

export interface SessionInfo {
  accountType: AccountType;
  displayName: string;
  permissions: string[];
}
