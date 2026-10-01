"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { PasswordRules } from "@/features/auth/PasswordRules";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPost } from "@/lib/api";
import { passwordProblems } from "@/lib/validation";

export function ResetPasswordForm() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    // O token chega no fragmento (#token=...), que o navegador não envia ao servidor.
    const fragment = new URLSearchParams(window.location.hash.substring(1));
    setToken(fragment.get("token"));
    // Remove o token da barra de endereço e do histórico.
    window.history.replaceState(null, "", window.location.pathname);
  }, []);

  // Nome e data de nascimento são verificados pelo backend, que conhece os dados da conta.
  const problems = password ? passwordProblems(password, "", "") : [];
  const mismatch = confirmation.length > 0 && confirmation !== password;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || problems.length > 0 || mismatch) {
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await apiPost("/api/auth/reset-password", { token, newPassword: password });
      setDone(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível redefinir a senha."));
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <main className="auth-page">
        <div className="card auth-card">
          <div className="auth-header">
            <span className="auth-icon"><i className="ti ti-circle-check" aria-hidden="true" /></span>
            <h1>Senha definida</h1>
          </div>
          <SuccessAlert message="Sua senha foi alterada. Por segurança, todas as sessões abertas foram encerradas." />
          <Link className="button block" href="/entrar">Entrar como candidato</Link>
          <p className="auth-back">
            <Link href="/admin/entrar">Entrar na área administrativa</Link>
          </p>
        </div>
      </main>
    );
  }

  return (
    <main className="auth-page">
      <div className="card auth-card">
        <div className="auth-header">
          <span className="auth-icon"><i className="ti ti-lock" aria-hidden="true" /></span>
          <h1>Definir nova senha</h1>
          <p className="hint">Campos marcados com * são obrigatórios.</p>
        </div>
        {token === null && (
          <div className="alert alert-error" role="alert">
            Link inválido. Solicite um novo link de redefinição de senha.
          </div>
        )}
        <ErrorAlert error={error} />
        <form onSubmit={handleSubmit} noValidate>
          <TextField name="password" label="Nova senha" icon="lock" type="password" autoComplete="new-password"
            placeholder="Crie uma senha" required value={password}
            onChange={(event) => setPassword(event.target.value)} />
          <PasswordRules password={password} problems={problems} checksPersonalData={false} />
          <TextField name="confirmation" label="Confirme a nova senha" icon="lock" type="password"
            autoComplete="new-password" placeholder="Repita a senha" required value={confirmation}
            error={mismatch ? "As senhas não conferem." : undefined}
            onChange={(event) => setConfirmation(event.target.value)} />
          <button
            type="submit"
            className="block"
            disabled={submitting || !token || !password || problems.length > 0 || confirmation !== password}
          >
            {submitting ? "Salvando..." : "Salvar senha"}
          </button>
        </form>
      </div>
    </main>
  );
}
