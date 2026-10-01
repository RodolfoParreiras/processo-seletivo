"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
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
    <main className="auth-page">
      <div className="card auth-card">
        <div className="auth-header">
          <span className="auth-icon"><i className="ti ti-key" aria-hidden="true" /></span>
          <h1>Recuperar senha</h1>
          <p className="hint">Informe o e-mail cadastrado. Enviaremos um link válido por 30 minutos.</p>
        </div>
        <ErrorAlert error={error} />
        <SuccessAlert message={message} />
        <form onSubmit={handleSubmit} noValidate>
          <TextField name="email" label="E-mail" icon="mail" type="email" autoComplete="email"
            placeholder="Digite seu e-mail" required value={email}
            onChange={(event) => setEmail(event.target.value)} />
          <button type="submit" className="block" disabled={submitting || !email}>
            {submitting ? "Enviando..." : "Enviar link"}
          </button>
        </form>
        <p className="auth-back">
          <Link href={loginHref}><i className="ti ti-arrow-left" aria-hidden="true" /> Voltar para o login</Link>
        </p>
      </div>
    </main>
  );
}
