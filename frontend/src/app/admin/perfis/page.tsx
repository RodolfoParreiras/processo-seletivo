"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";
import { ErrorAlert, SuccessAlert } from "@/components/FormAlert";
import { AdminShell } from "@/features/admin/AdminShell";
import { TextField } from "@/features/candidate/PersonalDataFields";
import { ApiError, apiDelete, apiGet, apiPost, apiPut } from "@/lib/api";

interface PermissionView {
  code: string;
  description: string;
}

interface RoleView {
  code: string;
  name: string;
  systemRole: boolean;
  permissions: string[];
  members: number;
}

function RoleForm({ permissions, initial, submitLabel, onSubmit, onCancel }: {
  permissions: PermissionView[];
  initial?: RoleView;
  submitLabel: string;
  onSubmit: (name: string, selected: string[]) => Promise<void>;
  onCancel?: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [selected, setSelected] = useState<string[]>(initial?.permissions ?? []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit(name.trim(), selected);
  }

  return (
    <form onSubmit={submit} noValidate>
      <TextField name={`role-name-${initial?.code ?? "new"}`} label="Nome do perfil" maxLength={100} value={name}
        onChange={(event) => setName(event.target.value)} />
      <fieldset>
        <legend>Permissões</legend>
        <div className="options">
          {permissions.map((permission) => (
            <label key={permission.code}>
              <input type="checkbox" checked={selected.includes(permission.code)}
                onChange={(event) => setSelected(event.target.checked
                  ? [...selected, permission.code]
                  : selected.filter((code) => code !== permission.code))} />{" "}
              {permission.description} <span className="hint">({permission.code})</span>
            </label>
          ))}
        </div>
      </fieldset>
      <div className="actions">
        <button type="submit" disabled={!name.trim()}>{submitLabel}</button>
        {onCancel && <button type="button" className="secondary" onClick={onCancel}>Cancelar</button>}
      </div>
    </form>
  );
}

function Roles() {
  const [permissions, setPermissions] = useState<PermissionView[]>([]);
  const [roles, setRoles] = useState<RoleView[]>([]);
  const [editing, setEditing] = useState<string | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((current) => current + 1), []);

  useEffect(() => {
    apiGet<PermissionView[]>("/api/admin/permissions").then(setPermissions).catch(() => setPermissions([]));
    apiGet<RoleView[]>("/api/admin/roles").then(setRoles).catch(() => setRoles([]));
  }, [version]);

  async function run(action: () => Promise<unknown>, success: string) {
    setError(null);
    setMessage(null);
    try {
      await action();
      setMessage(success);
      setEditing(null);
      reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught : new ApiError(0, "Não foi possível concluir a operação."));
    }
  }

  const describe = (code: string) => permissions.find((permission) => permission.code === code)?.description ?? code;

  return (
    <div className="card">
      <h1>Perfis e permissões</h1>
      <p className="hint">
        Um perfil agrupa permissões. Ao alterar um perfil, os administradores que o possuem precisam entrar novamente.
      </p>
      <ErrorAlert error={error} />
      <SuccessAlert message={message} />
      {roles.map((role) => (
        <fieldset key={role.code}>
          <legend>
            {role.name} {role.systemRole && <span className="hint">(perfil do sistema)</span>}
          </legend>
          {editing === role.code ? (
            <RoleForm permissions={permissions} initial={role} submitLabel="Salvar perfil" onCancel={() => setEditing(null)}
              onSubmit={(name, selected) => run(() => apiPut(`/api/admin/roles/${role.code}`, { name, permissions: selected }),
                "Perfil atualizado.")} />
          ) : (
            <>
              <p className="hint">{role.members} administrador(es)</p>
              <ul>
                {role.permissions.map((code) => <li key={code}>{describe(code)}</li>)}
              </ul>
              {!role.systemRole && (
                <div className="actions">
                  <button type="button" className="secondary" onClick={() => setEditing(role.code)}>Editar</button>
                  <button type="button" className="secondary" disabled={role.members > 0}
                    onClick={() => run(() => apiDelete(`/api/admin/roles/${role.code}`), "Perfil excluído.")}>
                    Excluir
                  </button>
                </div>
              )}
            </>
          )}
        </fieldset>
      ))}

      <h2>Novo perfil</h2>
      <RoleForm key={version} permissions={permissions} submitLabel="Criar perfil"
        onSubmit={(name, selected) => run(() => apiPost("/api/admin/roles", { name, permissions: selected }), "Perfil criado.")} />
    </div>
  );
}

export default function RolesPage() {
  return <AdminShell>{() => <Roles />}</AdminShell>;
}
