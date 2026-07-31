export type CommitConsistencyRequest = {
  from: string;
  to: string;
};

export type ConsistencyCommit = {
  sha: string;
  message: string;
  htmlUrl: string;
  committedAt: string;
  commitType: string | null;
  scope: string | null;
  category: string;
  consistent: boolean;
  issues: string[];
};

export type ConsistencyGroup = {
  pathPattern: string | null;
  category: string;
  scope: string | null;
  expectedPattern: string | null;
  commitCount: number;
  consistentCount: number;
  inconsistentCount: number;
  commits: ConsistencyCommit[];
};

export type CommitConsistencyResponse = {
  from: string;
  to: string;
  commitCount: number;
  consistentCount: number;
  inconsistentCount: number;
  groups: ConsistencyGroup[];
};