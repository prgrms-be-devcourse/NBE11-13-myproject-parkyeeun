import type {
  AnalysisJob,
  AnalysisJobSummary,
} from "../types";
import {
  API_BASE_URL,
  authHeaders,
  handleAuthenticationFailure,
} from "./client";

const getErrorMessage = async (
  response: Response,
  fallbackMessage: string,
) => {
  try {
    const data = await response.json();

    if (
      typeof data.message === "string" &&
      data.message.trim()
    ) {
      return data.message;
    }

    if (
      typeof data.errorMessage === "string" &&
      data.errorMessage.trim()
    ) {
      return data.errorMessage;
    }
  } catch {
    // 응답 본문이 없거나 JSON이 아닌 경우 기본 메시지를 사용한다.
  }

  return fallbackMessage;
};

const requestJson = async <T>(
  url: string,
  options: RequestInit,
  fallbackMessage: string,
): Promise<T> => {
  const response = await fetch(url, options);

  handleAuthenticationFailure(response);

  if (!response.ok) {
    throw new Error(
      await getErrorMessage(response, fallbackMessage),
    );
  }

  return response.json() as Promise<T>;
};

const createAnalysisJobBaseUrl = (
  connectedRepositoryId: number,
) =>
  `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/analysis-jobs`;

export const executeAnalysisJob = async (
  connectedRepositoryId: number,
  targetDate: string,
): Promise<AnalysisJob> => {
  const query = new URLSearchParams({
    date: targetDate,
  });

  return requestJson<AnalysisJob>(
    `${createAnalysisJobBaseUrl(
      connectedRepositoryId,
    )}?${query.toString()}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
    "커밋 분석 실행에 실패했습니다.",
  );
};

export const fetchAnalysisJob = async (
  connectedRepositoryId: number,
  analysisJobId: number,
): Promise<AnalysisJob> => {
  return requestJson<AnalysisJob>(
    `${createAnalysisJobBaseUrl(
      connectedRepositoryId,
    )}/${analysisJobId}`,
    {
      headers: authHeaders(),
    },
    "커밋 분석 결과 조회에 실패했습니다.",
  );
};

export const fetchAnalysisJobs = async (
  connectedRepositoryId: number,
  targetDate?: string,
): Promise<AnalysisJobSummary[]> => {
  const query = new URLSearchParams();

  if (targetDate) {
    query.set("date", targetDate);
  }

  const queryString = query.toString();

  const url = queryString
    ? `${createAnalysisJobBaseUrl(
        connectedRepositoryId,
      )}?${queryString}`
    : createAnalysisJobBaseUrl(connectedRepositoryId);

  return requestJson<AnalysisJobSummary[]>(
    url,
    {
      headers: authHeaders(),
    },
    "커밋 분석 작업 목록 조회에 실패했습니다.",
  );
};
