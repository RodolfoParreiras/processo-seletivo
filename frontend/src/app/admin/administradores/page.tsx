"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { type AdminContext, AdminShell } from "@/features/admin/AdminShell";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { formatDateTime } from "@/features/process/types";
import { ApiError, apiGet, apiPost, apiPut } from "@/lib/api";
import { isValidCpf, maskCpf, onlyDigits } from "@/lib/validation";

interface AdministratorView {
  accountId: string;
  fullName: string;
  maskedCpf: string;
  email: string;
  active: boolean;
  mfaEnabled: boolean;
  lastLoginAt: string | null;
  roles: { code: string; name: string }[];
}

interface RoleView {
  code: string;
  name: string;
}

function RolesEditor({ administrator, roles, onSaved, onError }: {
  administrator: AdministratorView;
  roles: RoleView[];
  onSaved: () => void;
  onError: (error: ApiError) => void;
}) {
  const [selected, setSelected] = useState<string[]>(administrator.roles.map((role) => role.code));

  async function save() {
    try {
      await apiPut(`/api/admin/administrators/${administrator.accountId}/roles`, { roleCodes: selected });
      onSaved();
    } catch (caught) {
      onError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível salvar os perfis."));
    }
  }

  return (
    <div className="options">
      {roles.map((role) => (
        <label key={role.code}>
          <input type="checkbox" checked={selected.includes(role.code)}
            onChange={(event) => setSelected(event.target.checked
              ? [...selected, role.code]
              : selected.filter((code) => code !== role.code))} />{" "}
          {role.name}
        </label>
      ))}
      <button type="button" className="secondary" onClick={save}>Salvar perfis</button>
    </div>
  );
}

function Administrators({ can }: AdminContext) {
  const [administrators, setAdministrators] = useState<AdministratorView[]>([]);
  const [roles, setRoles] = useState<RoleView[]>([]);
  const [editingRoles, setEditingRoles] = useState<string | null>(null);
  const [form, setForm] = useState({ cpf: "", fullName: "", email: "" });
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((current) => current + 1), []);
  const canManageRoles = can("PERMISSAO_GERENCIAR");

  useEffect(() => {
    apiGet<AdministratorView[]>("/api/admin/administrators").then(setAdministrators).catch(() => setAdministrators([]));
    apiGet<RoleView[]>("/api/admin/roles").then(setRoles).catch(() => setRoles([]));
  }, [version]);

  async function run(action: () => Promise<unknown>, success: string) {
    setError(null);
    setMessage(null);
    try {
      await action();
      setMessage(success);
      setEditingRoles(null);
      reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível concluir a operação."));
    }
  }

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isValidCpf(form.cpf)) {
      setError(new ApiError(400, "CPF inválido."));
      return;
    }
    await run(async () => {
      await apiPost("/api/admin/administrators", {
        cpf: onlyDigits(form.cpf), fullName: form.fullName.trim(), email: form.email.trim(),
      });
      setForm({ cpf: "", fullName: "", email: "" });
    }, "Administrador criado. Um link para definir a senha foi enviado ao e-mail informado.");
  }

  const operation = (accountId: string, name: string, success: string) =>
    run(() => apiPost(`/api/admin/administrators/${accountId}/${name}`), success);

  return (
    <div className="card">
      <h1>Administradores</h1>
      <ErrorAlert error={error} />
      <SuccessAlert message={message} />
      <table>
        <thead>
          <tr>
            <th scope="col">Nome</th>
            <th scope="col">Situação</th>
            <th scope="col">Perfis</th>
            <th scope="col">Ações</th>
          </tr>
        </thead>
        <tbody>
          {administrators.map((admin) => {
            return (
              <tr key={admin.accountId}>
                <td>
                  {admin.fullName}
                  <div className="hint">{admin.maskedCpf} · {admin.email}</div>
                  <div className="hint">Último acesso: {formatDateTime(admin.lastLoginAt)}</div>
                </td>
                <td>
                  {admin.active ? "Ativo" : "Inativo"}
                  <div className="hint">{admin.mfaEnabled ? "2º fator configurado" : "2º fator pendente"}</div>
                </td>
                <td>
                  {editingRoles === admin.accountId ? (
                    <RolesEditor administrator={admin} roles={roles} onSaved={() => {
                      setMessage("Perfis atualizados. As sessões do administrador foram encerradas.");
                      setEditingRoles(null);
                      reload();
                    }} onError={setError} />
                  ) : (
                    <>
                      {admin.roles.map((role) => role.name).join(", ") || "—"}
                      {canManageRoles && (
                        <div>
                          <button type="button" className="secondary" onClick={() => setEditingRoles(admin.accountId)}>
                            Alterar perfis
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </td>
                <td>
                  <div className="actions">
                    {admin.active ? (
                      <button type="button" className="secondary"
                        onClick={() => operation(admin.accountId, "deactivate", "Administrador desativado.")}>
                        Desativar
                      </button>
                    ) : (
                      <button type="button" className="secondary"
                        onClick={() => operation(admin.accountId, "activate", "Administrador reativado.")}>
                        Reativar
                      </button>
                    )}
                    {admin.mfaEnabled && (
                      <button type="button" className="secondary"
                        onClick={() => operation(admin.accountId, "reset-mfa",
                          "Segundo fator redefinido. O administrador cadastrará o aplicativo no próximo acesso.")}>
                        Redefinir 2º fator
                      </button>
                    )}
                    {admin.active && !admin.lastLoginAt && (
                      <button type="button" className="secondary"
                        onClick={() => operation(admin.accountId, "resend-setup-link", "Link de definição de senha reenviado.")}>
                        Reenviar link
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <h2>Novo administrador</h2>
      <p className="hint">A senha não é definida aqui: o administrador recebe por e-mail um link válido por 24 horas.</p>
      <form onSubmit={create} noValidate>
        <div className="grid">
          <TextField name="adminCpf" label="CPF" inputMode="numeric" value={form.cpf}
            onChange={(event) => setForm({ ...form, cpf: maskCpf(event.target.value) })} />
          <TextField name="adminEmail" label="E-mail" type="email" maxLength={254} value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })} />
        </div>
        <TextField name="adminName" label="Nome completo" maxLength={150} value={form.fullName}
          onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
        <button type="submit" disabled={!form.cpf || !form.fullName.trim() || !form.email.trim()}>
          Criar administrador
        </button>
      </form>
    </div>
  );
}

export default function AdministratorsPage() {
  return <AdminShell>{(context) => <Administrators {...context} />}</AdminShell>;
}
