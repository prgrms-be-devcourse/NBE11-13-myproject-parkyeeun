import type { ReadmeRow } from "../types";
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

  return "README 행 생성에 실패했습니다.";
};

export const generateReadmeRow = async (
  connectedRepositoryId: number,
  targetDate: string,
): Promise<ReadmeRow> => {
  const query = new URLSearchParams({ date: targetDate });
  const response = await fetch(
    `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/readme-rows?${query}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
  );

  await handleAuthenticationFailure(response);

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json() as Promise<ReadmeRow>;
};
