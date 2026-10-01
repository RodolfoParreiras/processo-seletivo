"use client";

import { type FormEvent, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { isoToLocalInput, localInputToIso, type ProcessDetail } from "@/features/process/types";
import { ApiError } from "@/lib/api";

export interface ProcessDetailsPayload {
  number: string;
  year: number;
  title: string;
  department: string;
  registrationStart: string;
  registrationEnd: string;
  multipleApplicationsAllowed: boolean;
  titleEvaluationEnabled: boolean;
}

interface ProcessDetailsFormProps {
  initial?: ProcessDetail;
  submitLabel: string;
  onSubmit: (payload: ProcessDetailsPayload) => Promise<void>;
}

export function ProcessDetailsForm({ initial, submitLabel, onSubmit }: ProcessDetailsFormProps) {
  const [form, setForm] = useState({
    number: initial?.number ?? "",
    year: String(initial?.year ?? new Date().getFullYear()),
    title: initial?.title ?? "",
    department: initial?.department ?? "",
    registrationStart: initial ? isoToLocalInput(initial.registrationStart) : "",
    registrationEnd: initial ? isoToLocalInput(initial.registrationEnd) : "",
    multipleApplicationsAllowed: initial?.multipleApplicationsAllowed ?? false,
    titleEvaluationEnabled: initial?.titleEvaluationEnabled ?? false,
  });
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof typeof form, value: string | boolean) => setForm((current) => ({ ...current, [field]: value }));

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    if (!form.registrationStart || !form.registrationEnd) {
      setError(new ApiError(400, "Informe o início e o fim das inscrições."));
      return;
    }
    setSubmitting(true);
    try {
      await onSubmit({
        number: form.number.trim(),
        year: Number(form.year),
        title: form.title.trim(),
        department: form.department.trim(),
        registrationStart: localInputToIso(form.registrationStart),
        registrationEnd: localInputToIso(form.registrationEnd),
        multipleApplicationsAllowed: form.multipleApplicationsAllowed,
        titleEvaluationEnabled: form.titleEvaluationEnabled,
      });
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível salvar."));
    } finally {
      setSubmitting(false);
    }
  }

  const fieldErrors = error?.fieldErrors ?? {};

  return (
    <form onSubmit={handleSubmit} noValidate>
      <ErrorAlert error={error} />
      <div className="grid">
        <TextField name="number" label="Número" maxLength={20} value={form.number} error={fieldErrors.number}
          onChange={(event) => set("number", event.target.value)} />
        <TextField name="year" label="Ano" type="number" value={form.year} error={fieldErrors.year}
          onChange={(event) => set("year", event.target.value)} />
      </div>
      <TextField name="title" label="Título" maxLength={200} value={form.title} error={fieldErrors.title}
        onChange={(event) => set("title", event.target.value)} />
      <TextField name="department" label="Secretaria responsável" maxLength={150} value={form.department}
        error={fieldErrors.department} onChange={(event) => set("department", event.target.value)} />
      <div className="grid">
        <TextField name="registrationStart" label="Início das inscrições" type="datetime-local"
          value={form.registrationStart} error={fieldErrors.registrationStart}
          onChange={(event) => set("registrationStart", event.target.value)} />
        <TextField name="registrationEnd" label="Fim das inscrições" type="datetime-local"
          value={form.registrationEnd} error={fieldErrors.registrationEnd}
          onChange={(event) => set("registrationEnd", event.target.value)} />
      </div>
      <div className="options">
        <label>
          <input type="checkbox" checked={form.multipleApplicationsAllowed}
            onChange={(event) => set("multipleApplicationsAllowed", event.target.checked)} />{" "}
          Permitir mais de uma inscrição por candidato (uma por cargo)
        </label>
        <label>
          <input type="checkbox" checked={form.titleEvaluationEnabled}
            onChange={(event) => set("titleEvaluationEnabled", event.target.checked)} />{" "}
          Haverá avaliação de títulos
        </label>
      </div>
      <div className="actions">
        <button type="submit" disabled={submitting}>
          {submitting ? "Salvando..." : submitLabel}
        </button>
      </div>
    </form>
  );
}
