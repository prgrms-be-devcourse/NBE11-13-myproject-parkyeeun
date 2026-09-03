import type { AnalysisJobStatus, StoredAnalysisResult } from "./analysis";

export type User = {
  id: number;
  githubId: number;
  githubLogin: string;
  createdAt: string;
};

export type GitHubRepository = {
  id: number;
  name: string;
  fullName: string;
  htmlUrl: string;
  privateRepository: boolean;
  defaultBranch: string;
};

export type ConnectedRepository = {
  id: number;
  githubRepositoryId: number;
  name: string;
  fullName: string;
  htmlUrl: string;
  privateRepository: boolean;
  defaultBranch: string;
  connectedAt: string;
};

export type {
  ClassificationRule,
  ClassificationRuleRequest,
  ConventionRule,
  ConventionRuleRequest,
} from "./rule";

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

export * from "./analysis";
export * from "./til";
