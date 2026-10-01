"use client";

import { type FormEvent, useEffect, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiGet, apiPost, apiUpload } from "@/lib/api";
import { formatDateTime } from "./types";

interface ProcessDocument {
  id: string;
  name: string;
  publishedAt: string;
  sizeBytes: number;
  withdrawnAt: string | null;
  withdrawalReason: string | null;
}

/** Lista pública dos Documentos do Processo. */
export function PublicProcessDocuments({ processId }: { processId: string }) {
  const [documents, setDocuments] = useState<ProcessDocument[]>([]);

  useEffect(() => {
    apiGet<ProcessDocument[]>(`/api/processes/${encodeURIComponent(processId)}/documents`)
      .then(setDocuments)
      .catch(() => setDocuments([]));
  }, [processId]);

  return (
    <section>
      <h2>Documentos do Processo</h2>
      {documents.length === 0 ? (
        <p className="hint">Nenhum documento publicado.</p>
      ) : (
        <ul>
          {documents.map((document) => (
            <li key={document.id}>
              <a href={`/api/processes/${processId}/documents/${document.id}/file`}>{document.name}</a>{" "}
              <span className="hint">· publicado em {formatDateTime(document.publishedAt)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/** Documentos do Processo na área administrativa: publicação com nome e retirada com justificativa. */
export function AdminProcessDocuments({ processId, canPublish }: { processId: string; canPublish: boolean }) {
  const [documents, setDocuments] = useState<ProcessDocument[]>([]);
  const [version, setVersion] = useState(0);
  const [name, setName] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [sending, setSending] = useState(false);
  const base = `/api/admin/processes/${encodeURIComponent(processId)}/documents`;

  useEffect(() => {
    apiGet<ProcessDocument[]>(base).then(setDocuments).catch(() => setDocuments([]));
  }, [base, version]);

  async function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) return;
    setError(null);
    setSending(true);
    const form = event.currentTarget;
    try {
      const data = new FormData();
      data.append("name", name.trim());
      data.append("file", file);
      await apiUpload(base, data);
      setName("");
      setFile(null);
      form.reset();
      setVersion((current) => current + 1);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível publicar o documento."));
    } finally {
      setSending(false);
    }
  }

  async function withdraw(documentId: string) {
    const reason = window.prompt("Justificativa da retirada do documento da página pública:");
    if (!reason?.trim()) return;
    setError(null);
    try {
      await apiPost(`${base}/${documentId}/withdraw`, { reason: reason.trim() });
      setVersion((current) => current + 1);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível retirar o documento."));
    }
  }

  return (
    <section>
      <h2>Documentos do Processo</h2>
      <ErrorAlert error={error} />
      {documents.length === 0 ? (
        <p className="hint">Nenhum documento publicado.</p>
      ) : (
        <ul>
          {documents.map((document) => (
            <li key={document.id}>
              <a href={`${base}/${document.id}/file`}>{document.name}</a>{" "}
              <span className="hint">· {formatDateTime(document.publishedAt)}</span>
              {document.withdrawnAt ? (
                <span className="hint">
                  {" "}· retirado em {formatDateTime(document.withdrawnAt)} ({document.withdrawalReason})
                </span>
              ) : (
                canPublish && (
                  <>
                    {" "}
                    <button type="button" className="secondary" onClick={() => withdraw(document.id)}>
                      Retirar
                    </button>
                  </>
                )
              )}
            </li>
          ))}
        </ul>
      )}
      {canPublish && (
        <form onSubmit={publish} noValidate>
          <TextField name="documentName" label="Nome do documento" maxLength={200} value={name}
            placeholder="Ex.: Relação de inscrições deferidas e indeferidas"
            onChange={(event) => setName(event.target.value)} />
          <div className="field">
            <label htmlFor="documentFile">Arquivo PDF (até 10 MB)</label>
            <input id="documentFile" type="file" accept="application/pdf,.pdf"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
          </div>
          <button type="submit" disabled={sending || !file || !name.trim()}>
            {sending ? "Publicando..." : "Publicar documento"}
          </button>
        </form>
      )}
    </section>
  );
}
