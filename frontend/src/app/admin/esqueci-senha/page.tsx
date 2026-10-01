import { ForgotPasswordForm } from "@/features/auth/ForgotPasswordForm";

export default function AdminForgotPasswordPage() {
  return <ForgotPasswordForm endpoint="/api/admin/auth/forgot-password" loginHref="/admin/entrar" />;
}
