"use client";

import { type FormEvent, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import {
  type Adaptation,
  EMPTY_PERSONAL_DATA,
  type PersonalData,
  PersonalDataFields,
  toPersonalDataPayload,
  validatePersonalData,
} from "@/features/candidate/PersonalDataFields";
import { ApiError, apiGet, apiPut } from "@/lib/api";
import { maskCep, maskCpf, maskPhone } from "@/lib/validation";

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

export function ProfileForm({ onProfileLoaded }: { onProfileLoaded?: (profile: CandidateProfile) => void }) {
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [data, setData] = useState<PersonalData>(EMPTY_PERSONAL_DATA);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [apiError, setApiError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    apiGet<CandidateProfile>("/api/candidate/me")
      .then((loaded) => {
        setProfile(loaded);
        setData(toFormData(loaded));
        onProfileLoaded?.(loaded);
      })
      .catch((caught: unknown) => setApiError(caught instanceof ApiError ? caught : null));
  }, [onProfileLoaded]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setApiError(null);
    setMessage(null);
    const validationErrors = validatePersonalData(data);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) {
      return;
    }
    setSubmitting(true);
    try {
      const updated = await apiPut<CandidateProfile>("/api/candidate/me", toPersonalDataPayload(data));
      setProfile(updated);
      setData(toFormData(updated));
      onProfileLoaded?.(updated);
      setMessage("Dados atualizados. Inscrições já realizadas mantêm os dados informados na data da inscrição.");
    } catch (caught) {
      if (caught instanceof ApiError) {
        setErrors(caught.fieldErrors);
        setApiError(caught);
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (!profile) {
    return apiError ? <ErrorAlert error={apiError} /> : <p aria-live="polite">Carregando...</p>;
  }

  return (
    <section aria-labelledby="profile-title">
      <h2 id="profile-title">Meus Dados</h2>
      <p>
        CPF: <strong>{maskCpf(profile.cpf)}</strong>
        <br />
        E-mail: <strong>{profile.email}</strong>
      </p>
      <ErrorAlert error={apiError} />
      <SuccessAlert message={message} />
      <form onSubmit={handleSubmit} noValidate>
        <PersonalDataFields data={data} errors={errors} onChange={setData} />
        <div className="actions">
          <button type="submit" disabled={submitting}>
            {submitting ? "Salvando..." : "Salvar alterações"}
          </button>
        </div>
      </form>
    </section>
  );
}
