"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { ApiError, apiPost } from "@/lib/api";

interface ForgotPasswordFormProps {
  endpoint: string;
  loginHref: string;
}

export function ForgotPasswordForm({ endpoint, loginHref }: ForgotPasswordFormProps) {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      const response = await apiPost<{ message: string }>(endpoint, { email: email.trim() });
      setMessage(response.message);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível enviar a solicitação."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <div className="card">
        <h1>Recuperar senha</h1>
        <p className="hint">Informe o e-mail cadastrado. Você receberá um link válido por 30 minutos.</p>
        <ErrorAlert error={error} />
        <SuccessAlert message={message} />
        <form onSubmit={handleSubmit} noValidate>
          <div className="field">
            <label htmlFor="email">E-mail</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
          <div className="actions">
            <button type="submit" disabled={submitting || !email}>
              {submitting ? "Enviando..." : "Enviar link"}
            </button>
            <Link href={loginHref}>Voltar para o login</Link>
          </div>
        </form>
      </div>
    </main>
  );
}
