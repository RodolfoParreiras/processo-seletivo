"use client";

import { type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPut } from "@/lib/api";

export function ChangeEmailForm({ onChanged }: { onChanged: () => void }) {
  const [newEmail, setNewEmail] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      await apiPut("/api/candidate/me/email", { newEmail: newEmail.trim(), currentPassword });
      setMessage("E-mail alterado. Um aviso foi enviado ao endereço anterior.");
      setNewEmail("");
      onChanged();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível alterar o e-mail."));
    } finally {
      setCurrentPassword("");
      setSubmitting(false);
    }
  }

  return (
    <section aria-labelledby="email-title">
      <h2 id="email-title">Alterar e-mail</h2>
      <p className="hint">O e-mail é usado para recuperar a senha e receber comunicações das inscrições.</p>
      <ErrorAlert error={error} />
      <SuccessAlert message={message} />
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          name="newEmail"
          label="Novo e-mail"
          type="email"
          autoComplete="email"
          maxLength={254}
          value={newEmail}
          onChange={(event) => setNewEmail(event.target.value)}
        />
        <TextField
          name="emailCurrentPassword"
          label="Senha atual"
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
        />
        <div className="actions">
          <button type="submit" disabled={submitting || !newEmail || !currentPassword}>
            {submitting ? "Salvando..." : "Alterar e-mail"}
          </button>
        </div>
      </form>
    </section>
  );
}
