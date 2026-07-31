import type { ClassificationRule } from "../../types";

type ClassificationRuleListProps = {
  rules: ClassificationRule[];
  processingRuleId: number | null;
  onEdit: (rule: ClassificationRule) => void;
  onToggleEnabled: (rule: ClassificationRule) => Promise<void>;
  onDelete: (rule: ClassificationRule) => Promise<void>;
};

function ClassificationRuleList({
  rules,
  processingRuleId,
  onEdit,
  onToggleEnabled,
  onDelete,
}: ClassificationRuleListProps) {
  if (rules.length === 0) {
    return (
      <p className="mt-4 rounded-xl border border-dashed border-slate-300 px-4 py-6 text-center text-sm text-slate-500">
        등록된 경로 규칙이 없습니다.
      </p>
    );
  }

  return (
    <ul className="mt-4 space-y-3">
      {rules.map((rule) => {
        const processing = processingRuleId === rule.id;

        return (
          <li
            key={rule.id}
            className="rounded-xl border border-slate-200 p-4"
          >
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="break-all font-medium text-slate-900">
                  {rule.pathPattern}
                </p>

                <div className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-sm text-slate-500">
                  <span>category: {rule.category}</span>
                  <span>scope: {rule.scope ?? "-"}</span>
                  <span>priority: {rule.priority}</span>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {rule.defaultRule && (
                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                      기본 규칙
                    </span>
                  )}

                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${
                      rule.enabled
                        ? "bg-green-50 text-green-700"
                        : "bg-slate-100 text-slate-500"
                    }`}
                  >
                    {rule.enabled ? "활성" : "비활성"}
                  </span>
                </div>
              </div>

              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => onEdit(rule)}
                  disabled={processing}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  수정
                </button>

                <button
                  type="button"
                  onClick={() => void onToggleEnabled(rule)}
                  disabled={processing}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {processing
                    ? "처리 중..."
                    : rule.enabled
                      ? "비활성화"
                      : "활성화"}
                </button>

                <button
                  type="button"
                  onClick={() => void onDelete(rule)}
                  disabled={processing}
                  className="rounded-lg border border-red-200 bg-white px-3 py-2 text-sm font-medium text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  삭제
                </button>
              </div>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

export default ClassificationRuleList;