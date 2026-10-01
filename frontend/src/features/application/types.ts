export type ApplicationStatus = "RASCUNHO" | "RECEBIDA" | "DEFERIDA" | "INDEFERIDA";

export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = {
  RASCUNHO: "Rascunho (não confirmada)",
  RECEBIDA: "Recebida",
  DEFERIDA: "Deferida",
  INDEFERIDA: "Indeferida",
};

export interface ApplicationSummary {
  id: string;
  processId: string;
  processNumber: string;
  processTitle: string;
  positionName: string;
  status: ApplicationStatus;
  applicationNumber: string | null;
  createdAt: string;
  confirmedAt: string | null;
}

export interface ApplicationDocumentItem {
  id: string;
  originalName: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface ApplicationDetail {
  id: string;
  processId: string;
  processNumber: string;
  processTitle: string;
  positionName: string;
  status: ApplicationStatus;
  applicationNumber: string | null;
  verificationCode: string | null;
  decisionReason: string | null;
  createdAt: string;
  confirmedAt: string | null;
  registrationEnd: string;
  acceptingApplications: boolean;
  maxDocuments: number;
  requirements: {
    id: string;
    name: string;
    description: string | null;
    mandatory: boolean;
    title: boolean;
    documents: ApplicationDocumentItem[];
  }[];
}
