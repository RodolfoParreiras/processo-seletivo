"use client";

import Link from "next/link";
import { type ChangeEvent, type FormEvent, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { ApiError, apiPost } from "@/lib/api";
import {
  ADAPTATIONS,
  isValidCpf,
  maskCep,
  maskCpf,
  maskPhone,
  onlyDigits,
  passwordProblems,
  UFS,
} from "@/lib/validation";

type Adaptation = (typeof ADAPTATIONS)[number]["value"];

interface FormState {
  cpf: string;
  fullName: string;
  birthDate: string;
  motherName: string;
  email: string;
  phone: string;
  cep: string;
  street: string;
  addressNumber: string;
  complement: string;
  neighborhood: string;
  city: string;
  uf: string;
  password: string;
  passwordConfirmation: string;
  hasDisability: "" | "SIM" | "NAO";
  adaptations: Adaptation[];
}

const INITIAL_STATE: FormState = {
  cpf: "",
  fullName: "",
  birthDate: "",
  motherName: "",
  email: "",
  phone: "",
  cep: "",
  street: "",
  addressNumber: "",
  complement: "",
  neighborhood: "",
  city: "",
  uf: "",
  password: "",
  passwordConfirmation: "",
  hasDisability: "",
  adaptations: [],
};

const MASKS: Partial<Record<keyof FormState, (value: string) => string>> = {
  cpf: maskCpf,
  phone: maskPhone,
  cep: maskCep,
};

function validate(form: FormState): Record<string, string> {
  const errors: Record<string, string> = {};
  const required: [keyof FormState, string][] = [
    ["fullName", "Informe o nome completo."],
    ["birthDate", "Informe a data de nascimento."],
    ["motherName", "Informe o nome da mãe."],
    ["email", "Informe o e-mail."],
    ["street", "Informe o endereço."],
    ["addressNumber", "Informe o número."],
    ["neighborhood", "Informe o bairro."],
    ["city", "Informe a cidade."],
    ["uf", "Informe a UF."],
    ["hasDisability", "Informe se é pessoa com deficiência."],
  ];
  for (const [field, message] of required) {
    if (!String(form[field]).trim()) errors[field] = message;
  }
  if (!isValidCpf(form.cpf)) errors.cpf = "CPF inválido.";
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errors.email = "E-mail inválido.";
  const phoneLength = onlyDigits(form.phone).length;
  if (phoneLength < 10 || phoneLength > 11) errors.phone = "Telefone inválido.";
  if (onlyDigits(form.cep).length !== 8) errors.cep = "CEP inválido.";
  if (form.birthDate && new Date(form.birthDate) >= new Date()) errors.birthDate = "Data de nascimento inválida.";
  if (passwordProblems(form.password, form.fullName, form.birthDate).length > 0) {
    errors.password = "A senha não atende aos requisitos.";
  }
  if (form.passwordConfirmation !== form.password) errors.passwordConfirmation = "As senhas não conferem.";
  if (form.hasDisability === "SIM" && form.adaptations.length === 0) {
    errors.adaptations = "Informe a necessidade de adaptações.";
  }
  return errors;
}

export function RegisterForm() {
  const [form, setForm] = useState<FormState>(INITIAL_STATE);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [registered, setRegistered] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const field = event.target.name as keyof FormState;
    const mask = MASKS[field];
    const value = mask ? mask(event.target.value) : event.target.value;
    setForm((current) => ({
      ...current,
      [field]: value,
      ...(field === "hasDisability" && value !== "SIM" ? { adaptations: [] } : {}),
    }));
  }

  function toggleAdaptation(adaptation: Adaptation) {
    setForm((current) => {
      if (adaptation === "NONE") {
        return { ...current, adaptations: current.adaptations.includes("NONE") ? [] : ["NONE"] };
      }
      const withoutNone = current.adaptations.filter((item) => item !== "NONE");
      return {
        ...current,
        adaptations: withoutNone.includes(adaptation)
          ? withoutNone.filter((item) => item !== adaptation)
          : [...withoutNone, adaptation],
      };
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setApiError(null);
    const validationErrors = validate(form);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }
    setSubmitting(true);
    try {
      await apiPost("/api/auth/register", {
        cpf: onlyDigits(form.cpf),
        fullName: form.fullName.trim(),
        birthDate: form.birthDate,
        motherName: form.motherName.trim(),
        email: form.email.trim(),
        phone: onlyDigits(form.phone),
        cep: onlyDigits(form.cep),
        street: form.street.trim(),
        addressNumber: form.addressNumber.trim(),
        complement: form.complement.trim() || null,
        neighborhood: form.neighborhood.trim(),
        city: form.city.trim(),
        uf: form.uf,
        password: form.password,
        hasDisability: form.hasDisability === "SIM",
        adaptations: form.hasDisability === "SIM" ? form.adaptations : [],
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

  const passwordHints = form.password ? passwordProblems(form.password, form.fullName, form.birthDate) : [];

  const input = (name: keyof FormState, label: string, props: Record<string, unknown> = {}) => (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        name={name}
        value={String(form[name])}
        onChange={handleChange}
        aria-invalid={Boolean(errors[name])}
        aria-describedby={errors[name] ? `${name}-error` : undefined}
        {...props}
      />
      {errors[name] && (
        <p id={`${name}-error`} className="field-error">
          {errors[name]}
        </p>
      )}
    </div>
  );

  return (
    <main>
      <div className="card">
        <h1>Criar conta de candidato</h1>
        <p className="hint">Todos os campos são obrigatórios, exceto o complemento.</p>
        <ErrorAlert error={apiError} />
        <form onSubmit={handleSubmit} noValidate>
          <fieldset>
            <legend>Dados pessoais</legend>
            {input("cpf", "CPF", { inputMode: "numeric", autoComplete: "off" })}
            {input("fullName", "Nome completo", { autoComplete: "name", maxLength: 150 })}
            <div className="grid">
              {input("birthDate", "Data de nascimento", { type: "date", autoComplete: "bday" })}
              {input("phone", "Telefone", { inputMode: "tel", autoComplete: "tel" })}
            </div>
            {input("motherName", "Nome da mãe", { maxLength: 150 })}
            {input("email", "E-mail", { type: "email", autoComplete: "email", maxLength: 254 })}
          </fieldset>

          <fieldset>
            <legend>Endereço</legend>
            <div className="grid">
              {input("cep", "CEP", { inputMode: "numeric", autoComplete: "postal-code" })}
              {input("addressNumber", "Número", { maxLength: 10 })}
            </div>
            {input("street", "Endereço", { autoComplete: "address-line1", maxLength: 150 })}
            {input("complement", "Complemento (opcional)", { autoComplete: "address-line2", maxLength: 60 })}
            <div className="grid">
              {input("neighborhood", "Bairro", { maxLength: 80 })}
              {input("city", "Cidade", { autoComplete: "address-level2", maxLength: 80 })}
            </div>
            <div className="field">
              <label htmlFor="uf">UF</label>
              <select id="uf" name="uf" value={form.uf} onChange={handleChange} aria-invalid={Boolean(errors.uf)}>
                <option value="">Selecione</option>
                {UFS.map((uf) => (
                  <option key={uf} value={uf}>
                    {uf}
                  </option>
                ))}
              </select>
              {errors.uf && <p className="field-error">{errors.uf}</p>}
            </div>
          </fieldset>

          <fieldset>
            <legend>Pessoa com deficiência?</legend>
            <div className="options" role="radiogroup" aria-invalid={Boolean(errors.hasDisability)}>
              <label>
                <input
                  type="radio"
                  name="hasDisability"
                  value="SIM"
                  checked={form.hasDisability === "SIM"}
                  onChange={handleChange}
                />{" "}
                Sim
              </label>
              <label>
                <input
                  type="radio"
                  name="hasDisability"
                  value="NAO"
                  checked={form.hasDisability === "NAO"}
                  onChange={handleChange}
                />{" "}
                Não
              </label>
            </div>
            {errors.hasDisability && <p className="field-error">{errors.hasDisability}</p>}

            {form.hasDisability === "SIM" && (
              <fieldset>
                <legend>Necessidade de adaptações</legend>
                <div className="options">
                  {ADAPTATIONS.map((adaptation) => (
                    <label key={adaptation.value}>
                      <input
                        type="checkbox"
                        checked={form.adaptations.includes(adaptation.value)}
                        onChange={() => toggleAdaptation(adaptation.value)}
                      />{" "}
                      {adaptation.label}
                    </label>
                  ))}
                </div>
                {errors.adaptations && <p className="field-error">{errors.adaptations}</p>}
              </fieldset>
            )}
          </fieldset>

          <fieldset>
            <legend>Senha de acesso</legend>
            {input("password", "Senha", { type: "password", autoComplete: "new-password" })}
            <p className="hint">
              Mínimo de 8 caracteres, com letra maiúscula, minúscula, número e caractere especial. Não use seu
              nome, sobrenome ou data de nascimento.
            </p>
            {passwordHints.length > 0 && <p className="field-error">Falta: {passwordHints.join(" ")}</p>}
            {input("passwordConfirmation", "Confirme a senha", { type: "password", autoComplete: "new-password" })}
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
