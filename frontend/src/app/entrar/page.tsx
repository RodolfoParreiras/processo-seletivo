import { LoginForm } from "@/features/auth/LoginForm";

export default function CandidateLoginPage() {
  return (
    <LoginForm
      title="Área do Candidato"
      subtitle="Acesse para se inscrever e acompanhar suas inscrições."
      endpoint="/api/auth/login"
      redirectTo="/"
      forgotPasswordHref="/esqueci-senha"
      registerHref="/cadastro"
    />
  );
}
