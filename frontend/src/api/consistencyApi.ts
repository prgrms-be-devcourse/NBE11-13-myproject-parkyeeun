import type {
  CommitConsistencyRequest,
  CommitConsistencyResponse,
} from "../types/consistency";
import {
  API_BASE_URL,
  authHeaders,
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

export const analyzeCommitConsistency = async (
  connectedRepositoryId: number,
  request: CommitConsistencyRequest,
): Promise<CommitConsistencyResponse> => {
  const response = await fetch(
    `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/consistency-analysis`,
    {
      method: "POST",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    },
  );

  if (!response.ok) {
    throw new Error(
      await getErrorMessage(
        response,
        "커밋 컨벤션 분석에 실패했습니다.",
      ),
    );
  }

  return response.json() as Promise<CommitConsistencyResponse>;
};