"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPost, type SessionInfo } from "@/lib/api";
import { maskCpf, onlyDigits, safeRedirectPath } from "@/lib/validation";

interface LoginFormProps {
  title: string;
  subtitle?: string;
  endpoint: string;
  redirectTo: string;
  forgotPasswordHref: string;
  registerHref?: string;
}

export function LoginForm({ title, subtitle, endpoint, redirectTo, forgotPasswordHref, registerHref }: LoginFormProps) {
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
      router.replace(safeRedirectPath(new URLSearchParams(window.location.search).get("redirect"), redirectTo));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível entrar. Tente novamente."));
      setPassword("");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="card auth-card">
        <div className="auth-header">
          <span className="auth-icon"><i className="ti ti-user" aria-hidden="true" /></span>
          <h1>{title}</h1>
          {subtitle && <p className="hint">{subtitle}</p>}
        </div>
        <ErrorAlert error={error} />
        <form onSubmit={handleSubmit} noValidate>
          <TextField name="cpf" label="CPF" icon="id" inputMode="numeric" autoComplete="username"
            placeholder="000.000.000-00" required value={cpf}
            onChange={(event) => setCpf(maskCpf(event.target.value))} />
          <TextField name="password" label="Senha" icon="lock" type="password" autoComplete="current-password"
            placeholder="Digite sua senha" required value={password}
            labelAside={<Link href={forgotPasswordHref}>Esqueci minha senha</Link>}
            onChange={(event) => setPassword(event.target.value)} />
          <button type="submit" className="block" disabled={submitting || !cpf || !password}>
            {submitting ? "Entrando..." : "Entrar"}
          </button>
        </form>
        {registerHref && (
          <div className="auth-register">
            <p className="hint">Ainda não tem cadastro?</p>
            <Link href={registerHref} className="button block">Criar conta</Link>
          </div>
        )}
      </div>
    </main>
  );
}
