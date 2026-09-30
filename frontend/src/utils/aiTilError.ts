export const UNKNOWN_AI_TIL_ERROR_MESSAGE =
  "AI TIL 초안 생성 중 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요.";

const AI_TIL_ERROR_MESSAGES: Record<string, string> = {
  GEMINI_API_KEY_REQUIRED: "Gemini API 키를 입력해 주세요.",
  GEMINI_AUTHENTICATION_FAILED:
    "Gemini API 키가 올바르지 않거나 사용할 수 없습니다.\n키를 확인해 주세요.",
  GEMINI_RATE_LIMIT_EXCEEDED:
    "Gemini API 요청 한도를 초과했습니다.\n잠시 후 다시 시도해 주세요.",
  GEMINI_API_ERROR:
    "Gemini 요청을 처리하지 못했습니다.\n잠시 후 다시 시도해 주세요.",
  GEMINI_CONNECTION_FAILED:
    "Gemini 서비스에 연결할 수 없습니다.\n잠시 후 다시 시도해 주세요.",
  GEMINI_TIMEOUT:
    "Gemini 응답 시간이 초과됐습니다.\n잠시 후 다시 시도해 주세요.",
  GEMINI_INVALID_RESPONSE:
    "AI 응답을 처리할 수 없습니다.\n다시 시도해 주세요.",
};

export const getAiTilErrorMessage = (
  errorCode: string | null | undefined,
) => {
  if (!errorCode) {
    return UNKNOWN_AI_TIL_ERROR_MESSAGE;
  }

  return AI_TIL_ERROR_MESSAGES[errorCode] ?? UNKNOWN_AI_TIL_ERROR_MESSAGE;
};
