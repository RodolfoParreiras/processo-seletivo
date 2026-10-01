import type { ApiError } from "@/lib/api";

export function ErrorAlert({ error }: { error: ApiError | null }) {
  if (!error) {
    return null;
  }
  return (
    <div className="alert alert-error" role="alert">
      {error.message}
      {error.violations.length > 0 && (
        <ul>
          {error.violations.map((violation) => (
            <li key={violation}>{violation}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

export function SuccessAlert({ message }: { message: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <div className="alert alert-success" role="status">
      {message}
    </div>
  );
}
