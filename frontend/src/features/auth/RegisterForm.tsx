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
import { isValidCpf, maskCpf, onlyDigits, passwordProblems } from "@/lib/validation";

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
  if (passwordProblems(credentials.password, personalData.fullName, personalData.birthDate).length > 0) {
    errors.password = "A senha não atende aos requisitos.";
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
      <main>
        <div className="card">
          <h1>Cadastro concluído</h1>
          <SuccessAlert message="Sua conta foi criada. Entre com seu CPF e senha." />
          <Link className="button" href="/entrar">Entrar</Link>
        </div>
      </main>
    );
  }

  const passwordHints = credentials.password
    ? passwordProblems(credentials.password, personalData.fullName, personalData.birthDate)
    : [];

  return (
    <main>
      <div className="card">
        <h1>Criar conta de candidato</h1>
        <p className="hint">Todos os campos são obrigatórios, exceto o complemento.</p>
        <ErrorAlert error={apiError} />
        <form onSubmit={handleSubmit} noValidate>
          <fieldset>
            <legend>Identificação</legend>
            <TextField
              name="cpf"
              label="CPF"
              inputMode="numeric"
              autoComplete="off"
              value={credentials.cpf}
              error={errors.cpf}
              onChange={(event) => setCredential("cpf", event.target.value)}
            />
            <TextField
              name="email"
              label="E-mail"
              type="email"
              autoComplete="email"
              maxLength={254}
              value={credentials.email}
              error={errors.email}
              onChange={(event) => setCredential("email", event.target.value)}
            />
          </fieldset>

          <PersonalDataFields data={personalData} errors={errors} onChange={setPersonalData} />

          <fieldset>
            <legend>Senha de acesso</legend>
            <TextField
              name="password"
              label="Senha"
              type="password"
              autoComplete="new-password"
              value={credentials.password}
              error={errors.password}
              onChange={(event) => setCredential("password", event.target.value)}
            />
            <p className="hint">
              Mínimo de 8 caracteres, com letra maiúscula, minúscula, número e caractere especial. Não use seu
              nome, sobrenome ou data de nascimento.
            </p>
            {passwordHints.length > 0 && <p className="field-error">Falta: {passwordHints.join(" ")}</p>}
            <TextField
              name="passwordConfirmation"
              label="Confirme a senha"
              type="password"
              autoComplete="new-password"
              value={credentials.passwordConfirmation}
              error={errors.passwordConfirmation}
              onChange={(event) => setCredential("passwordConfirmation", event.target.value)}
            />
          </fieldset>

          <div className="actions">
            <button type="submit" disabled={submitting}>
              {submitting ? "Enviando..." : "Criar conta"}
            </button>
            <Link href="/entrar">Já tenho conta</Link>
          </div>
        </form>
      </div>
    </main>
  );
}
