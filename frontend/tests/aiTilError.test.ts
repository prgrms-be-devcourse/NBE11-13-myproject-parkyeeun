import assert from "node:assert/strict";
import test from "node:test";

import {
  getAiTilErrorMessage,
  UNKNOWN_AI_TIL_ERROR_MESSAGE,
} from "../src/utils/aiTilError.ts";

const messages = {
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

test("Gemini ErrorCode별 사용자 메시지를 반환한다", () => {
  for (const [code, message] of Object.entries(messages)) {
    assert.equal(getAiTilErrorMessage(code), message);
  }
});

test("알 수 없거나 누락된 ErrorCode는 안전한 공통 메시지를 반환한다", () => {
  assert.equal(
    getAiTilErrorMessage("UNKNOWN_ERROR"),
    UNKNOWN_AI_TIL_ERROR_MESSAGE,
  );
  assert.equal(getAiTilErrorMessage(null), UNKNOWN_AI_TIL_ERROR_MESSAGE);
});

test("GEMINI_API_ERROR 메시지는 서비스 혼잡을 단정하지 않는다", () => {
  assert.equal(
    getAiTilErrorMessage("GEMINI_API_ERROR").includes("혼잡"),
    false,
  );
});
