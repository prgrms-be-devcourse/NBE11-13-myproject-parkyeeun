import type {
  ClassificationRule,
  ClassificationRuleRequest,
  ConventionRule,
  ConventionRuleRequest,
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
    throw new Error(
      await getErrorMessage(response, fallbackMessage),
    );
  }

  return response.json() as Promise<T>;
};

const requestNoContent = async (
  url: string,
  options: RequestInit,
  fallbackMessage: string,
): Promise<void> => {
  const response = await fetch(url, options);

  handleAuthenticationFailure(response);

  if (!response.ok) {
    throw new Error(
      await getErrorMessage(response, fallbackMessage),
    );
  }
};

const createJsonHeaders = () => ({
  ...authHeaders(),
  "Content-Type": "application/json",
});

const createRuleBaseUrl = (connectedRepositoryId: number) =>
  `${API_BASE_URL}/api/repositories/${connectedRepositoryId}/rules`;

export const fetchClassificationRules = async (
  connectedRepositoryId: number,
): Promise<ClassificationRule[]> => {
  return requestJson<ClassificationRule[]>(
    `${createRuleBaseUrl(connectedRepositoryId)}/classifications`,
    {
      headers: authHeaders(),
    },
    "경로 규칙 조회에 실패했습니다.",
  );
};

export const createClassificationRule = async (
  connectedRepositoryId: number,
  request: ClassificationRuleRequest,
): Promise<ClassificationRule> => {
  return requestJson<ClassificationRule>(
    `${createRuleBaseUrl(connectedRepositoryId)}/classifications`,
    {
      method: "POST",
      headers: createJsonHeaders(),
      body: JSON.stringify(request),
    },
    "경로 규칙 추가에 실패했습니다.",
  );
};

export const updateClassificationRule = async (
  connectedRepositoryId: number,
  ruleId: number,
  request: ClassificationRuleRequest,
): Promise<ClassificationRule> => {
  return requestJson<ClassificationRule>(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/classifications/${ruleId}`,
    {
      method: "PUT",
      headers: createJsonHeaders(),
      body: JSON.stringify(request),
    },
    "경로 규칙 수정에 실패했습니다.",
  );
};

export const updateClassificationRuleEnabled = async (
  connectedRepositoryId: number,
  ruleId: number,
  enabled: boolean,
): Promise<ClassificationRule> => {
  const query = new URLSearchParams({
    enabled: String(enabled),
  });

  return requestJson<ClassificationRule>(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/classifications/${ruleId}/enabled?${query}`,
    {
      method: "PATCH",
      headers: authHeaders(),
    },
    "경로 규칙 활성화 상태 변경에 실패했습니다.",
  );
};

export const deleteClassificationRule = async (
  connectedRepositoryId: number,
  ruleId: number,
): Promise<void> => {
  return requestNoContent(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/classifications/${ruleId}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    },
    "경로 규칙 삭제에 실패했습니다.",
  );
};

export const fetchConventionRules = async (
  connectedRepositoryId: number,
): Promise<ConventionRule[]> => {
  return requestJson<ConventionRule[]>(
    `${createRuleBaseUrl(connectedRepositoryId)}/conventions`,
    {
      headers: authHeaders(),
    },
    "커밋 규칙 조회에 실패했습니다.",
  );
};

export const createConventionRule = async (
  connectedRepositoryId: number,
  request: ConventionRuleRequest,
): Promise<ConventionRule> => {
  return requestJson<ConventionRule>(
    `${createRuleBaseUrl(connectedRepositoryId)}/conventions`,
    {
      method: "POST",
      headers: createJsonHeaders(),
      body: JSON.stringify(request),
    },
    "커밋 규칙 추가에 실패했습니다.",
  );
};

export const updateConventionRule = async (
  connectedRepositoryId: number,
  ruleId: number,
  request: ConventionRuleRequest,
): Promise<ConventionRule> => {
  return requestJson<ConventionRule>(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/conventions/${ruleId}`,
    {
      method: "PUT",
      headers: createJsonHeaders(),
      body: JSON.stringify(request),
    },
    "커밋 규칙 수정에 실패했습니다.",
  );
};

export const updateConventionRuleEnabled = async (
  connectedRepositoryId: number,
  ruleId: number,
  enabled: boolean,
): Promise<ConventionRule> => {
  const query = new URLSearchParams({
    enabled: String(enabled),
  });

  return requestJson<ConventionRule>(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/conventions/${ruleId}/enabled?${query}`,
    {
      method: "PATCH",
      headers: authHeaders(),
    },
    "커밋 규칙 활성화 상태 변경에 실패했습니다.",
  );
};

export const deleteConventionRule = async (
  connectedRepositoryId: number,
  ruleId: number,
): Promise<void> => {
  return requestNoContent(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/conventions/${ruleId}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    },
    "커밋 규칙 삭제에 실패했습니다.",
  );
};

export const restoreDefaultRules = async (
  connectedRepositoryId: number,
): Promise<void> => {
  return requestNoContent(
    `${createRuleBaseUrl(
      connectedRepositoryId,
    )}/defaults/restore`,
    {
      method: "POST",
      headers: authHeaders(),
    },
    "기본 규칙 복원에 실패했습니다.",
  );
};
