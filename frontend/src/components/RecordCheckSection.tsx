import { useEffect, useState } from "react";
import { fetchRecordChecks } from "../api/recordCheckApi";
import type { ConnectedRepository, RecordCheckResponse } from "../types";

const getCurrentMonth = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
};

function StatusBadge({ complete }: { complete: boolean }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${
      complete ? "bg-slate-100 text-slate-600" : "bg-slate-700 text-white"
    }`}>
      {complete ? "완료" : "누락"}
    </span>
  );
}

function RecordCheckResults({
  connectedRepositoryId,
  month,
}: {
  connectedRepositoryId: number;
  month: string;
}) {
  const [result, setResult] = useState<RecordCheckResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [requestVersion, setRequestVersion] = useState(0);
  const [showMissingOnly, setShowMissingOnly] = useState(false);

  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setResult(null);
    setErrorMessage("");

    void fetchRecordChecks(connectedRepositoryId, month)
      .then((data) => {
        if (!cancelled) {
          setResult(data);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setErrorMessage(
            error instanceof Error
              ? error.message
              : "학습 기록을 점검하는 중 오류가 발생했습니다.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [connectedRepositoryId, month, requestVersion]);

  const refresh = () => {
    setResult(null);
    setErrorMessage("");
    setLoading(true);
    setRequestVersion((value) => value + 1);
  };

  if (loading) {
    return (
      <p role="status" className="mt-5 text-sm text-slate-500">
        {month} 학습 기록을 점검하고 있습니다.
      </p>
    );
  }

  if (errorMessage || !result) {
    return (
      <div className="mt-5">
        <p role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage || "학습 기록을 불러오지 못했습니다."}
        </p>
        <button type="button" onClick={refresh} className="mt-3 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
          다시 시도
        </button>
      </div>
    );
  }

  const completeCount = result.items.filter(
    (item) => item.tilExists && item.readmeEntryExists,
  ).length;
  const needsAttentionCount = result.items.length - completeCount;
  const allComplete = result.items.length > 0 && needsAttentionCount === 0;
  const visibleItems = showMissingOnly
    ? result.items.filter(
        (item) => !(item.tilExists && item.readmeEntryExists),
      )
    : result.items;

  const viewButtonClassName = (active: boolean) =>
    `rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
      active
        ? "bg-slate-700 text-white"
        : "text-slate-600 hover:bg-slate-100"
    }`;

  const summaryItems = [
    { label: "전체", count: result.items.length },
    { label: "완료", count: completeCount },
    { label: "확인 필요", count: needsAttentionCount },
  ];

  return (
    <div className="mt-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="font-semibold text-slate-900">{month} 학습 기록</h3>
        <button type="button" onClick={refresh} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
          다시 점검
        </button>
      </div>

      {result.items.length === 0 ? (
        <p role="status" className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
          이 달에는 점검할 학습 기록이 없습니다.
        </p>
      ) : (
        <>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
            <dl className="flex flex-wrap items-center gap-2 text-sm">
              {summaryItems.map((summary, index) => (
                <div key={summary.label} className="flex items-center gap-2">
                  {index > 0 && (
                    <span aria-hidden="true" className="text-slate-300">
                      /
                    </span>
                  )}
                  <dt className="text-slate-500">{summary.label}</dt>
                  <dd className="font-semibold text-slate-900">
                    {summary.count}
                  </dd>
                </div>
              ))}
            </dl>
            <div
              role="group"
              aria-label="학습 기록 표시 범위"
              className="inline-flex rounded-lg border border-slate-200 bg-white p-1"
            >
              <button
                type="button"
                aria-pressed={!showMissingOnly}
                onClick={() => setShowMissingOnly(false)}
                className={viewButtonClassName(!showMissingOnly)}
              >
                전체 보기
              </button>
              <button
                type="button"
                aria-pressed={showMissingOnly}
                onClick={() => setShowMissingOnly(true)}
                className={viewButtonClassName(showMissingOnly)}
              >
                누락만 보기
              </button>
            </div>
          </div>
          {allComplete && (
            <p role="status" className="mt-4 rounded-lg bg-slate-100/70 px-4 py-3 text-sm font-medium text-slate-700">
              이번 달 학습 기록이 모두 정리되어 있습니다.
            </p>
          )}
          {showMissingOnly && visibleItems.length === 0 ? (
            <p role="status" className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
              확인 필요한 기록이 없습니다.
            </p>
          ) : (
            <ul className="mt-4 divide-y divide-slate-200 rounded-xl border border-slate-200 bg-slate-50/50">
              {visibleItems.map((item) => (
                <li key={item.date} className="flex flex-wrap items-center justify-between gap-4 p-4">
                  <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
                    <time dateTime={item.date} className="font-medium text-slate-900">{item.date}</time>
                    <dl className="flex flex-wrap gap-4 text-sm">
                      <div className="flex items-center gap-2">
                        <dt className="text-slate-600">TIL</dt>
                        <dd><StatusBadge complete={item.tilExists} /></dd>
                      </div>
                      <div className="flex items-center gap-2">
                        <dt className="text-slate-600">README</dt>
                        <dd><StatusBadge complete={item.readmeEntryExists} /></dd>
                      </div>
                    </dl>
                  </div>
                  {(!item.tilExists || !item.readmeEntryExists) && (
                    <a
                      href={`/repositories/${connectedRepositoryId}/analysis?${new URLSearchParams({ date: item.date })}`}
                      className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100"
                    >
                      {!item.tilExists ? "TIL 작성하기" : "README 행 생성하기"}
                    </a>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}

function RecordCheckSection({ repository }: { repository: ConnectedRepository }) {
  const [month, setMonth] = useState(getCurrentMonth);

  return (
    <section className="mt-6 border-t border-slate-200 pt-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-slate-900">학습 기록 누락 점검</h2>
          <p className="mt-2 break-words text-sm text-slate-600">
            점검 저장소: {repository.fullName} · {repository.defaultBranch}
          </p>
        </div>
        <div>
          <label htmlFor="record-check-month" className="block text-sm font-medium text-slate-700">점검 월</label>
          <input
            id="record-check-month"
            type="month"
            value={month}
            onChange={(event) => setMonth(event.target.value)}
            className="mt-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900"
          />
        </div>
      </div>
      <p className="mt-3 text-sm leading-6 text-slate-500">
        GitHub 학습 커밋이 있는 날짜의 TIL과 README 반영 여부를 점검합니다.
        <br />
        누락된 기록은 해당 날짜의 분석 화면에서 보완할 수 있습니다.
      </p>
      {/^[0-9]{4}-(0[1-9]|1[0-2])$/.test(month) ? (
        <RecordCheckResults key={`${repository.id}:${month}`} connectedRepositoryId={repository.id} month={month} />
      ) : (
        <p className="mt-5 text-sm text-slate-500">점검할 월을 선택해 주세요.</p>
      )}
    </section>
  );
}

export default RecordCheckSection;
