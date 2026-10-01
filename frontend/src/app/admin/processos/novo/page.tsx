"use client";

import { useRouter } from "next/navigation";
import { AdminShell } from "@/features/admin/AdminShell";
import { ProcessDetailsForm } from "@/features/admin/process/ProcessDetailsForm";
import { apiPost } from "@/lib/api";

export default function NewProcessPage() {
  const router = useRouter();

  return (
    <AdminShell>
      {() => (
        <div className="card">
          <h1>Novo processo seletivo</h1>
          <p className="hint">O processo é criado como rascunho. Cargos, documentos e edital são cadastrados em seguida.</p>
          <ProcessDetailsForm
            submitLabel="Criar rascunho"
            onSubmit={async (payload) => {
              const created = await apiPost<{ id: string }>("/api/admin/processes", payload);
              router.push(`/admin/processos/${created.id}`);
            }}
          />
        </div>
      )}
    </AdminShell>
  );
}
