"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { ApiError, apiPost, type SessionInfo } from "@/lib/api";
import { maskCpf, onlyDigits } from "@/lib/validation";

interface LoginFormProps {
  title: string;
  endpoint: string;
  redirectTo: string;
  forgotPasswordHref: string;
  registerHref?: string;
}

export function LoginForm({ title, endpoint, redirectTo, forgotPasswordHref, registerHref }: LoginFormProps) {
  const router = useRouter();
  const [cpf, setCpf] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiPost<SessionInfo>(endpoint, { cpf: onlyDigits(cpf), password });
      router.replace(redirectTo);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível entrar. Tente novamente."));
      setPassword("");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <div className="card">
        <h1>{title}</h1>
        <ErrorAlert error={error} />
        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="cpf">CPF</label>
            <input
              id="cpf"
              name="cpf"
              inputMode="numeric"
              autoComplete="username"
              required
              value={cpf}
              onChange={(event) => setCpf(maskCpf(event.target.value))}
            />
          </div>
          <div className="field">
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>
          <div className="actions">
            <button type="submit" disabled={submitting || !cpf || !password}>
              {submitting ? "Entrando..." : "Entrar"}
            </button>
            <Link href={forgotPasswordHref}>Esqueci minha senha</Link>
            {registerHref && <Link href={registerHref}>Criar conta</Link>}
          </div>
        </form>
      </div>
    </main>
  );
}
