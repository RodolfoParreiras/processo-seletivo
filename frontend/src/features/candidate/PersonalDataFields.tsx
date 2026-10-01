"use client";

import { type ChangeEvent, type InputHTMLAttributes, type ReactNode, useState } from "react";
import { ADAPTATIONS, maskCep, maskPhone, onlyDigits, UFS } from "@/lib/validation";

export type Adaptation = (typeof ADAPTATIONS)[number]["value"];

/** Campos do cadastro que o candidato também pode alterar em "Meus Dados". */
export interface PersonalData {
  fullName: string;
  birthDate: string;
  motherName: string;
  phone: string;
  cep: string;
  street: string;
  addressNumber: string;
  complement: string;
  neighborhood: string;
  city: string;
  uf: string;
  hasDisability: "" | "SIM" | "NAO";
  adaptations: Adaptation[];
}

export const EMPTY_PERSONAL_DATA: PersonalData = {
  fullName: "",
  birthDate: "",
  motherName: "",
  phone: "",
  cep: "",
  street: "",
  addressNumber: "",
  complement: "",
  neighborhood: "",
  city: "",
  uf: "",
  hasDisability: "",
  adaptations: [],
};

const MASKS: Partial<Record<keyof PersonalData, (value: string) => string>> = {
  phone: maskPhone,
  cep: maskCep,
};

export function validatePersonalData(data: PersonalData): Record<string, string> {
  const errors: Record<string, string> = {};
  const required: [keyof PersonalData, string][] = [
    ["fullName", "Informe o nome completo."],
    ["birthDate", "Informe a data de nascimento."],
    ["motherName", "Informe o nome da mãe."],
    ["street", "Informe o endereço."],
    ["addressNumber", "Informe o número."],
    ["neighborhood", "Informe o bairro."],
    ["city", "Informe a cidade."],
    ["uf", "Informe a UF."],
    ["hasDisability", "Informe se é pessoa com deficiência."],
  ];
  for (const [field, message] of required) {
    if (!String(data[field]).trim()) errors[field] = message;
  }
  const phoneLength = onlyDigits(data.phone).length;
  if (phoneLength < 10 || phoneLength > 11) errors.phone = "Telefone inválido.";
  if (onlyDigits(data.cep).length !== 8) errors.cep = "CEP inválido.";
  if (data.birthDate && new Date(data.birthDate) >= new Date()) errors.birthDate = "Data de nascimento inválida.";
  if (data.hasDisability === "SIM" && data.adaptations.length === 0) {
    errors.adaptations = "Informe a necessidade de adaptações.";
  }
  return errors;
}


/** Corpo esperado pela API (valores numéricos sem máscara). */
export function toPersonalDataPayload(data: PersonalData) {
  return {
    fullName: data.fullName.trim(),
    birthDate: data.birthDate,
    motherName: data.motherName.trim(),
    phone: onlyDigits(data.phone),
    cep: onlyDigits(data.cep),
    street: data.street.trim(),
    addressNumber: data.addressNumber.trim(),
    complement: data.complement.trim() || null,
    neighborhood: data.neighborhood.trim(),
    city: data.city.trim(),
    uf: data.uf,
    hasDisability: data.hasDisability === "SIM",
    adaptations: data.hasDisability === "SIM" ? data.adaptations : [],
  };
}

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  name: string;
  label: string;
  value: string;
  error?: string;
  /** Nome de um ícone Tabler (sem o prefixo "ti-") exibido dentro do campo. */
  icon?: string;
  /** Conteúdo exibido à direita do rótulo, como o link "Esqueci minha senha". */
  labelAside?: ReactNode;
}

/** Marca visual de campo obrigatório; leitores de tela usam o atributo required do campo. */
export function RequiredMark() {
  return <span className="required-mark" aria-hidden="true"> *</span>;
}

export function TextField({ name, label, value, error, icon, labelAside, type, required, ...props }: FieldProps) {
  const [visible, setVisible] = useState(false);
  const isPassword = type === "password";
  const labelElement = (
    <label htmlFor={name}>
      {label}
      {required && <RequiredMark />}
    </label>
  );
  const wrapperClass = ["input-wrap", icon && "with-icon", isPassword && "with-toggle"].filter(Boolean).join(" ");

  return (
    <div className="field">
      {labelAside ? <div className="label-row">{labelElement}{labelAside}</div> : labelElement}
      <div className={wrapperClass}>
        {icon && <i className={`ti ti-${icon}`} aria-hidden="true" />}
        <input
          id={name}
          name={name}
          value={value}
          type={isPassword && visible ? "text" : type}
          required={required}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${name}-error` : undefined}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            className="input-toggle"
            aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
            aria-pressed={visible}
            onClick={() => setVisible((current) => !current)}
          >
            <i className={visible ? "ti ti-eye-off" : "ti ti-eye"} aria-hidden="true" />
          </button>
        )}
      </div>
      {error && (
        <p id={`${name}-error`} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}

interface PersonalDataFieldsProps {
  data: PersonalData;
  errors: Record<string, string>;
  onChange: (data: PersonalData) => void;
}

export function PersonalDataFields({ data, errors, onChange }: PersonalDataFieldsProps) {
  function handleChange(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const field = event.target.name as keyof PersonalData;
    const mask = MASKS[field];
    const value = mask ? mask(event.target.value) : event.target.value;
    onChange({
      ...data,
      [field]: value,
      ...(field === "hasDisability" && value !== "SIM" ? { adaptations: [] } : {}),
    });
  }

  function toggleAdaptation(adaptation: Adaptation) {
    if (adaptation === "NONE") {
      onChange({ ...data, adaptations: data.adaptations.includes("NONE") ? [] : ["NONE"] });
      return;
    }
    const withoutNone = data.adaptations.filter((item) => item !== "NONE");
    onChange({
      ...data,
      adaptations: withoutNone.includes(adaptation)
        ? withoutNone.filter((item) => item !== adaptation)
        : [...withoutNone, adaptation],
    });
  }

  const text = (
    name: keyof PersonalData,
    label: string,
    props: Omit<InputHTMLAttributes<HTMLInputElement>, "name" | "value"> = {},
  ) => (
    <TextField
      name={name}
      label={label}
      value={String(data[name])}
      error={errors[name]}
      onChange={handleChange}
      required
      {...props}
    />
  );

  return (
    <>
      <fieldset>
        <legend>Dados pessoais</legend>
        {text("fullName", "Nome completo", { autoComplete: "name", maxLength: 150, placeholder: "Digite seu nome completo" })}
        <div className="grid">
          {text("birthDate", "Data de nascimento", { type: "date", autoComplete: "bday" })}
          {text("phone", "Telefone", { inputMode: "tel", autoComplete: "tel", placeholder: "(00) 00000-0000" })}
        </div>
        {text("motherName", "Nome da mãe", { maxLength: 150, placeholder: "Digite o nome da mãe" })}
      </fieldset>

      <fieldset>
        <legend>Endereço</legend>
        <div className="grid">
          {text("cep", "CEP", { inputMode: "numeric", autoComplete: "postal-code", placeholder: "00000-000" })}
          {text("addressNumber", "Número", { maxLength: 10 })}
        </div>
        {text("street", "Endereço", { autoComplete: "address-line1", maxLength: 150 })}
        {text("complement", "Complemento", { autoComplete: "address-line2", maxLength: 60, required: false })}
        <div className="grid">
          {text("neighborhood", "Bairro", { maxLength: 80 })}
          {text("city", "Cidade", { autoComplete: "address-level2", maxLength: 80 })}
        </div>
        <div className="field">
          <label htmlFor="uf">UF<RequiredMark /></label>
          <select id="uf" name="uf" value={data.uf} onChange={handleChange} required aria-invalid={Boolean(errors.uf)}>
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
        <legend>Pessoa com deficiência?<RequiredMark /></legend>
        <div className="options choice" role="radiogroup" aria-required="true">
          <label>
            <input
              type="radio"
              name="hasDisability"
              value="SIM"
              checked={data.hasDisability === "SIM"}
              onChange={handleChange}
            />{" "}
            Sim
          </label>
          <label>
            <input
              type="radio"
              name="hasDisability"
              value="NAO"
              checked={data.hasDisability === "NAO"}
              onChange={handleChange}
            />{" "}
            Não
          </label>
        </div>
        {errors.hasDisability && <p className="field-error">{errors.hasDisability}</p>}

        {data.hasDisability === "SIM" && (
          <fieldset>
            <legend>Necessidade de adaptações<RequiredMark /></legend>
            <div className="options">
              {ADAPTATIONS.map((adaptation) => (
                <label key={adaptation.value}>
                  <input
                    type="checkbox"
                    checked={data.adaptations.includes(adaptation.value)}
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
    </>
  );
}
