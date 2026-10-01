"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import QRCode from "qrcode";
import { type FormEvent, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiPost } from "@/lib/api";
import { maskCpf, onlyDigits, safeRedirectPath } from "@/lib/validation";

type Step = "password" | "enroll" | "code";

interface Challenge {
  mfaRequired: boolean;
  enrollmentRequired: boolean;
}

interface Setup {
  secret: string;
  provisioningUri: string;
}

/** Login administrativo: senha e, em seguida, código do aplicativo autenticador (obrigatório). */
export function AdminLoginFlow() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("password");
  const [cpf, setCpf] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [setup, setSetup] = useState<Setup | null>(null);
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fail = (caught: unknown, fallback: string) =>
    setError(caught instanceof ApiError ? caught : new ApiError(0, fallback));

  async function submitPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const challenge = await apiPost<Challenge>("/api/admin/auth/login", { cpf: onlyDigits(cpf), password });
      setPassword("");
      if (challenge.enrollmentRequired) {
        const created = await apiPost<Setup>("/api/admin/auth/mfa/setup");
        setSetup(created);
        // QR code gerado localmente, sem enviar o segredo a serviços externos.
        setQrCode(await QRCode.toDataURL(created.provisioningUri, { margin: 1, width: 220 }));
        setStep("enroll");
      } else {
        setStep("code");
      }
    } catch (caught) {
      setPassword("");
      fail(caught, "Não foi possível entrar.");
    } finally {
      setSubmitting(false);
    }
  }

  async function submitCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiPost("/api/admin/auth/mfa/verify", { code: onlyDigits(code) });
      router.replace(safeRedirectPath(new URLSearchParams(window.location.search).get("redirect"), "/admin"));
    } catch (caught) {
      setCode("");
      if (caught instanceof ApiError && caught.message.includes("expirada")) {
        setStep("password");
      }
      fail(caught, "Código inválido.");
    } finally {
      setSubmitting(false);
    }
  }

  const codeForm = (
    <form onSubmit={submitCode} noValidate>
      <TextField name="code" label="Código de 6 dígitos do aplicativo autenticador" inputMode="numeric"
        autoComplete="one-time-code" maxLength={6} value={code}
        onChange={(event) => setCode(onlyDigits(event.target.value))} />
      <div className="actions">
        <button type="submit" disabled={submitting || code.length !== 6}>
          {submitting ? "Verificando..." : "Confirmar"}
        </button>
      </div>
    </form>
  );

  return (
    <main>
      <div className="card">
        <h1>Área Administrativa</h1>
        <ErrorAlert error={error} />

        {step === "password" && (
          <form onSubmit={submitPassword} noValidate>
            <TextField name="cpf" label="CPF" inputMode="numeric" autoComplete="username" value={cpf}
              onChange={(event) => setCpf(maskCpf(event.target.value))} />
            <TextField name="password" label="Senha" type="password" autoComplete="current-password" value={password}
              onChange={(event) => setPassword(event.target.value)} />
            <div className="actions">
              <button type="submit" disabled={submitting || !cpf || !password}>
                {submitting ? "Entrando..." : "Continuar"}
              </button>
              <Link href="/admin/esqueci-senha">Esqueci minha senha</Link>
            </div>
          </form>
        )}

        {step === "enroll" && setup && (
          <>
            <h2>Configure o segundo fator</h2>
            <p>
              O acesso administrativo exige um aplicativo autenticador (por exemplo, Google Authenticator ou Microsoft
              Authenticator). Escaneie o QR code abaixo no aplicativo e informe o código gerado.
            </p>
            {qrCode && <img src={qrCode} alt="QR code para cadastrar o aplicativo autenticador" width={220} height={220} />}
            <p className="hint">
              Se não puder escanear, digite esta chave no aplicativo: <code>{setup.secret}</code>
            </p>
            {codeForm}
          </>
        )}

        {step === "code" && (
          <>
            <p>Informe o código exibido no seu aplicativo autenticador.</p>
            {codeForm}
          </>
        )}
      </div>
    </main>
  );
}
