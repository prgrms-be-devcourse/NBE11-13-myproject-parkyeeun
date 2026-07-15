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