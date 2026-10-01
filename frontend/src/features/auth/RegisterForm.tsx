"use client";

import Link from "next/link";
import { type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import {
  EMPTY_PERSONAL_DATA,
  type PersonalData,
  PersonalDataFields,
  TextField,
  toPersonalDataPayload,
  validatePersonalData,
} from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPost } from "@/lib/api";
import { isValidCpf, maskCpf, onlyDigits, passwordErrorMessage, passwordProblems } from "@/lib/validation";

interface Credentials {
  cpf: string;
  email: string;
  password: string;
  passwordConfirmation: string;
}

function validateCredentials(credentials: Credentials, personalData: PersonalData): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!isValidCpf(credentials.cpf)) errors.cpf = "CPF inválido.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(credentials.email.trim())) errors.email = "E-mail inválido.";
  const problems = passwordProblems(credentials.password, personalData.fullName, personalData.birthDate);
  if (problems.length > 0) {
    errors.password = passwordErrorMessage(problems);
  }
  if (credentials.passwordConfirmation !== credentials.password) {
    errors.passwordConfirmation = "As senhas não conferem.";
  }
  return errors;
}

export function RegisterForm() {
  const [personalData, setPersonalData] = useState<PersonalData>(EMPTY_PERSONAL_DATA);
  const [credentials, setCredentials] = useState<Credentials>({
    cpf: "",
    email: "",
    password: "",
    passwordConfirmation: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [registered, setRegistered] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const setCredential = (field: keyof Credentials, value: string) =>
    setCredentials((current) => ({ ...current, [field]: field === "cpf" ? maskCpf(value) : value }));

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setApiError(null);
    const validationErrors = {
      ...validatePersonalData(personalData),
      ...validateCredentials(credentials, personalData),
    };
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }
    setSubmitting(true);
    try {
      await apiPost("/api/auth/register", {
        ...toPersonalDataPayload(personalData),
        cpf: onlyDigits(credentials.cpf),
        email: credentials.email.trim(),
        password: credentials.password,
      });
      setRegistered(true);
    } catch (caught) {
      if (caught instanceof ApiError) {
        setErrors(caught.fieldErrors);
        setApiError(caught);
      } else {
        setApiError(new ApiError(0, "Não foi possível concluir o cadastro."));
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (registered) {
    return (
      <main className="auth-page">
        <div className="card auth-card">
          <div className="auth-header">
            <span className="auth-icon"><i className="ti ti-circle-check" aria-hidden="true" /></span>
            <h1>Cadastro concluído</h1>
          </div>
          <SuccessAlert message="Sua conta foi criada. Entre com seu CPF e senha." />
          <Link className="button block" href="/entrar">Entrar</Link>
        </div>
      </main>
    );
  }


  return (
    <main className="auth-page">
      <div className="card auth-card wide">
        <div className="auth-header">
          <span className="auth-icon"><i className="ti ti-user-plus" aria-hidden="true" /></span>
          <h1>Criar conta</h1>
          <p className="hint">Campos marcados com * são obrigatórios.</p>
        </div>
        <ErrorAlert error={apiError} />
        <form className="numbered-sections" onSubmit={handleSubmit} noValidate>
          <fieldset>
            <legend>Identificação</legend>
            <div className="grid">
              <TextField
                name="cpf"
                label="CPF"
                icon="id"
                inputMode="numeric"
                autoComplete="off"
                placeholder="000.000.000-00"
                required
                value={credentials.cpf}
                error={errors.cpf}
                onChange={(event) => setCredential("cpf", event.target.value)}
              />
              <TextField
                name="email"
                label="E-mail"
                icon="mail"
                type="email"
                autoComplete="email"
                placeholder="Digite seu e-mail"
                maxLength={254}
                fieldClassName="span-2"
                required
                value={credentials.email}
                error={errors.email}
                onChange={(event) => setCredential("email", event.target.value)}
              />
            </div>
          </fieldset>

          <PersonalDataFields data={personalData} errors={errors} onChange={setPersonalData} />

          <fieldset>
            <legend>Senha de acesso</legend>
            <div className="grid">
              <TextField
                name="password"
                label="Senha"
                icon="lock"
                type="password"
                autoComplete="new-password"
                placeholder="Crie uma senha"
                required
                value={credentials.password}
                error={errors.password}
                onChange={(event) => setCredential("password", event.target.value)}
              />
              <TextField
                name="passwordConfirmation"
                label="Confirme a senha"
                icon="lock"
                type="password"
                autoComplete="new-password"
                placeholder="Repita a senha"
                required
                value={credentials.passwordConfirmation}
                error={errors.passwordConfirmation}
                onChange={(event) => setCredential("passwordConfirmation", event.target.value)}
              />
            </div>
          </fieldset>

          <button type="submit" className="block" disabled={submitting}>
            {submitting ? "Enviando..." : "Criar conta"}
          </button>
        </form>
        <p className="auth-back">
          Já tem conta? <Link href="/entrar">Entrar</Link>
        </p>
      </div>
    </main>
  );
}
