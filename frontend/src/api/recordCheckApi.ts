import type { RecordCheckResponse } from "../types";
import {
  API_BASE_URL,
  authHeaders,
  handleAuthenticationFailure,
} from "./client";

const getErrorMessage = async (response: Response) => {
  try {
    const data = await response.json();

    if (typeof data.message === "string" && data.message.trim()) {
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

  return "학습 기록 누락 점검에 실패했습니다.";
};

export const fetchRecordChecks = async (
  connectedRepositoryId: number,
  month: string,
): Promise<RecordCheckResponse> => {
  const query = new URLSearchParams({ month });
  const response = await fetch(
    `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/record-checks?${query}`,
    { headers: authHeaders() },
  );

  handleAuthenticationFailure(response);

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json() as Promise<RecordCheckResponse>;
};
