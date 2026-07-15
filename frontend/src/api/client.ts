import type { ConnectedRepository, GitHubRepository, User } from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const getAccessToken = () => {
  return localStorage.getItem("repoaryAccessToken");
};

export const saveAccessToken = (token: string) => {
  localStorage.setItem("repoaryAccessToken", token);
};

export const removeAccessToken = () => {
  localStorage.removeItem("repoaryAccessToken");
};

const authHeaders = () => {
  const token = getAccessToken();

  return {
    Authorization: `Bearer ${token}`,
  };
};

export const requestGitHubLoginUrl = async () => {
  const response = await fetch(`${API_BASE_URL}/api/auth/github/login`);

  if (!response.ok) {
    throw new Error("GitHub 로그인 URL 요청에 실패했습니다.");
  }

  const data: { loginUrl: string } = await response.json();
  return data.loginUrl;
};

export const fetchMe = async () => {
  const response = await fetch(`${API_BASE_URL}/api/users/me`, {
    headers: authHeaders(),
  });

  if (!response.ok) {
    throw new Error("사용자 정보 조회에 실패했습니다.");
  }

  const data: User = await response.json();
  return data;
};

export const fetchGitHubRepositories = async () => {
  const response = await fetch(`${API_BASE_URL}/api/github/repositories`, {
    headers: authHeaders(),
  });

  if (!response.ok) {
    throw new Error("GitHub 저장소 목록 조회에 실패했습니다.");
  }

  const data: GitHubRepository[] = await response.json();
  return data;
};

export const connectRepository = async (repository: GitHubRepository) => {
  const response = await fetch(`${API_BASE_URL}/api/repositories/connect`, {
    method: "POST",
    headers: {
      ...authHeaders(),
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      githubRepositoryId: repository.id,
      name: repository.name,
      fullName: repository.fullName,
      htmlUrl: repository.htmlUrl,
      privateRepository: repository.privateRepository,
      defaultBranch: repository.defaultBranch,
    }),
  });

  if (!response.ok) {
    throw new Error("저장소 연결에 실패했습니다.");
  }

  const data: ConnectedRepository = await response.json();
  return data;
};

export const fetchConnectedRepositories = async () => {
  const response = await fetch(`${API_BASE_URL}/api/repositories/connected`, {
    headers: authHeaders(),
  });

  if (!response.ok) {
    throw new Error("연결된 저장소 목록 조회에 실패했습니다.");
  }

  const data: ConnectedRepository[] = await response.json();
  return data;
};

export const disconnectRepository = async (githubRepositoryId: number) => {
  const response = await fetch(
    `${API_BASE_URL}/api/repositories/connected/${githubRepositoryId}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    }
  );

  if (!response.ok) {
    throw new Error("저장소 연결 해제에 실패했습니다.");
  }
};