"use client";

import { type FormEvent, useEffect, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { formatDateTime, localInputToIso, type ProcessDetail, STATUS_LABELS, type ProcessStatus } from "@/features/process/types";
import { ApiError, apiDelete, apiGet, apiPost, apiUpload } from "@/lib/api";

interface EditorProps {
  process: ProcessDetail;
  onChanged: () => void;
}

const toApiError = (caught: unknown, fallback: string) =>
  caught instanceof ApiError ? caught : new ApiError(0, fallback);

export function PositionsEditor({ process, onChanged }: EditorProps) {
  const [name, setName] = useState("");
  const [vacancies, setVacancies] = useState("1");
  const [error, setError] = useState<ApiError | null>(null);
  const base = `/api/admin/processes/${process.id}/positions`;

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await apiPost(base, { name: name.trim(), vacancies: Number(vacancies) });
      setName("");
      setVacancies("1");
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível adicionar o cargo."));
    }
  }

  async function remove(positionId: string) {
    setError(null);
    try {
      await apiDelete(`${base}/${positionId}`);
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível remover o cargo."));
    }
  }

  return (
    <section>
      <h2>Cargos e vagas</h2>
      <ErrorAlert error={error} />
      <ul>
        {process.positions.map((position) => (
          <li key={position.id}>
            {position.name} — {position.vacancies} vaga(s){" "}
            <button type="button" className="secondary" onClick={() => remove(position.id)}>
              Remover
            </button>
          </li>
        ))}
      </ul>
      <form onSubmit={add} noValidate>
        <div className="grid">
          <TextField name="positionName" label="Cargo" maxLength={150} value={name}
            onChange={(event) => setName(event.target.value)} />
          <TextField name="positionVacancies" label="Vagas" type="number" min={1} value={vacancies}
            onChange={(event) => setVacancies(event.target.value)} />
        </div>
        <button type="submit" disabled={!name.trim()}>Adicionar cargo</button>
      </form>
    </section>
  );
}

export function RequirementsEditor({ process, onChanged }: EditorProps) {
  const [form, setForm] = useState({ name: "", description: "", mandatory: true, title: false });
  const [error, setError] = useState<ApiError | null>(null);
  const base = `/api/admin/processes/${process.id}/document-requirements`;

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await apiPost(base, { ...form, name: form.name.trim(), description: form.description.trim() || null });
      setForm({ name: "", description: "", mandatory: true, title: false });
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível adicionar o documento."));
    }
  }

  async function remove(requirementId: string) {
    setError(null);
    try {
      await apiDelete(`${base}/${requirementId}`);
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível remover o documento."));
    }
  }

  return (
    <section>
      <h2>Documentos exigidos</h2>
      <ErrorAlert error={error} />
      <ul>
        {process.documentRequirements.map((requirement) => (
          <li key={requirement.id}>
            {requirement.name} — {requirement.mandatory ? "obrigatório" : "opcional"}
            {requirement.title && " (título)"}{" "}
            <button type="button" className="secondary" onClick={() => remove(requirement.id)}>
              Remover
            </button>
          </li>
        ))}
      </ul>
      <form onSubmit={add} noValidate>
        <TextField name="requirementName" label="Documento" maxLength={150} value={form.name}
          onChange={(event) => setForm({ ...form, name: event.target.value })} />
        <TextField name="requirementDescription" label="Descrição (opcional)" maxLength={500} value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })} />
        <div className="options">
          <label>
            <input type="checkbox" checked={form.mandatory}
              onChange={(event) => setForm({ ...form, mandatory: event.target.checked })} />{" "}
            Obrigatório
          </label>
          <label>
            <input type="checkbox" checked={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.checked })} />{" "}
            É título (avaliação de títulos)
          </label>
        </div>
        <button type="submit" disabled={!form.name.trim()}>Adicionar documento</button>
      </form>
    </section>
  );
}

export function NoticeUpload({ process, onChanged }: EditorProps) {
  const [file, setFile] = useState<File | null>(null);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isDraft = process.status === "RASCUNHO";

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    setError(null);
    setSubmitting(true);
    try {
      const form = new FormData();
      form.append("file", file);
      if (!isDraft) form.append("reason", reason.trim());
      await apiUpload(`/api/admin/processes/${process.id}/notices`, form);
      setFile(null);
      setReason("");
      (event.target as HTMLFormElement).reset();
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível enviar o edital."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h2>{isDraft ? "Edital" : "Retificar edital"}</h2>
      <p className="hint">
        {isDraft
          ? "Arquivo PDF de até 10 MB. Enquanto rascunho, um novo envio substitui o anterior."
          : "Cria uma nova versão do edital. A versão anterior continua disponível no histórico."}
      </p>
      <ErrorAlert error={error} />
      <form onSubmit={upload} noValidate>
        <div className="field">
          <label htmlFor="noticeFile">Arquivo PDF</label>
          <input id="noticeFile" type="file" accept="application/pdf,.pdf"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        </div>
        {!isDraft && (
          <div className="field">
            <label htmlFor="noticeReason">Motivo da retificação</label>
            <textarea id="noticeReason" maxLength={1000} rows={3} value={reason}
              onChange={(event) => setReason(event.target.value)} />
          </div>
        )}
        <button type="submit" disabled={submitting || !file || (!isDraft && !reason.trim())}>
          {submitting ? "Enviando..." : "Enviar edital"}
        </button>
      </form>
    </section>
  );
}

interface ReasonActionProps {
  label: string;
  description: string;
  endpoint: string;
  onDone: () => void;
}

/** Ação de mudança de situação que exige motivo registrado. */
export function ReasonAction({ label, description, endpoint, onDone }: ReasonActionProps) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiError | null>(null);

  async function confirm() {
    setError(null);
    try {
      await apiPost(endpoint, { reason: reason.trim() });
      setOpen(false);
      setReason("");
      onDone();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível concluir a operação."));
    }
  }

  if (!open) {
    return (
      <button type="button" className="secondary" onClick={() => setOpen(true)}>
        {label}
      </button>
    );
  }
  return (
    <div className="card">
      <p>{description}</p>
      <ErrorAlert error={error} />
      <div className="field">
        <label htmlFor={`reason-${label}`}>Motivo</label>
        <textarea id={`reason-${label}`} maxLength={1000} rows={3} value={reason}
          onChange={(event) => setReason(event.target.value)} />
      </div>
      <div className="actions">
        <button type="button" disabled={!reason.trim()} onClick={confirm}>
          Confirmar: {label}
        </button>
        <button type="button" className="secondary" onClick={() => setOpen(false)}>
          Cancelar
        </button>
      </div>
    </div>
  );
}

export function ExtendRegistration({ process, onChanged }: EditorProps) {
  const [newEnd, setNewEnd] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<ApiError | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    try {
      await apiPost(`/api/admin/processes/${process.id}/extend-registration`, {
        newRegistrationEnd: localInputToIso(newEnd),
        reason: reason.trim(),
      });
      setNewEnd("");
      setReason("");
      onChanged();
    } catch (caught) {
      setError(toApiError(caught, "Não foi possível prorrogar."));
    }
  }

  return (
    <section>
      <h2>Prorrogar inscrições</h2>
      <p className="hint">Fim atual: {formatDateTime(process.registrationEnd)}. Só é possível adiar a data final.</p>
      <ErrorAlert error={error} />
      <form onSubmit={submit} noValidate>
        <TextField name="newRegistrationEnd" label="Nova data final" type="datetime-local" value={newEnd}
          onChange={(event) => setNewEnd(event.target.value)} />
        <div className="field">
          <label htmlFor="extendReason">Motivo</label>
          <textarea id="extendReason" maxLength={1000} rows={2} value={reason}
            onChange={(event) => setReason(event.target.value)} />
        </div>
        <button type="submit" disabled={!newEnd || !reason.trim()}>Prorrogar</button>
      </form>
    </section>
  );
}

interface HistoryEntry {
  fromStatus: ProcessStatus | null;
  toStatus: ProcessStatus;
  reason: string | null;
  changedBy: string | null;
  changedAt: string;
}

export function StatusHistory({ processId, version }: { processId: string; version: number }) {
  const [entries, setEntries] = useState<HistoryEntry[]>([]);

  useEffect(() => {
    apiGet<HistoryEntry[]>(`/api/admin/processes/${processId}/history`).then(setEntries).catch(() => setEntries([]));
  }, [processId, version]);

  return (
    <section>
      <h2>Histórico de situações</h2>
      <table>
        <thead>
          <tr>
            <th scope="col">Data</th>
            <th scope="col">Situação</th>
            <th scope="col">Responsável</th>
            <th scope="col">Motivo</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry, index) => (
            <tr key={index}>
              <td>{formatDateTime(entry.changedAt)}</td>
              <td>{STATUS_LABELS[entry.toStatus]}</td>
              <td>{entry.changedBy ?? "Automático (datas)"}</td>
              <td>{entry.reason ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
