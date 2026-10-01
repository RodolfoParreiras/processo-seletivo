"use client";

import { type FormEvent, useEffect, useState } from "react";
import { NAME_CHANGED_EVENT } from "@/components/SiteHeaderNav";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import {
  type Adaptation,
  EMPTY_PERSONAL_DATA,
  type PersonalData,
  PersonalDataFields,
  TextField,
  toPersonalDataPayload,
  validatePersonalData,
} from "@/features/candidate/PersonalDataFields";
import { ApiError, apiGet, apiPut } from "@/lib/api";
import { maskCep, maskCpf, maskPhone, passwordErrorMessage, passwordProblems } from "@/lib/validation";

export interface CandidateProfile {
  cpf: string;
  fullName: string;
  birthDate: string;
  motherName: string;
  email: string;
  phone: string;
  cep: string;
  street: string;
  addressNumber: string;
  complement: string | null;
  neighborhood: string;
  city: string;
  uf: string;
  hasDisability: boolean;
  adaptations: Adaptation[];
}

interface Access {
  email: string;
  newPassword: string;
  newPasswordConfirmation: string;
  currentPassword: string;
}

function toFormData(profile: CandidateProfile): PersonalData {
  return {
    fullName: profile.fullName,
    birthDate: profile.birthDate,
    motherName: profile.motherName,
    phone: maskPhone(profile.phone),
    cep: maskCep(profile.cep),
    street: profile.street,
    addressNumber: profile.addressNumber,
    complement: profile.complement ?? "",
    neighborhood: profile.neighborhood,
    city: profile.city,
    uf: profile.uf,
    hasDisability: profile.hasDisability ? "SIM" : "NAO",
    adaptations: profile.adaptations,
  };
}

const samePersonalData = (a: PersonalData, b: PersonalData) =>
  JSON.stringify(toPersonalDataPayload(a)) === JSON.stringify(toPersonalDataPayload(b));

/**
 * "Meus Dados": dados pessoais, e-mail e senha num único formulário. Ao salvar, envia apenas o que mudou,
 * cada parte ao seu endpoint; e-mail e senha exigem a senha atual.
 */
export function ProfileForm() {
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [data, setData] = useState<PersonalData>(EMPTY_PERSONAL_DATA);
  const [access, setAccess] = useState<Access>({
    email: "",
    newPassword: "",
    newPasswordConfirmation: "",
    currentPassword: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiGet<CandidateProfile>("/api/candidate/me")
      .then((loaded) => {
        setProfile(loaded);
        setData(toFormData(loaded));
        setAccess((current) => ({ ...current, email: loaded.email }));
      })
      .catch((caught: unknown) => setApiError(caught instanceof ApiError ? caught : null));
  }, []);

  const setAccessField = (field: keyof Access, value: string) =>
    setAccess((current) => ({ ...current, [field]: value }));

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!profile) return;
    setApiError(null);
    setMessage(null);

    const personalChanged = !samePersonalData(data, toFormData(profile));
    const newEmail = access.email.trim();
    const emailChanged = newEmail.toLowerCase() !== profile.email.toLowerCase();
    const passwordChanged = access.newPassword.length > 0;

    const validationErrors = validatePersonalData(data);
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail)) validationErrors.email = "E-mail inválido.";
    if (passwordChanged) {
      const problems = passwordProblems(access.newPassword, data.fullName, data.birthDate);
      if (problems.length > 0) validationErrors.newPassword = passwordErrorMessage(problems);
      if (access.newPasswordConfirmation !== access.newPassword) {
        validationErrors.newPasswordConfirmation = "As senhas não conferem.";
      }
    }
    if ((emailChanged || passwordChanged) && !access.currentPassword) {
      validationErrors.currentPassword = "Informe sua senha atual para alterar e-mail ou senha.";
    }
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;
    if (!personalChanged && !emailChanged && !passwordChanged) {
      setMessage("Nenhuma alteração para salvar.");
      return;
    }

    setSubmitting(true);
    const saved: string[] = [];
    try {
      if (personalChanged) {
        const updated = await apiPut<CandidateProfile>("/api/candidate/me", toPersonalDataPayload(data));
        setProfile(updated);
        setData(toFormData(updated));
        window.dispatchEvent(new CustomEvent(NAME_CHANGED_EVENT, { detail: updated.fullName }));
        saved.push("Dados atualizados. Inscrições já realizadas mantêm os dados informados na data da inscrição.");
      }
      // O e-mail vem antes da senha: a troca de senha invalida a senha atual usada na confirmação.
      if (emailChanged) {
        await apiPut("/api/candidate/me/email", { newEmail, currentPassword: access.currentPassword });
        setProfile((current) => (current ? { ...current, email: newEmail } : current));
        saved.push("E-mail alterado. Um aviso foi enviado ao endereço anterior.");
      }
      if (passwordChanged) {
        await apiPut("/api/auth/password", {
          currentPassword: access.currentPassword,
          newPassword: access.newPassword,
        });
        setAccess((current) => ({ ...current, newPassword: "", newPasswordConfirmation: "" }));
        saved.push("Senha alterada. As sessões abertas em outros dispositivos foram encerradas.");
      }
    } catch (caught) {
      if (caught instanceof ApiError) {
        const { newEmail: emailError, ...fieldErrors } = caught.fieldErrors;
        setErrors(emailError ? { ...fieldErrors, email: emailError } : fieldErrors);
        setApiError(caught);
      } else {
        setApiError(new ApiError(0, "Não foi possível salvar as alterações."));
      }
    } finally {
      setAccessField("currentPassword", "");
      setMessage(saved.length > 0 ? saved.join(" ") : null);
      setSubmitting(false);
    }
  }

  if (!profile) {
    return apiError ? <ErrorAlert error={apiError} /> : <p aria-live="polite">Carregando...</p>;
  }

  return (
    <section aria-label="Meus Dados">
      <p className="hint">CPF {maskCpf(profile.cpf)} · Campos marcados com * são obrigatórios.</p>
      <ErrorAlert error={apiError} />
      <SuccessAlert message={message} />
      <form className="numbered-sections three-columns" onSubmit={handleSubmit} noValidate>
        <PersonalDataFields data={data} errors={errors} onChange={setData} />

        <fieldset>
          <legend>Acesso</legend>
          <div className="grid">
            <TextField
              name="email"
              label="E-mail"
              icon="mail"
              type="email"
              autoComplete="email"
              maxLength={254}
              fieldClassName="span-2"
              required
              value={access.email}
              error={errors.email}
              onChange={(event) => setAccessField("email", event.target.value)}
            />
          </div>
          <div className="grid">
            <TextField
              name="newPassword"
              label="Nova senha"
              icon="lock"
              type="password"
              autoComplete="new-password"
              placeholder="Deixe em branco para manter"
              value={access.newPassword}
              error={errors.newPassword}
              onChange={(event) => setAccessField("newPassword", event.target.value)}
            />
            <TextField
              name="newPasswordConfirmation"
              label="Confirme a nova senha"
              icon="lock"
              type="password"
              autoComplete="new-password"
              placeholder="Repita a nova senha"
              value={access.newPasswordConfirmation}
              error={errors.newPasswordConfirmation}
              onChange={(event) => setAccessField("newPasswordConfirmation", event.target.value)}
            />
          </div>
          <div className="confirm-box">
            <p className="hint">
              <i className="ti ti-shield-lock" aria-hidden="true" /> Para alterar e-mail ou senha, confirme sua senha
              atual.
            </p>
            <TextField
              name="currentPassword"
              label="Senha atual"
              icon="lock"
              type="password"
              autoComplete="current-password"
              placeholder="Digite sua senha atual"
              value={access.currentPassword}
              error={errors.currentPassword}
              onChange={(event) => setAccessField("currentPassword", event.target.value)}
            />
          </div>
        </fieldset>

        <div className="form-footer">
          <button type="submit" disabled={submitting}>
            {submitting ? "Salvando..." : "Salvar alterações"}
          </button>
        </div>
      </form>
    </section>
  );
}
