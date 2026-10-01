"use client";

import { type FormEvent, useState } from "react";
import { ErrorAlert } from "@/components/FormAlert";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { formatDateTime } from "@/features/process/types";
import { ApiError, apiGet } from "@/lib/api";

interface Verification {
  applicationNumber: string;
  processNumber: string;
  processTitle: string;
  positionName: string;
  confirmedAt: string;
}

/** Verificação pública de autenticidade do comprovante; não exibe dados pessoais. */
export default function VerifyReceiptPage() {
  const [code, setCode] = useState("");
  const [result, setResult] = useState<Verification | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setResult(null);
    const compact = code.replace(/[^0-9A-Za-z]/g, "");
    if (compact.length !== 12) {
      setError(new ApiError(400, "O código tem 12 caracteres (ex.: ABCD-EFGH-JKLM)."));
      return;
    }
    try {
      setResult(await apiGet<Verification>(`/api/receipts/${encodeURIComponent(compact)}`));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível verificar."));
    }
  }

  return (
    <main>
      <div className="card">
        <h1>Verificar comprovante de inscrição</h1>
        <form onSubmit={verify} noValidate>
          <TextField name="code" label="Código de autenticidade" maxLength={14} value={code}
            onChange={(event) => setCode(event.target.value.toUpperCase())} />
          <button type="submit">Verificar</button>
        </form>
        <ErrorAlert error={error} />
        {result && (
          <div className="alert alert-success" role="status">
            Comprovante autêntico.
            <br />
            Inscrição {result.applicationNumber} — Processo Seletivo {result.processNumber} ({result.processTitle})
            <br />
            Cargo: {result.positionName} · {formatDateTime(result.confirmedAt)}
          </div>
        )}
      </div>
    </main>
  );
}
