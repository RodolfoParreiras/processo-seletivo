"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import {
  APPLICATION_STATUS_LABELS,
  type ApplicationDetail,
  DOCUMENT_STATUS_LABELS,
} from "@/features/application/types";
import { SessionGate } from "@/features/auth/SessionGate";
import type { CandidateProfile } from "@/features/candidate/ProfileForm";
import { formatDateTime } from "@/features/process/types";
import { ApiError, apiDelete, apiGet, apiPost, apiUpload } from "@/lib/api";
import { maskCep, maskCpf, maskPhone } from "@/lib/validation";

const MAX_FILE_BYTES = 2 * 1024 * 1024;

function DocumentUpload({ applicationId, requirementId, onUploaded }: {
  applicationId: string;
  requirementId: string;
  onUploaded: () => void;
}) {
  const [error, setError] = useState<ApiError | null>(null);
  const [sending, setSending] = useState(false);

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const input = event.currentTarget.elements.namedItem("file") as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > MAX_FILE_BYTES) {
      setError(new ApiError(400, "Arquivo acima de 2 MB."));
      return;
    }
    setError(null);
    setSending(true);
    try {
      const form = new FormData();
      form.append("file", file);
      await apiUpload(`/api/candidate/applications/${applicationId}/documents?requirementId=${requirementId}`, form);
      event.currentTarget?.reset();
      onUploaded();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível enviar o arquivo."));
    } finally {
      setSending(false);
    }
  }

  return (
    <form onSubmit={upload} className="actions">
      <input name="file" type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
        aria-label="Arquivo (PDF, JPG ou PNG, até 2 MB)" />
      <button type="submit" disabled={sending}>{sending ? "Enviando..." : "Enviar arquivo"}</button>
      <ErrorAlert error={error} />
    </form>
  );
}

function PersonalDataReview() {
  const [profile, setProfile] = useState<CandidateProfile | null>(null);

  useEffect(() => {
    apiGet<CandidateProfile>("/api/candidate/me").then(setProfile).catch(() => setProfile(null));
  }, []);

  if (!profile) return null;
  return (
    <section>
      <h2>Confira seus dados</h2>
      <p className="hint">
        Estes dados serão registrados na inscrição no momento da confirmação. Para corrigir, use &quot;Meus Dados&quot;
        na Área do Candidato antes de confirmar.
      </p>
      <p>
        {profile.fullName} · CPF {maskCpf(profile.cpf)} · nascimento {profile.birthDate.split("-").reverse().join("/")}
        <br />
        {profile.email} · {maskPhone(profile.phone)}
        <br />
        {profile.street}, {profile.addressNumber}
        {profile.complement ? ` — ${profile.complement}` : ""} · {profile.neighborhood} · {profile.city}/{profile.uf} ·
        CEP {maskCep(profile.cep)}
        <br />
        Pessoa com deficiência: {profile.hasDisability ? "sim" : "não"}
      </p>
    </section>
  );
}

function ApplicationPage({ applicationId }: { applicationId: string }) {
  const router = useRouter();
  const [application, setApplication] = useState<ApplicationDetail | null>(null);
  const [version, setVersion] = useState(0);
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [agreed, setAgreed] = useState(false);
  const reload = useCallback(() => setVersion((current) => current + 1), []);

  useEffect(() => {
    apiGet<ApplicationDetail>(`/api/candidate/applications/${encodeURIComponent(applicationId)}`)
      .then(setApplication)
      .catch((caught: unknown) => setError(caught instanceof ApiError ? caught : null));
  }, [applicationId, version]);

  if (!application) {
    return error ? <ErrorAlert error={error} /> : <p aria-live="polite">Carregando...</p>;
  }

  const base = `/api/candidate/applications/${application.id}`;
  const isDraft = application.status === "RASCUNHO";
  const totalFiles = application.requirements.reduce((sum, requirement) => sum + requirement.documents.length, 0);
  const missingMandatory = application.requirements.filter(
    (requirement) => requirement.mandatory && requirement.documents.length === 0,
  );

  async function run(action: () => Promise<unknown>, success?: string) {
    setError(null);
    setMessage(null);
    try {
      await action();
      if (success) setMessage(success);
      reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível concluir a operação."));
    }
  }

  return (
    <div className="card">
      <h1>Inscrição — {application.processNumber}</h1>
      <p>
        {application.processTitle}
        <br />
        <strong>Cargo:</strong> {application.positionName}
        <br />
        <strong>Situação:</strong> {APPLICATION_STATUS_LABELS[application.status]}
        {application.applicationNumber && (
          <>
            <br />
            <strong>Número da inscrição:</strong> {application.applicationNumber}
            <br />
            <strong>Data/hora:</strong> {formatDateTime(application.confirmedAt)}
            <br />
            <strong>Código de autenticidade:</strong> {application.verificationCode}
          </>
        )}
      </p>
      <ErrorAlert error={error} />
      <SuccessAlert message={message} />

      {!isDraft && (
        <p>
          <a className="button" href={`${base}/receipt`}>Baixar comprovante (PDF)</a>
        </p>
      )}
      {isDraft && !application.acceptingApplications && (
        <div className="alert alert-error" role="alert">
          O período de inscrições não está aberto. Este rascunho não pode mais ser confirmado.
        </div>
      )}

      <h2>Documentos</h2>
      {isDraft && (
        <p className="hint">
          PDF, JPG ou PNG, até 2 MB cada. {totalFiles} de {application.maxDocuments} arquivos enviados. Após a
          confirmação, não é possível trocar ou adicionar documentos.
        </p>
      )}
      {application.requirements.length === 0 && <p className="hint">Este processo não exige documentos.</p>}
      {application.requirements.map((requirement) => (
        <fieldset key={requirement.id}>
          <legend>
            {requirement.name} {requirement.mandatory ? "(obrigatório)" : "(opcional)"}
            {requirement.title && " — título"}
          </legend>
          {requirement.description && <p className="hint">{requirement.description}</p>}
          <ul>
            {requirement.documents.map((document) => (
              <li key={document.id}>
                <a href={`${base}/documents/${document.id}/file`}>{document.originalName}</a>{" "}
                <span className="hint">({Math.ceil(document.sizeBytes / 1024)} KB)</span>
                {!isDraft && <span className="hint"> · {DOCUMENT_STATUS_LABELS[document.status]}</span>}
                {isDraft && (
                  <>
                    {" "}
                    <button type="button" className="secondary"
                      onClick={() => run(() => apiDelete(`${base}/documents/${document.id}`))}>
                      Remover
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
          {isDraft && application.acceptingApplications && totalFiles < application.maxDocuments && (
            <DocumentUpload applicationId={application.id} requirementId={requirement.id} onUploaded={reload} />
          )}
        </fieldset>
      ))}

      {isDraft && application.acceptingApplications && (
        <>
          <hr />
          <PersonalDataReview />
          <section>
            <h2>Confirmar inscrição</h2>
            {missingMandatory.length > 0 && (
              <p className="field-error">
                Envie os documentos obrigatórios: {missingMandatory.map((requirement) => requirement.name).join(", ")}.
              </p>
            )}
            <label>
              <input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} /> Declaro,
              sob as penas da lei, que as informações prestadas são verdadeiras e que conheço e aceito as normas do
              edital.
            </label>
            <div className="actions">
              <button type="button" disabled={!agreed || missingMandatory.length > 0}
                onClick={() => run(() => apiPost(`${base}/confirm`), "Inscrição confirmada. O comprovante já está disponível.")}>
                Confirmar inscrição
              </button>
              <button type="button" className="secondary"
                onClick={() => run(async () => {
                  await apiDelete(base);
                  router.replace("/candidato");
                })}>
                Descartar rascunho
              </button>
            </div>
          </section>
        </>
      )}
      <div className="actions">
        <Link href="/candidato">Voltar para a Área do Candidato</Link>
      </div>
    </div>
  );
}

export default function CandidateApplicationPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => (
        <main>
          <ApplicationPage applicationId={id} />
        </main>
      )}
    </SessionGate>
  );
}
