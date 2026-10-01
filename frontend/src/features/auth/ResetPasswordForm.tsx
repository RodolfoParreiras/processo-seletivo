"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
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
      <main>
        <div className="card">
          <h1>Senha definida</h1>
          <SuccessAlert message="Sua senha foi alterada. Por segurança, todas as sessões abertas foram encerradas." />
          <div className="actions">
            <Link className="button" href="/entrar">Entrar como candidato</Link>
            <Link href="/admin/entrar">Entrar na área administrativa</Link>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main>
      <div className="card">
        <h1>Definir nova senha</h1>
        {token === null && (
          <div className="alert alert-error" role="alert">
            Link inválido. Solicite um novo link de redefinição de senha.
          </div>
        )}
        <ErrorAlert error={error} />
        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="password">Nova senha</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              required
              aria-invalid={problems.length > 0}
              aria-describedby="password-rules"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
            <p id="password-rules" className="hint">
              Mínimo de 8 caracteres, com letra maiúscula, minúscula, número e caractere especial. Não use seu
              nome, sobrenome ou data de nascimento.
            </p>
            {problems.length > 0 && <p className="field-error">Falta: {problems.join(" ")}</p>}
          </div>
          <div className="field">
            <label htmlFor="confirmation">Confirme a nova senha</label>
            <input
              id="confirmation"
              type="password"
              autoComplete="new-password"
              required
              aria-invalid={mismatch}
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
            />
            {mismatch && <p className="field-error">As senhas não conferem.</p>}
          </div>
          <div className="actions">
            <button
              type="submit"
              disabled={submitting || !token || !password || problems.length > 0 || confirmation !== password}
            >
              {submitting ? "Salvando..." : "Salvar senha"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
}
