export type AnalysisJobStatus =
  | "PENDING"
  | "RUNNING"
  | "COMPLETED"
  | "FAILED";

export type StoredFileAnalysis = {
  filename: string;
  status: string;
  previousFilename: string | null;
  category: string | null;
  scope: string | null;
};

export type StoredCommitAnalysis = {
  sha: string;
  message: string;
  committedAt: string;
  commitType: string | null;
  scope: string | null;
  categories: string[];
  files: StoredFileAnalysis[];
};

export type StoredAnalysisResult = {
  targetDate: string;
  commitCount: number;
  commits: StoredCommitAnalysis[];
};

export type AnalysisJob = {
  id: number;
  connectedRepositoryId: number;
  targetDate: string;
  status: AnalysisJobStatus;
  result: StoredAnalysisResult | null;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AnalysisJobSummary = {
  id: number;
  targetDate: string;
  status: AnalysisJobStatus;
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
};