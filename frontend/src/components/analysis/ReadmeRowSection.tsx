import { useId, useRef, useState } from "react";
import type { ReactNode } from "react";
import { generateReadmeRow } from "../../api/readmeApi";
import type { ReadmeRow } from "../../types";

type ReadmeRowSectionProps = {
  connectedRepositoryId: number;
  targetDate: string;
  canGenerate: boolean;
  unavailableMessage: string;
  showMissingTilTooltip?: boolean;
  tilAction: ReactNode;
};

function ReadmeRowSection({
  connectedRepositoryId,
  targetDate,
  canGenerate,
  unavailableMessage,
  showMissingTilTooltip = false,
  tilAction,
}: ReadmeRowSectionProps) {
  const [row, setRow] = useState<ReadmeRow | null>(null);
  const [generating, setGenerating] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [copyStatus, setCopyStatus] = useState<{
    message: string;
    error: boolean;
  } | null>(null);
  const busy = useRef(false);
  const [copying, setCopying] = useState(false);
  const tooltipId = useId();
  const showTooltip = showMissingTilTooltip && !canGenerate;

  const handleGenerate = async () => {
    if (!canGenerate || busy.current) {
      return;
    }

    busy.current = true;
    setGenerating(true);
    setRow(null);
    setErrorMessage("");
    setCopyStatus(null);

    try {
      setRow(await generateReadmeRow(connectedRepositoryId, targetDate));
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "README 행 생성 중 오류가 발생했습니다.",
      );
    } finally {
      busy.current = false;
      setGenerating(false);
    }
  };

  const handleCopy = async () => {
    if (!row || busy.current) {
      return;
    }

    busy.current = true;
    setCopying(true);
    setCopyStatus(null);

    try {
      await navigator.clipboard.writeText(row.markdown);
      setCopyStatus({ message: "복사했습니다.", error: false });
    } catch {
      setCopyStatus({
        message: "Markdown 복사에 실패했습니다.",
        error: true,
      });
    } finally {
      busy.current = false;
      setCopying(false);
    }
  };

  return (
    <section className="mt-6 border-t border-slate-200 pt-6">
      <h3 className="text-lg font-semibold text-slate-900">학습 기록</h3>
      <p className="mt-1 text-sm text-slate-500">
        분석 결과를 바탕으로 TIL과 월별 README를 관리합니다.
      </p>
      <div className="mt-4 flex flex-wrap items-start gap-4">
        {tilAction}
        <span
          className="group relative inline-flex rounded-lg focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-400"
          tabIndex={showTooltip ? 0 : undefined}
          aria-label={showTooltip ? "README 행 생성 불가" : undefined}
          aria-describedby={showTooltip ? tooltipId : undefined}
        >
          <button
            type="button"
            onClick={() => void handleGenerate()}
            disabled={!canGenerate || generating || copying}
            className={`rounded-lg border border-transparent bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50 ${showTooltip ? "pointer-events-none" : ""}`}
          >
            {generating ? "README 생성 중..." : "README 행 생성"}
          </button>
          {showTooltip && (
            <span
              id={tooltipId}
              role="tooltip"
              className="invisible absolute top-full left-0 z-20 mt-3 w-max whitespace-nowrap rounded bg-slate-700 px-2 py-1 text-xs text-white shadow-sm group-hover:visible group-focus:visible"
            >
              TIL이 있어야 README 행을 생성할 수 있습니다.
            </span>
          )}
        </span>
      </div>

      {!canGenerate && !showTooltip && (
        <p className="mt-3 text-sm text-slate-500">
          {unavailableMessage}
        </p>
      )}

      {errorMessage && (
        <p role="alert" className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </p>
      )}

      {row && (
        <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50/60 p-5" aria-live="polite">
          <h4 className="mb-4 border-b border-slate-200 pb-3 font-semibold text-slate-900">README Summary</h4>
          <div className="rounded-lg bg-slate-100/80 p-4">
            <p className="text-sm font-semibold text-slate-800">{row.weekLabel}</p>
            <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-7 text-slate-700">
              {row.summary}
            </p>
          </div>
          <p className="mt-4 text-sm font-medium text-slate-700">Markdown</p>
          <pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-words rounded-lg bg-slate-200/50 p-4 font-mono text-sm leading-6 text-slate-900">
            <code>{row.markdown}</code>
          </pre>
          <button
            type="button"
            onClick={() => void handleCopy()}
            disabled={copying}
            className="mt-3 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {copying ? "복사 중..." : "Markdown 복사"}
          </button>
          {copyStatus && (
            <p role="status" className={`mt-3 text-sm ${copyStatus.error ? "text-red-700" : "text-emerald-700"}`}>
              {copyStatus.message}
            </p>
          )}
        </div>
      )}
    </section>
  );
}

export default ReadmeRowSection;
