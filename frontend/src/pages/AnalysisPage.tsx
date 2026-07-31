import { useCallback, useEffect, useState } from "react";
import {
  executeAnalysisJob,
  fetchAnalysisJob,
  fetchAnalysisJobs,
} from "../api/analysisApi";
import Layout from "../components/Layout";
import ConsistencyAnalysisSection from "../components/analysis/ConsistencyAnalysisSection";
import FileGroupList from "../components/analysis/FileGroupList";
import type {
  AnalysisJob,
  AnalysisJobStatus,
} from "../types";

type AnalysisPageProps = {
  connectedRepositoryId: number;
};

type AnalysisTab = "commit" | "consistency";

const getToday = () => {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset() * 60 * 1000;

  return new Date(now.getTime() - timezoneOffset)
    .toISOString()
    .slice(0, 10);
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
  }).format(new Date(value));
};

function AnalysisPage({
  connectedRepositoryId,
}: AnalysisPageProps) {
  const [activeTab, setActiveTab] =
    useState<AnalysisTab>("commit");
  const [targetDate, setTargetDate] = useState(getToday());
  const [analysisJob, setAnalysisJob] =
    useState<AnalysisJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [executing, setExecuting] = useState(false);
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

  useEffect(() => {
    void loadLatestAnalysis(targetDate);
  }, [loadLatestAnalysis, targetDate]);

  const analysisJobId = analysisJob?.id;
  const analysisJobStatus = analysisJob?.status;

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
          className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700"
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

          <section className="mt-6 rounded-xl border border-slate-200 p-5">
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

          {errorMessage && (
            <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {errorMessage}
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
              <section className="mt-6 rounded-xl border border-slate-200 p-5">
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
                  <section className="mt-6">
                    <div className="flex flex-wrap items-end justify-between gap-3">
                      <div>
                        <h3 className="text-lg font-semibold text-slate-900">
                          분석 결과
                        </h3>

                        <p className="mt-1 text-sm text-slate-500">
                          총 {analysisJob.result.commitCount}개의
                          커밋을 분석했습니다.
                        </p>
                      </div>
                    </div>

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