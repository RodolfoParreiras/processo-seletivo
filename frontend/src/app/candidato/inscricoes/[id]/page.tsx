"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { type ChangeEvent, type ReactNode, useCallback, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { Hero } from "@/components/Hero";
import {
  APPLICATION_STATUS_LABELS,
  type ApplicationDetail,
} from "@/features/application/types";
import { SessionGate } from "@/features/auth/SessionGate";
import type { CandidateProfile } from "@/features/candidate/ProfileForm";
import { formatDateTime } from "@/features/process/types";
import { ApiError, apiDelete, apiGet, apiPost, apiUpload } from "@/lib/api";
import { maskCep, maskCpf, maskPhone } from "@/lib/validation";

const MAX_FILE_BYTES = 2 * 1024 * 1024;

type Requirement = ApplicationDetail["requirements"][number];

function InfoItem({ label, children, wide }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? "info-item span-2" : "info-item"}>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

function StepTitle({ number, children }: { number?: number; children: ReactNode }) {
  return (
    <h2 className="step-title">
      {number !== undefined && <span className="step-number">{number}</span>}
      {children}
    </h2>
  );
}

/** Envia o arquivo assim que o candidato o escolhe. */
function DocumentUpload({ applicationId, requirement, onUploaded }: {
  applicationId: string;
  requirement: Requirement;
  onUploaded: () => void;
}) {
  const [error, setError] = useState<ApiError | null>(null);
  const [sending, setSending] = useState(false);
  const inputId = `arquivo-${requirement.id}`;

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const input = event.currentTarget;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > MAX_FILE_BYTES) {
      setError(new ApiError(400, "Arquivo acima de 2 MB."));
      input.value = "";
      return;
    }
    setError(null);
    setSending(true);
    try {
      const form = new FormData();
      form.append("file", file);
      await apiUpload(`/api/candidate/applications/${applicationId}/documents?requirementId=${requirement.id}`, form);
      onUploaded();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível enviar o arquivo."));
    } finally {
      input.value = "";
      setSending(false);
    }
  }

  return (
    <div className="upload">
      <input id={inputId} className="file-input" type="file" disabled={sending} onChange={upload}
        accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" />
      <label htmlFor={inputId} className="button secondary" aria-disabled={sending}>
        <i className="ti ti-upload" aria-hidden="true" />
        {sending ? "Enviando..." : "Escolher arquivo"}
        <span className="sr-only"> para {requirement.name}</span>
      </label>
      <ErrorAlert error={error} />
    </div>
  );
}

function DocumentsCard({ application, isDraft, onChanged, onAction, step }: {
  application: ApplicationDetail;
  isDraft: boolean;
  onChanged: () => void;
  onAction: (action: () => Promise<unknown>) => void;
  step?: number;
}) {
  const base = `/api/candidate/applications/${application.id}`;
  const totalFiles = application.requirements.reduce((sum, requirement) => sum + requirement.documents.length, 0);
  const canUpload = isDraft && application.acceptingApplications && totalFiles < application.maxDocuments;

  return (
    <section className="card">
      <StepTitle number={step}>
        {step === undefined && <i className="ti ti-paperclip" aria-hidden="true" />}
        Documentos
      </StepTitle>
      {application.requirements.length === 0 ? (
        <p className="hint">Este processo não exige documentos.</p>
      ) : (
        <>
          {isDraft && (
            <p className="hint">
              PDF, JPG ou PNG, até 2 MB cada · {totalFiles} de {application.maxDocuments} arquivos enviados. Após a
              confirmação, não é possível trocar ou adicionar documentos.
            </p>
          )}
          <div className="requirement-list">
            {application.requirements.map((requirement) => (
              <div key={requirement.id} className="requirement">
                <div className="requirement-name">
                  {requirement.name}
                  {requirement.title && <span className="hint"> · título</span>}
                  {requirement.mandatory ? (
                    <span className="required-mark" aria-label="obrigatório"> *</span>
                  ) : (
                    <span className="hint"> (opcional)</span>
                  )}
                </div>
                {requirement.description && <p className="hint">{requirement.description}</p>}
                {requirement.documents.length > 0 && (
                  <ul className="file-list">
                    {requirement.documents.map((document) => (
                      <li key={document.id}>
                        <i className="ti ti-file" aria-hidden="true" />
                        <a href={`${base}/documents/${document.id}/file`}>{document.originalName}</a>
                        <span className="hint">({Math.ceil(document.sizeBytes / 1024)} KB)</span>
                        {isDraft && (
                          <button type="button" className="link-danger"
                            aria-label={`Remover ${document.originalName}`}
                            onClick={() => onAction(() => apiDelete(`${base}/documents/${document.id}`))}>
                            <i className="ti ti-trash" aria-hidden="true" /> Remover
                          </button>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
                {isDraft && requirement.documents.length === 0 && !canUpload && (
                  <p className="hint">Nenhum arquivo enviado.</p>
                )}
                {canUpload && (
                  <DocumentUpload applicationId={application.id} requirement={requirement} onUploaded={onChanged} />
                )}
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

function PersonalDataReview() {
  const [profile, setProfile] = useState<CandidateProfile | null>(null);

  useEffect(() => {
    apiGet<CandidateProfile>("/api/candidate/me").then(setProfile).catch(() => setProfile(null));
  }, []);

  return (
    <section className="card">
      <StepTitle number={2}>Confira seus dados</StepTitle>
      {profile ? (
        <dl className="info-grid">
          <InfoItem label="Nome" wide>{profile.fullName}</InfoItem>
          <InfoItem label="CPF">{maskCpf(profile.cpf)}</InfoItem>
          <InfoItem label="Nascimento">{profile.birthDate.split("-").reverse().join("/")}</InfoItem>
          <InfoItem label="E-mail">{profile.email}</InfoItem>
          <InfoItem label="Telefone">{maskPhone(profile.phone)}</InfoItem>
          <InfoItem label="Endereço" wide>
            {profile.street}, {profile.addressNumber}
            {profile.complement ? ` — ${profile.complement}` : ""} · {profile.neighborhood} · {profile.city}/
            {profile.uf} · CEP {maskCep(profile.cep)}
          </InfoItem>
          <InfoItem label="Pessoa com deficiência">{profile.hasDisability ? "Sim" : "Não"}</InfoItem>
        </dl>
      ) : (
        <p className="hint" aria-live="polite">Carregando...</p>
      )}
      <p className="hint">
        Estes dados serão registrados na inscrição no momento da confirmação. Para corrigir, acesse{" "}
        <Link href="/candidato/dados">Meus Dados</Link> antes de confirmar.
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
    return (
      <main>
        {error ? <ErrorAlert error={error} /> : <p aria-live="polite">Carregando...</p>}
      </main>
    );
  }

  const base = `/api/candidate/applications/${application.id}`;
  const isDraft = application.status === "RASCUNHO";
  const canConfirm = isDraft && application.acceptingApplications;
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

  function discard() {
    if (!window.confirm("Descartar este rascunho? Os arquivos enviados serão removidos.")) return;
    run(async () => {
      await apiDelete(base);
      router.replace("/candidato/candidaturas");
    });
  }

  const status = APPLICATION_STATUS_LABELS[application.status];

  return (
    <>
      <Hero
        title={`${application.processNumber} · ${application.processTitle}`}
        subtitle={isDraft ? `${application.positionName} · ${status}` : application.positionName}
        back={{ href: "/candidato/candidaturas", label: "Minhas Candidaturas" }}
      />
      <main className="stack">
        <ErrorAlert error={error} />
        <SuccessAlert message={message} />

        {!isDraft && (
          <section className="card">
            <div className="card-header">
              <StepTitle><i className="ti ti-file-certificate" aria-hidden="true" />Dados da inscrição</StepTitle>
              <a className="button" href={`${base}/receipt`}>
                <i className="ti ti-download" aria-hidden="true" /> Baixar comprovante (PDF)
              </a>
            </div>
            <dl className="info-grid">
              <InfoItem label="Situação">{status}</InfoItem>
              <InfoItem label="Número da inscrição">{application.applicationNumber}</InfoItem>
              <InfoItem label="Data e hora">{formatDateTime(application.confirmedAt)}</InfoItem>
              <InfoItem label="Cargo">{application.positionName}</InfoItem>
              <InfoItem label="Código de autenticidade" wide>
                <span className="code">{application.verificationCode}</span>
              </InfoItem>
              {application.decisionReason && (
                <InfoItem label="Justificativa" wide>{application.decisionReason}</InfoItem>
              )}
            </dl>
          </section>
        )}

        {isDraft && !application.acceptingApplications && (
          <div className="alert alert-error" role="alert">
            O período de inscrições não está aberto. Este rascunho não pode mais ser confirmado.
          </div>
        )}

        <DocumentsCard application={application} isDraft={isDraft} onChanged={reload}
          onAction={(action) => run(action)} step={canConfirm ? 1 : undefined} />

        {canConfirm && (
          <>
            <PersonalDataReview />
            <section className="card">
              <StepTitle number={3}>Confirmar inscrição</StepTitle>
              {missingMandatory.length > 0 && (
                <p className="field-error">
                  Envie os documentos obrigatórios: {missingMandatory.map((requirement) => requirement.name).join(", ")}.
                </p>
              )}
              <label className="declaration">
                <input type="checkbox" checked={agreed} onChange={(event) => setAgreed(event.target.checked)} />
                <span>
                  Declaro, sob as penas da lei, que as informações prestadas são verdadeiras e que conheço e aceito as
                  normas do edital.
                </span>
              </label>
              <div className="confirm-actions">
                <button type="button" className="link-danger" onClick={discard}>
                  <i className="ti ti-trash" aria-hidden="true" /> Descartar rascunho
                </button>
                <button type="button" disabled={!agreed || missingMandatory.length > 0}
                  onClick={() => run(() => apiPost(`${base}/confirm`),
                    "Inscrição confirmada. O comprovante já está disponível.")}>
                  Confirmar inscrição
                </button>
              </div>
            </section>
          </>
        )}

      </main>
    </>
  );
}

export default function CandidateApplicationPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <SessionGate accountType="CANDIDATE" loginHref="/entrar">
      {() => <ApplicationPage applicationId={id} />}
    </SessionGate>
  );
}
