import { useMemo, useState } from "react";
import { analyzeCommitConsistency } from "../../api/consistencyApi";
import type {
  CommitConsistencyResponse,
  ConsistencyGroup,
} from "../../types/consistency";

type ConsistencyAnalysisSectionProps = {
  connectedRepositoryId: number;
};

type PathGroup = {
  key: string;
  pathPattern: string | null;
  category: string;
  groups: ConsistencyGroup[];
  commitCount: number;
};

const getToday = () => {
  const now = new Date();
  const timezoneOffset =
    now.getTimezoneOffset() * 60 * 1000;

  return new Date(now.getTime() - timezoneOffset)
    .toISOString()
    .slice(0, 10);
};

const formatDateTime = (value: string) => {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
};

const groupByPath = (
  groups: ConsistencyGroup[],
): PathGroup[] => {
  const grouped = new Map<string, PathGroup>();

  groups.forEach((group) => {
    const key = [
      group.pathPattern ?? "unclassified",
      group.category,
    ].join("::");

    const current = grouped.get(key);

    if (current) {
      current.groups.push(group);
      current.commitCount += group.commitCount;
      return;
    }

    grouped.set(key, {
      key,
      pathPattern: group.pathPattern,
      category: group.category,
      groups: [group],
      commitCount: group.commitCount,
    });
  });

  return Array.from(grouped.values()).sort(
    (first, second) => {
      if (first.pathPattern === null) {
        return 1;
      }

      if (second.pathPattern === null) {
        return -1;
      }

      return first.pathPattern.localeCompare(
        second.pathPattern,
      );
    },
  );
};

function ConsistencyAnalysisSection({
  connectedRepositoryId,
}: ConsistencyAnalysisSectionProps) {
  const today = getToday();

  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(today);
  const [result, setResult] =
    useState<CommitConsistencyResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [expandedPaths, setExpandedPaths] = useState<
    Set<string>
  >(new Set());
  const [expandedRules, setExpandedRules] = useState<
    Set<string>
  >(new Set());

  const pathGroups = useMemo(
    () => groupByPath(result?.groups ?? []),
    [result],
  );

  const conventionCount = useMemo(
    () =>
      result?.groups.filter(
        (group) => group.expectedPattern !== null,
      ).length ?? 0,
    [result],
  );

  const unmatchedCommitCount =
    result?.inconsistentCount ?? 0;

  const handleAnalyze = async () => {
    if (!from || !to) {
      setErrorMessage(
        "분석 시작일과 종료일을 선택해 주세요.",
      );
      return;
    }

    if (from > to) {
      setErrorMessage(
        "분석 시작일은 종료일보다 늦을 수 없습니다.",
      );
      return;
    }

    setLoading(true);
    setErrorMessage("");

    try {
      const response = await analyzeCommitConsistency(
        connectedRepositoryId,
        {
          from,
          to,
        },
      );

      setResult(response);

      setExpandedPaths(
        new Set(
          groupByPath(response.groups).map(
            (pathGroup) => pathGroup.key,
          ),
        ),
      );

      setExpandedRules(new Set());
    } catch (error) {
      setResult(null);

      setErrorMessage(
        error instanceof Error
          ? error.message
          : "커밋 컨벤션 분석 중 오류가 발생했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  const togglePath = (key: string) => {
    setExpandedPaths((previous) => {
      const next = new Set(previous);

      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }

      return next;
    });
  };

  const toggleRule = (key: string) => {
    setExpandedRules((previous) => {
      const next = new Set(previous);

      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }

      return next;
    });
  };

  return (
    <section>
      <div>
        <h3 className="text-lg font-semibold text-slate-900">
          커밋 컨벤션 분석
        </h3>

        <p className="mt-2 text-sm leading-6 text-slate-500">
          선택한 기간에 각 경로에서 사용된 커밋 형식을
          확인하고, 새 커밋 메시지를 작성할 때 기존 형식을
          참고합니다.
        </p>
      </div>

      <div className="mt-5 rounded-xl border border-slate-200 bg-white p-5">
        <div className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <div>
            <label
              htmlFor="consistency-from"
              className="text-sm font-medium text-slate-700"
            >
              시작일
            </label>

            <input
              id="consistency-from"
              type="date"
              value={from}
              max={today}
              onChange={(event) =>
                setFrom(event.target.value)
              }
              disabled={loading}
              className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-50"
            />
          </div>

          <div>
            <label
              htmlFor="consistency-to"
              className="text-sm font-medium text-slate-700"
            >
              종료일
            </label>

            <input
              id="consistency-to"
              type="date"
              value={to}
              min={from}
              max={today}
              onChange={(event) =>
                setTo(event.target.value)
              }
              disabled={loading}
              className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 disabled:cursor-not-allowed disabled:bg-slate-50"
            />
          </div>

          <button
            type="button"
            onClick={() => void handleAnalyze()}
            disabled={loading || !from || !to}
            className="rounded-lg bg-slate-900 px-5 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? "분석 중..." : "컨벤션 분석"}
          </button>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          경로별 과거 커밋 메시지를 확인해 현재 작업에 적절한
          type과 scope를 선택할 수 있습니다.
        </p>
      </div>

      {errorMessage && (
        <div className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      )}

      {result && (
        <div className="mt-6">
          <div className="grid gap-3 sm:grid-cols-3">
            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm text-slate-500">
                분석 커밋
              </p>

              <p className="mt-2 text-2xl font-semibold text-slate-900">
                {result.commitCount}
              </p>
            </div>

            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm text-slate-500">
                사용된 경로
              </p>

              <p className="mt-2 text-2xl font-semibold text-slate-900">
                {pathGroups.length}
              </p>
            </div>

            <div className="rounded-xl border border-slate-200 bg-white p-4">
              <p className="text-sm text-slate-500">
                사용된 커밋 형식
              </p>

              <p className="mt-2 text-2xl font-semibold text-slate-900">
                {conventionCount}
              </p>
            </div>
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500">
            <p>
              분석 기간: {result.from} ~ {result.to}
            </p>

            {unmatchedCommitCount > 0 && (
              <p className="font-medium text-amber-700">
                등록되지 않은 형식의 커밋{" "}
                {unmatchedCommitCount}개
              </p>
            )}
          </div>

          {result.commitCount === 0 ? (
            <div className="mt-5 rounded-xl border border-dashed border-slate-300 px-4 py-10 text-center">
              <p className="text-sm font-medium text-slate-700">
                선택한 기간에 분석할 커밋이 없습니다.
              </p>

              <p className="mt-2 text-sm text-slate-500">
                다른 기간을 선택한 뒤 다시 분석해 주세요.
              </p>
            </div>
          ) : (
            <ul className="mt-5 space-y-4">
              {pathGroups.map((pathGroup) => {
                const pathExpanded =
                  expandedPaths.has(pathGroup.key);

                return (
                  <li
                    key={pathGroup.key}
                    className="overflow-hidden rounded-xl border border-slate-200 bg-white"
                  >
                    <button
                      type="button"
                      onClick={() =>
                        togglePath(pathGroup.key)
                      }
                      className="flex w-full items-center justify-between gap-4 bg-slate-50 px-5 py-4 text-left transition-colors hover:bg-slate-100"
                    >
                      <div className="min-w-0">
                        <p className="break-all font-semibold text-slate-900">
                          {pathGroup.pathPattern ??
                            "분류되지 않은 경로"}
                        </p>

                        <p className="mt-1 text-xs text-slate-500">
                          category: {pathGroup.category} ·{" "}
                          {pathGroup.commitCount}개 커밋 ·{" "}
                          {
                            pathGroup.groups.filter(
                              (group) =>
                                group.expectedPattern !==
                                null,
                            ).length
                          }
                          개 형식
                        </p>
                      </div>

                      <span className="shrink-0 text-sm text-slate-500">
                        {pathExpanded ? "접기" : "보기"}
                      </span>
                    </button>

                    {pathExpanded && (
                      <ul className="divide-y divide-slate-100">
                        {pathGroup.groups.map(
                          (group, groupIndex) => {
                            const ruleKey = [
                              pathGroup.key,
                              group.expectedPattern ??
                                "unmatched",
                              groupIndex,
                            ].join("::");

                            const ruleExpanded =
                              expandedRules.has(ruleKey);

                            const unmatched =
                              group.expectedPattern === null;

                            return (
                              <li key={ruleKey}>
                                <button
                                  type="button"
                                  onClick={() =>
                                    toggleRule(ruleKey)
                                  }
                                  className="flex w-full items-start justify-between gap-4 px-5 py-4 text-left transition-colors hover:bg-slate-50"
                                >
                                  <div className="min-w-0">
                                    <div className="flex flex-wrap items-center gap-2">
                                      <code
                                        className={
                                          unmatched
                                            ? "rounded-md bg-amber-100 px-2.5 py-1 text-sm font-medium text-amber-800"
                                            : "rounded-md bg-slate-100 px-2.5 py-1 text-sm font-medium text-slate-800"
                                        }
                                      >
                                        {group.expectedPattern ??
                                          "등록되지 않은 형식"}
                                      </code>

                                      <span className="text-xs text-slate-500">
                                        {group.commitCount}개
                                      </span>
                                    </div>

                                    {group.scope && (
                                      <div className="mt-2">
                                        <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                                          scope: {group.scope}
                                        </span>
                                      </div>
                                    )}

                                    {unmatched && (
                                      <p className="mt-2 text-xs text-amber-700">
                                        등록된 커밋 규칙에
                                        매칭되지 않은 커밋입니다.
                                      </p>
                                    )}
                                  </div>

                                  <span className="shrink-0 text-sm text-slate-500">
                                    {ruleExpanded
                                      ? "접기"
                                      : "커밋 보기"}
                                  </span>
                                </button>

                                {ruleExpanded && (
                                  <ul className="border-t border-slate-100 bg-slate-50/60">
                                    {group.commits.map(
                                      (commit) => (
                                        <li
                                          key={commit.sha}
                                          className="border-b border-slate-100 px-5 py-4 last:border-b-0"
                                        >
                                          <a
                                            href={commit.htmlUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="break-words text-sm font-medium text-slate-900 hover:underline"
                                          >
                                            {commit.message}
                                          </a>

                                          <p className="mt-1 text-xs text-slate-500">
                                            {commit.sha.slice(
                                              0,
                                              7,
                                            )}{" "}
                                            ·{" "}
                                            {formatDateTime(
                                              commit.committedAt,
                                            )}
                                          </p>

                                          {commit.issues.length >
                                            0 && (
                                            <ul className="mt-3 space-y-1 rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-800">
                                              {commit.issues.map(
                                                (
                                                  issue,
                                                  issueIndex,
                                                ) => (
                                                  <li
                                                    key={`${commit.sha}-${issueIndex}`}
                                                  >
                                                    {issue}
                                                  </li>
                                                ),
                                              )}
                                            </ul>
                                          )}
                                        </li>
                                      ),
                                    )}
                                  </ul>
                                )}
                              </li>
                            );
                          },
                        )}
                      </ul>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}

export default ConsistencyAnalysisSection;