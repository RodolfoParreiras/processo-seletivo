import { ForgotPasswordForm } from "@/features/auth/ForgotPasswordForm";

export default function CandidateForgotPasswordPage() {
  return <ForgotPasswordForm endpoint="/api/auth/forgot-password" loginHref="/entrar" />;
}
