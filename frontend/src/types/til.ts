export type TilDocumentStatus = "DRAFT";

export type TilDocument = {
  id: number;
  connectedRepositoryId: number;
  analysisJobId: number;
  targetDate: string;
  title: string;
  content: string;
  status: TilDocumentStatus;
  createdAt: string;
  updatedAt: string;
};
