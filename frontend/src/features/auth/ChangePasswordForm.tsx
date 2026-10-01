"use client";

import { type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPut } from "@/lib/api";
import { passwordProblems } from "@/lib/validation";

interface ChangePasswordFormProps {
  fullName?: string;
  birthDate?: string;
}

export function ChangePasswordForm({ fullName = "", birthDate = "" }: ChangePasswordFormProps) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const problems = newPassword ? passwordProblems(newPassword, fullName, birthDate) : [];
  const mismatch = confirmation.length > 0 && confirmation !== newPassword;

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      await apiPut("/api/auth/password", { currentPassword, newPassword });
      setMessage("Senha alterada. As sessões abertas em outros dispositivos foram encerradas.");
      setNewPassword("");
      setConfirmation("");
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível alterar a senha."));
    } finally {
      setCurrentPassword("");
      setSubmitting(false);
    }
  }

  return (
    <section aria-labelledby="password-title">
      <h2 id="password-title">Alterar senha</h2>
      <ErrorAlert error={error} />
      <SuccessAlert message={message} />
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          name="currentPassword"
          label="Senha atual"
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
        />
        <TextField
          name="newPassword"
          label="Nova senha"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          error={problems.length > 0 ? `Falta: ${problems.join(" ")}` : undefined}
          onChange={(event) => setNewPassword(event.target.value)}
        />
        <TextField
          name="newPasswordConfirmation"
          label="Confirme a nova senha"
          type="password"
          autoComplete="new-password"
          value={confirmation}
          error={mismatch ? "As senhas não conferem." : undefined}
          onChange={(event) => setConfirmation(event.target.value)}
        />
        <div className="actions">
          <button
            type="submit"
            disabled={submitting || !currentPassword || !newPassword || problems.length > 0 || confirmation !== newPassword}
          >
            {submitting ? "Salvando..." : "Alterar senha"}
          </button>
        </div>
      </form>
    </section>
  );
}
