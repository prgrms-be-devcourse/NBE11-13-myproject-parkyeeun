import type { TilDocument } from "../types";
import {
  API_BASE_URL,
  authHeaders,
  handleAuthenticationFailure,
} from "./client";

export class TilApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "TilApiError";
    this.status = status;
  }
}

const getErrorMessage = async (
  response: Response,
  fallbackMessage: string,
) => {
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
    throw new TilApiError(
      await getErrorMessage(response, fallbackMessage),
      response.status,
    );
  }

  return response.json() as Promise<T>;
};

const createTilBaseUrl = (connectedRepositoryId: number) =>
  `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/til-documents`;

const createDateQuery = (targetDate: string) =>
  new URLSearchParams({ date: targetDate }).toString();

export const createTilDraft = async (
  connectedRepositoryId: number,
  targetDate: string,
): Promise<TilDocument> => {
  return requestJson<TilDocument>(
    `${createTilBaseUrl(connectedRepositoryId)}?${createDateQuery(targetDate)}`,
    {
      method: "POST",
      headers: authHeaders(),
    },
    "TIL 초안 생성에 실패했습니다.",
  );
};

export const fetchTilByDate = async (
  connectedRepositoryId: number,
  targetDate: string,
): Promise<TilDocument> => {
  return requestJson<TilDocument>(
    `${createTilBaseUrl(connectedRepositoryId)}?${createDateQuery(targetDate)}`,
    {
      headers: authHeaders(),
    },
    "TIL 조회에 실패했습니다.",
  );
};

export const fetchTilDocument = async (
  connectedRepositoryId: number,
  tilDocumentId: number,
): Promise<TilDocument> => {
  return requestJson<TilDocument>(
    `${createTilBaseUrl(connectedRepositoryId)}/${tilDocumentId}`,
    {
      headers: authHeaders(),
    },
    "TIL 문서를 불러오지 못했습니다.",
  );
};

export const updateTilDocument = async (
  connectedRepositoryId: number,
  tilDocumentId: number,
  content: string,
): Promise<TilDocument> => {
  return requestJson<TilDocument>(
    `${createTilBaseUrl(connectedRepositoryId)}/${tilDocumentId}`,
    {
      method: "PATCH",
      headers: {
        ...authHeaders(),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content }),
    },
    "TIL 저장에 실패했습니다.",
  );
};
