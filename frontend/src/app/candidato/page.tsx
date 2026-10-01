import { redirect } from "next/navigation";

/** A lista de processos fica na página inicial; o endereço antigo continua funcionando. */
export default function CandidateHomePage() {
  redirect("/");
}
