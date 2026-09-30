import { useCallback, useEffect, useState } from "react";
import {
  executeAnalysisJob,
  fetchAnalysisJob,
  fetchAnalysisJobs,
} from "../api/analysisApi";
import {
  createAiTilPreview,
  createTilDraft,
  fetchTilByDate,
  TilApiError,
  updateTilDocument,
} from "../api/tilApi";
import { getAiTilErrorMessage } from "../utils/aiTilError";
import type { AiTilPreview } from "../api/tilApi";
import Layout from "../components/Layout";
import ConsistencyAnalysisSection from "../components/analysis/ConsistencyAnalysisSection";
import FileGroupList from "../components/analysis/FileGroupList";
import ReadmeRowSection from "../components/analysis/ReadmeRowSection";
import { MarkdownPreview } from "./TilEditorPage";
import type {
  AnalysisJob,
  AnalysisJobStatus,
  TilDocument,
} from "../types";

type AnalysisPageProps = {
  connectedRepositoryId: number;
};

type AnalysisTab = "commit" | "consistency";

type TilLookupStatus = "idle" | "loading" | "ready" | "error";

const getToday = () => {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset() * 60 * 1000;

  return new Date(now.getTime() - timezoneOffset)
    .toISOString()
    .slice(0, 10);
};

const getInitialDate = () => {
  const date = new URLSearchParams(window.location.search).get("date");

  if (date && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
    const parsed = new Date(`${date}T00:00:00Z`);

    if (
      Number.isFinite(parsed.getTime()) &&
      parsed.toISOString().slice(0, 10) === date
    ) {
      return date;
    }
  }

  return getToday();
};

const getStatusLabel = (status: AnalysisJobStatus) => {
  switch (status) {
    case "PENDING":
      return "대기 중";
    case "RUNNING":
      return "분석 중";
    case "COMPLETED":
      return "완료";
    case "FAILED":
      return "실패";
  }
};

const getStatusClassName = (status: AnalysisJobStatus) => {
  switch (status) {
    case "PENDING":
      return "bg-slate-100 text-slate-600";
    case "RUNNING":
      return "bg-blue-50 text-blue-700";
    case "COMPLETED":
      return "bg-emerald-50 text-emerald-700";
    case "FAILED":
      return "bg-red-50 text-red-700";
  }
};

const formatDateTime = (value: string | null) => {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Seoul",
  }).format(new Date(value));
};

function AnalysisPage({
  connectedRepositoryId,
}: AnalysisPageProps) {
  const [activeTab, setActiveTab] =
    useState<AnalysisTab>("commit");
  const [targetDate, setTargetDate] = useState(getInitialDate);
  const [analysisJob, setAnalysisJob] =
    useState<AnalysisJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [executing, setExecuting] = useState(false);
  const [creatingTil, setCreatingTil] = useState(false);
  const [creatingAiTil, setCreatingAiTil] = useState(false);
  const [showAiTilDialog, setShowAiTilDialog] = useState(false);
  const [geminiApiKey, setGeminiApiKey] = useState("");
  const [aiTilError, setAiTilError] = useState("");
  const [aiTilPreview, setAiTilPreview] =
    useState<AiTilPreview | null>(null);
  const [savingAiTil, setSavingAiTil] = useState(false);
  const [existingTil, setExistingTil] =
    useState<TilDocument | null>(null);
  const [tilLookupKey, setTilLookupKey] = useState("");
  const [tilLookupStatus, setTilLookupStatus] =
    useState<TilLookupStatus>("idle");
  const [tilLookupError, setTilLookupError] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const loadLatestAnalysis = useCallback(
    async (date: string) => {
      setLoading(true);
      setErrorMessage("");

      try {
        const jobs = await fetchAnalysisJobs(
          connectedRepositoryId,
          date,
        );

        if (jobs.length === 0) {
          setAnalysisJob(null);
          return;
        }

        const latestJob = await fetchAnalysisJob(
          connectedRepositoryId,
          jobs[0].id,
        );

        setAnalysisJob(latestJob);
      } catch (error) {
        setErrorMessage(
          error instanceof Error
            ? error.message
            : "커밋 분석 결과를 불러오는 중 오류가 발생했습니다.",
        );
      } finally {
        setLoading(false);
      }
    },
    [connectedRepositoryId],
  );

  const handleExecuteAnalysis = async () => {
    setExecuting(true);
    setErrorMessage("");

    try {
      const createdJob = await executeAnalysisJob(
        connectedRepositoryId,
        targetDate,
      );

      setAnalysisJob(createdJob);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "커밋 분석 실행 중 오류가 발생했습니다.",
      );
    } finally {
      setExecuting(false);
    }
  };

  const moveToTilEditor = (tilDocumentId: number) => {
    window.location.assign(
      `/repositories/${connectedRepositoryId}/til/${tilDocumentId}`,
    );
  };

  const handleCreateTil = async () => {
    if (!analysisJob) {
      return;
    }

    setCreatingTil(true);
    setErrorMessage("");

    try {
      const tilDocument = await createTilDraft(
        connectedRepositoryId,
        analysisJob.targetDate,
      );

      moveToTilEditor(tilDocument.id);
    } catch (error) {
      if (error instanceof TilApiError && error.status === 409) {
        try {
          const existingTil = await fetchTilByDate(
            connectedRepositoryId,
            analysisJob.targetDate,
          );

          moveToTilEditor(existingTil.id);
          return;
        } catch (fetchError) {
          setErrorMessage(
            fetchError instanceof Error
              ? fetchError.message
              : "기존 TIL을 불러오는 중 오류가 발생했습니다.",
          );
        }
      } else {
        setErrorMessage(
          error instanceof Error
            ? error.message
            : "TIL 초안 생성 중 오류가 발생했습니다.",
        );
      }
    } finally {
      setCreatingTil(false);
    }
  };

  const closeAiTilDialog = () => {
    if (creatingAiTil) {
      return;
    }

    setShowAiTilDialog(false);
    setGeminiApiKey("");
    setAiTilError("");
  };

  const handleCreateAiTilPreview = async () => {
    if (!analysisJob || creatingAiTil) {
      return;
    }

    const apiKey = geminiApiKey.trim();
    if (!apiKey) {
      setAiTilError("Gemini API 키를 입력해 주세요.");
      return;
    }

    setCreatingAiTil(true);
    setAiTilError("");

    try {
      const preview = await createAiTilPreview(
        connectedRepositoryId,
        analysisJob.targetDate,
        apiKey,
      );

      setShowAiTilDialog(false);
      setAiTilPreview(preview);
    } catch (error) {
      setAiTilError(
        getAiTilErrorMessage(
          error instanceof TilApiError ? error.code : null,
        ),
      );
    } finally {
      setGeminiApiKey("");
      setCreatingAiTil(false);
    }
  };

  useEffect(() => {
    setShowAiTilDialog(false);
    setGeminiApiKey("");
    setAiTilError("");
    setAiTilPreview(null);
    setSavingAiTil(false);
  }, [connectedRepositoryId, targetDate]);

  useEffect(() => {
    void loadLatestAnalysis(targetDate);
  }, [loadLatestAnalysis, targetDate]);

  const analysisJobId = analysisJob?.id;
  const analysisJobStatus = analysisJob?.status;
  const completedTilLookupKey =
    analysisJobStatus === "COMPLETED" && analysisJob?.result
      ? `${targetDate}:${analysisJob.id}:${analysisJob.targetDate}:${analysisJob.updatedAt}`
      : "";

  useEffect(() => {
    let cancelled = false;

    setExistingTil(null);
    setTilLookupError("");

    if (!completedTilLookupKey || !analysisJob) {
      setTilLookupKey("");
      setTilLookupStatus("idle");
      return;
    }

    const lookupKey = completedTilLookupKey;
    const lookupDate = analysisJob.targetDate;

    setTilLookupKey(lookupKey);
    setTilLookupStatus("loading");

    void fetchTilByDate(connectedRepositoryId, lookupDate)
      .then((tilDocument) => {
        if (!cancelled) {
          setExistingTil(tilDocument);
          setTilLookupStatus("ready");
        }
      })
      .catch((error) => {
        if (cancelled) {
          return;
        }

        if (error instanceof TilApiError && error.status === 404) {
          setTilLookupStatus("ready");
          return;
        }

        setTilLookupStatus("error");
        setTilLookupError(
          error instanceof Error
            ? error.message
            : "TIL 존재 여부를 확인하는 중 오류가 발생했습니다.",
        );
      });

    return () => {
      cancelled = true;
    };
  }, [
    analysisJob,
    completedTilLookupKey,
    connectedRepositoryId,
  ]);

  useEffect(() => {
    if (
      !analysisJobId ||
      !analysisJobStatus ||
      !["PENDING", "RUNNING"].includes(analysisJobStatus)
    ) {
      return;
    }

    const intervalId = window.setInterval(() => {
      void fetchAnalysisJob(
        connectedRepositoryId,
        analysisJobId,
      )
        .then((updatedJob) => {
          setAnalysisJob(updatedJob);
        })
        .catch((error) => {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "분석 상태 조회 중 오류가 발생했습니다.",
          );
        });
    }, 2000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [
    analysisJobId,
    analysisJobStatus,
    connectedRepositoryId,
  ]);

  const analysisRunning =
    analysisJobStatus === "PENDING" ||
    analysisJobStatus === "RUNNING";
  const currentTilLookupStatus =
    tilLookupKey === completedTilLookupKey
      ? tilLookupStatus
      : "loading";
  const currentExistingTil =
    currentTilLookupStatus === "ready" ? existingTil : null;

  const closeAiTilPreview = () => {
    if (savingAiTil) {
      return;
    }

    setAiTilPreview(null);
    setAiTilError("");
  };

  const handleSaveAiTilPreview = async () => {
    if (!analysisJob || !aiTilPreview || savingAiTil) {
      return;
    }

    setSavingAiTil(true);
    setAiTilError("");

    try {
      let tilDocument = currentExistingTil;

      if (!tilDocument) {
        try {
          tilDocument = await createTilDraft(
            connectedRepositoryId,
            analysisJob.targetDate,
          );
        } catch (error) {
          if (!(error instanceof TilApiError) || error.status !== 409) {
            throw error;
          }

          tilDocument = await fetchTilByDate(
            connectedRepositoryId,
            analysisJob.targetDate,
          );
        }

        setExistingTil(tilDocument);
        setTilLookupStatus("ready");
      }

      const updatedDocument = await updateTilDocument(
        connectedRepositoryId,
        tilDocument.id,
        aiTilPreview.content,
      );

      moveToTilEditor(updatedDocument.id);
    } catch (error) {
      setAiTilError(
        error instanceof Error
          ? error.message
          : "AI TIL 초안을 저장하는 중 오류가 발생했습니다.",
      );
    } finally {
      setSavingAiTil(false);
    }
  };

  const handleTilAction = () => {
    if (currentExistingTil) {
      moveToTilEditor(currentExistingTil.id);
      return;
    }

    void handleCreateTil();
  };

  return (
    <Layout>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-slate-900">
            분석
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            커밋 변경 내용과 저장소의 커밋 컨벤션 사용 내역을
            분석합니다.
          </p>
        </div>

        <button
          type="button"
          onClick={() =>
            window.location.assign(
              `/repositories/${connectedRepositoryId}/rules`,
            )
          }
          className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50"
        >
          규칙 관리
        </button>
      </div>

      <div className="mt-6 border-b border-slate-200">
        <div
          role="tablist"
          aria-label="분석 유형"
          className="flex gap-6"
        >
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "commit"}
            onClick={() => setActiveTab("commit")}
            className={
              activeTab === "commit"
                ? "border-b-2 border-slate-900 px-1 pb-3 text-sm font-semibold text-slate-900"
                : "border-b-2 border-transparent px-1 pb-3 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700"
            }
          >
            커밋 분석
          </button>

          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "consistency"}
            onClick={() => setActiveTab("consistency")}
            className={
              activeTab === "consistency"
                ? "border-b-2 border-slate-900 px-1 pb-3 text-sm font-semibold text-slate-900"
                : "border-b-2 border-transparent px-1 pb-3 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700"
            }
          >
            컨벤션 분석
          </button>
        </div>
      </div>

      {activeTab === "commit" && (
        <>
          <div className="mt-6">
            <h3 className="text-lg font-semibold text-slate-900">
              커밋 분석
            </h3>

            <p className="mt-2 text-sm text-slate-500">
              선택한 날짜의 커밋과 변경 파일을 저장소 규칙에
              따라 분석합니다.
            </p>
          </div>

          <section className="mt-6 rounded-xl border border-slate-200 bg-slate-50/50 p-5">
            <label
              htmlFor="analysis-date"
              className="text-sm font-medium text-slate-700"
            >
              분석 날짜
            </label>

            <div className="mt-2 flex flex-wrap gap-2">
              <input
                id="analysis-date"
                type="date"
                value={targetDate}
                max={getToday()}
                onChange={(event) =>
                  setTargetDate(event.target.value)
                }
                disabled={executing || analysisRunning}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-50"
              />

              <button
                type="button"
                onClick={() => void handleExecuteAnalysis()}
                disabled={
                  !targetDate || executing || analysisRunning
                }
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                {executing
                  ? "요청 중..."
                  : analysisRunning
                    ? "분석 중..."
                    : analysisJobStatus === "COMPLETED"
                      ? "다시 분석"
                      : "분석 실행"}
              </button>
            </div>

            <p className="mt-2 text-xs text-slate-500">
              학습 날짜는 한국 시간 오전 6시부터 다음 날 오전
              6시 이전까지의 커밋을 기준으로 분석합니다.
            </p>
          </section>

          {(errorMessage || tilLookupError) && (
            <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {errorMessage || tilLookupError}
            </div>
          )}

          {showAiTilDialog && analysisJob && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4"
              role="dialog"
              aria-modal="true"
              aria-labelledby="ai-til-dialog-title"
            >
              <form
                onSubmit={(event) => {
                  event.preventDefault();
                  void handleCreateAiTilPreview();
                }}
                className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
              >
                <h3
                  id="ai-til-dialog-title"
                  className="text-lg font-semibold text-slate-900"
                >
                  AI로 TIL 초안 생성
                </h3>
                <p className="mt-2 text-sm leading-6 text-slate-600">
                  Gemini API Key는 저장되지 않고 이번 요청에만 사용됩니다.
                </p>

                <label
                  htmlFor="gemini-api-key"
                  className="mt-5 block text-sm font-medium text-slate-800"
                >
                  Gemini API Key
                </label>
                <input
                  id="gemini-api-key"
                  name="gemini-api-key"
                  type="password"
                  autoComplete="new-password"
                  value={geminiApiKey}
                  onChange={(event) => {
                    setGeminiApiKey(event.target.value);
                    setAiTilError("");
                  }}
                  disabled={creatingAiTil}
                  className="mt-2 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-500 disabled:bg-slate-100"
                  placeholder="Gemini API Key 입력"
                  autoFocus
                />

                <div className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-xs leading-5 text-amber-800">
                  <p>
                    AI 생성을 위해 해당 날짜의 GitHub 변경 내용이 Gemini로
                    전송될 수 있습니다.
                  </p>
                  <p className="mt-1">
                    비공개 저장소라면 코드 diff가 외부 AI 서비스로 전달될 수
                    있습니다.
                  </p>
                  <p className="mt-1">
                    AI를 사용하지 않아도 기존 TIL 초안 생성 기능은 그대로 사용할
                    수 있습니다.
                  </p>
                </div>

                {aiTilError && (
                  <p
                    role="alert"
                    className="mt-4 whitespace-pre-line rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
                  >
                    {aiTilError}
                  </p>
                )}

                <div className="mt-6 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={closeAiTilDialog}
                    disabled={creatingAiTil}
                    className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    취소
                  </button>
                  <button
                    type="submit"
                    disabled={creatingAiTil}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {creatingAiTil ? "AI 생성 중..." : "AI로 생성"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {aiTilPreview && analysisJob && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-4 py-6"
              role="dialog"
              aria-modal="true"
              aria-labelledby="ai-til-preview-title"
            >
              <div className="flex max-h-full w-full max-w-3xl flex-col rounded-2xl bg-white p-6 shadow-xl">
                <div>
                  <h3
                    id="ai-til-preview-title"
                    className="text-lg font-semibold text-slate-900"
                  >
                    AI TIL 초안 미리보기
                  </h3>
                  <p className="mt-1 text-sm text-slate-500">
                    {aiTilPreview.title}
                  </p>
                </div>

                <div className="mt-5 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/40 p-5">
                  <MarkdownPreview content={aiTilPreview.content} />
                </div>

                <p className="mt-4 text-sm text-slate-600">
                  {currentExistingTil
                    ? "현재 TIL 내용이 AI 초안으로 교체됩니다. 적용 전 내용을 확인하세요."
                    : "확인 후 새 TIL 초안으로 저장됩니다."}
                </p>

                {aiTilError && (
                  <p
                    role="alert"
                    className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
                  >
                    {aiTilError}
                  </p>
                )}

                <div className="mt-6 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={closeAiTilPreview}
                    disabled={savingAiTil}
                    className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    취소
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleSaveAiTilPreview()}
                    disabled={savingAiTil}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {savingAiTil
                      ? "저장 중..."
                      : currentExistingTil
                        ? "현재 TIL에 적용"
                        : "이 내용으로 TIL 생성"}
                  </button>
                </div>
              </div>
            </div>
          )}

          {loading ? (
            <p className="mt-8 text-sm text-slate-500">
              분석 결과를 불러오는 중입니다.
            </p>
          ) : !analysisJob ? (
            <div className="mt-6 rounded-xl border border-dashed border-slate-300 px-4 py-10 text-center">
              <p className="text-sm font-medium text-slate-700">
                선택한 날짜의 분석 결과가 없습니다.
              </p>

              <p className="mt-2 text-sm text-slate-500">
                분석 실행 버튼을 눌러 커밋 분석을 시작합니다.
              </p>
            </div>
          ) : (
            <>
              <section className="mt-6 rounded-xl border border-slate-200 bg-slate-50/50 p-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="font-semibold text-slate-900">
                      분석 상태
                    </h3>

                    <p className="mt-1 text-sm text-slate-500">
                      대상 날짜: {analysisJob.targetDate}
                    </p>
                  </div>

                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${getStatusClassName(
                      analysisJob.status,
                    )}`}
                  >
                    {getStatusLabel(analysisJob.status)}
                  </span>
                </div>

                <dl className="mt-4 grid gap-3 text-sm sm:grid-cols-3">
                  <div>
                    <dt className="text-slate-500">
                      요청 시각
                    </dt>
                    <dd className="mt-1 text-slate-900">
                      {formatDateTime(analysisJob.createdAt)}
                    </dd>
                  </div>

                  <div>
                    <dt className="text-slate-500">
                      시작 시각
                    </dt>
                    <dd className="mt-1 text-slate-900">
                      {formatDateTime(analysisJob.startedAt)}
                    </dd>
                  </div>

                  <div>
                    <dt className="text-slate-500">
                      완료 시각
                    </dt>
                    <dd className="mt-1 text-slate-900">
                      {formatDateTime(analysisJob.completedAt)}
                    </dd>
                  </div>
                </dl>

                {analysisRunning && (
                  <p className="mt-4 text-sm text-blue-700">
                    GitHub 커밋과 변경 파일을 분석하고 있습니다.
                  </p>
                )}

                {analysisJob.status === "FAILED" && (
                  <div className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
                    {analysisJob.errorMessage ??
                      "커밋 분석에 실패했습니다."}
                  </div>
                )}
              </section>

              {analysisJob.status === "COMPLETED" &&
                analysisJob.result && (
                  <>
                    {analysisJob.targetDate === targetDate && (
                      <ReadmeRowSection
                        key={`${connectedRepositoryId}:${completedTilLookupKey}`}
                        connectedRepositoryId={connectedRepositoryId}
                        targetDate={analysisJob.targetDate}
                        canGenerate={!!currentExistingTil && !executing}
                        tilAction={
                          <>
                            <button
                              type="button"
                              onClick={handleTilAction}
                              disabled={
                                creatingTil ||
                                creatingAiTil ||
                                currentTilLookupStatus !== "ready"
                              }
                              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {creatingTil
                                ? "TIL 생성 중..."
                                : currentTilLookupStatus === "loading"
                                  ? "TIL 확인 중..."
                                  : currentTilLookupStatus === "error"
                                    ? "TIL 확인 실패"
                                    : currentExistingTil
                                      ? "TIL 확인하기"
                                      : "TIL 초안 만들기"}
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setAiTilError("");
                                setGeminiApiKey("");
                                setAiTilPreview(null);
                                setShowAiTilDialog(true);
                              }}
                              disabled={
                                creatingTil ||
                                creatingAiTil ||
                                currentTilLookupStatus !== "ready"
                              }
                              className="rounded-lg border border-violet-200 bg-violet-50 px-4 py-2 text-sm font-medium text-violet-700 transition-colors hover:bg-violet-100 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {creatingAiTil
                                ? "AI 생성 중..."
                                : "AI로 초안 생성"}
                            </button>
                          </>
                        }
                        unavailableMessage={
                          executing
                            ? "분석 완료 후 README 행을 생성할 수 있습니다."
                            : currentTilLookupStatus === "error"
                              ? "TIL 존재 여부를 확인하지 못했습니다."
                              : currentTilLookupStatus !== "ready"
                                ? "TIL 존재 여부를 확인하고 있습니다."
                                : "TIL이 있어야 README 행을 생성할 수 있습니다."
                        }
                        showMissingTilTooltip={
                          !executing &&
                          currentTilLookupStatus === "ready" &&
                          !currentExistingTil
                        }
                      />
                    )}

                    <section className="mt-6 border-t border-slate-200 pt-6">
                      <h3 className="text-lg font-semibold text-slate-900">
                        분석 결과
                      </h3>
                      <p className="mt-1 text-sm text-slate-500">
                        총 {analysisJob.result.commitCount}개의
                        커밋을 분석했습니다.
                      </p>

                    {analysisJob.result.commits.length ===
                    0 ? (
                      <div className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-10 text-center text-sm text-slate-500">
                        선택한 날짜에 분석할 커밋이 없습니다.
                      </div>
                    ) : (
                      <ul className="mt-4 space-y-4">
                        {analysisJob.result.commits.map(
                          (commit) => (
                            <li
                              key={commit.sha}
                              className="rounded-xl border border-slate-200 bg-slate-50/40 p-5"
                            >
                              <div className="flex flex-wrap items-start justify-between gap-3">
                                <div className="min-w-0">
                                  <p className="break-words font-semibold text-slate-900">
                                    {commit.message}
                                  </p>

                                  <p className="mt-1 text-xs text-slate-500">
                                    {commit.sha.slice(0, 7)} ·{" "}
                                    {formatDateTime(
                                      commit.committedAt,
                                    )}
                                  </p>
                                </div>

                                <div className="flex flex-wrap gap-2">
                                  {commit.commitType && (
                                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-700">
                                      type: {commit.commitType}
                                    </span>
                                  )}

                                  {commit.scope && (
                                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-700">
                                      scope: {commit.scope}
                                    </span>
                                  )}

                                  {commit.categories.map(
                                    (category) => (
                                      <span
                                        key={category}
                                        className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-700"
                                      >
                                        category: {category}
                                      </span>
                                    ),
                                  )}
                                </div>
                              </div>

                              <div className="mt-5">
                                {commit.files.length === 0 ? (
                                  <p className="text-sm text-slate-500">
                                    조회된 변경 파일이 없습니다.
                                  </p>
                                ) : (
                                  <FileGroupList
                                    files={commit.files}
                                  />
                                )}
                              </div>
                            </li>
                          ),
                        )}
                      </ul>
                    )}
                    </section>
                  </>
                )}
            </>
          )}
        </>
      )}

      {activeTab === "consistency" && (
        <div className="mt-6">
          <ConsistencyAnalysisSection
            connectedRepositoryId={connectedRepositoryId}
          />
        </div>
      )}
    </Layout>
  );
}

export default AnalysisPage;
