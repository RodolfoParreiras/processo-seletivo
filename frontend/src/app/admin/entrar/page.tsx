import { LoginForm } from "@/features/auth/LoginForm";

export default function AdminLoginPage() {
  return (
    <LoginForm
      title="Área Administrativa"
      endpoint="/api/admin/auth/login"
      redirectTo="/admin"
      forgotPasswordHref="/admin/esqueci-senha"
    />
  );
}
