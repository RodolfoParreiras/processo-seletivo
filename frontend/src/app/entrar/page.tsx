import { LoginForm } from "@/features/auth/LoginForm";

export default function CandidateLoginPage() {
  return (
    <LoginForm
      title="Área do Candidato"
      endpoint="/api/auth/login"
      redirectTo="/candidato"
      forgotPasswordHref="/esqueci-senha"
      registerHref="/cadastro"
    />
  );
}
