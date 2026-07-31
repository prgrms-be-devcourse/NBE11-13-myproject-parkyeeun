import { useEffect, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import type {
  ConventionRule,
  ConventionRuleRequest,
} from "../../types";

type ConventionRuleFormProps = {
  editingRule: ConventionRule | null;
  submitting: boolean;
  onSubmit: (request: ConventionRuleRequest) => Promise<void>;
  onCancelEdit: () => void;
};

const INITIAL_FORM: ConventionRuleRequest = {
  messagePattern: "",
  commitType: "",
  scope: null,
  category: null,
  priority: 100,
};

function ConventionRuleForm({
  editingRule,
  submitting,
  onSubmit,
  onCancelEdit,
}: ConventionRuleFormProps) {
  const [form, setForm] =
    useState<ConventionRuleRequest>(INITIAL_FORM);
  const [errorMessage, setErrorMessage] = useState("");

  const isEditing = editingRule !== null;

  useEffect(() => {
    if (!editingRule) {
      setForm(INITIAL_FORM);
      setErrorMessage("");
      return;
    }

    setForm({
      messagePattern: editingRule.messagePattern,
      commitType: editingRule.commitType,
      scope: editingRule.scope,
      category: editingRule.category,
      priority: editingRule.priority,
    });
    setErrorMessage("");
  }, [editingRule]);

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;

    if (name === "priority") {
      setForm((prev) => ({
        ...prev,
        priority: Number(value),
      }));
      return;
    }

    setForm((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const validate = () => {
    if (!form.messagePattern.trim()) {
      return "커밋 메시지 패턴을 입력해주세요.";
    }

    if (!form.commitType.trim()) {
      return "커밋 type을 입력해주세요.";
    }

    if (!Number.isInteger(form.priority) || form.priority < 0) {
      return "priority는 0 이상의 정수여야 합니다.";
    }

    return "";
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationMessage = validate();

    if (validationMessage) {
      setErrorMessage(validationMessage);
      return;
    }

    setErrorMessage("");

    await onSubmit({
      messagePattern: form.messagePattern.trim(),
      commitType: form.commitType.trim(),
      scope: form.scope?.trim() || null,
      category: form.category?.trim() || null,
      priority: form.priority,
    });

    if (!isEditing) {
      setForm(INITIAL_FORM);
    }
  };

  const handleCancel = () => {
    setForm(INITIAL_FORM);
    setErrorMessage("");
    onCancelEdit();
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4"
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h4 className="font-semibold text-slate-900">
            {isEditing ? "커밋 규칙 수정" : "커밋 규칙 추가"}
          </h4>

          <p className="mt-1 text-sm text-slate-500">
            커밋 메시지 패턴과 분류할 type, scope, category를
            입력합니다.
          </p>
        </div>

        {isEditing && (
          <button
            type="button"
            onClick={handleCancel}
            disabled={submitting}
            className="text-sm font-medium text-slate-500 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-50"
          >
            수정 취소
          </button>
        )}
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <label className="sm:col-span-2">
          <span className="text-sm font-medium text-slate-700">
            커밋 메시지 패턴
          </span>

          <input
            type="text"
            name="messagePattern"
            value={form.messagePattern}
            onChange={handleChange}
            placeholder="예: study(java):"
            disabled={submitting}
            className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label>
          <span className="text-sm font-medium text-slate-700">
            type
          </span>

          <input
            type="text"
            name="commitType"
            value={form.commitType}
            onChange={handleChange}
            placeholder="예: study"
            disabled={submitting}
            className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label>
          <span className="text-sm font-medium text-slate-700">
            scope
          </span>

          <input
            type="text"
            name="scope"
            value={form.scope ?? ""}
            onChange={handleChange}
            placeholder="예: java"
            disabled={submitting}
            className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label>
          <span className="text-sm font-medium text-slate-700">
            category
          </span>

          <input
            type="text"
            name="category"
            value={form.category ?? ""}
            onChange={handleChange}
            placeholder="선택 입력"
            disabled={submitting}
            className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label>
          <span className="text-sm font-medium text-slate-700">
            priority
          </span>

          <input
            type="number"
            name="priority"
            min={0}
            step={1}
            value={form.priority}
            onChange={handleChange}
            disabled={submitting}
            className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-slate-400 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>
      </div>

      {errorMessage && (
        <p className="mt-3 text-sm text-red-600">
          {errorMessage}
        </p>
      )}

      <div className="mt-4 flex justify-end gap-2">
        <button
            type="button"
            onClick={handleCancel}
            disabled={submitting}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
            취소
        </button>

        <button
          type="submit"
          disabled={submitting}
          className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting
            ? "저장 중..."
            : isEditing
              ? "수정"
              : "추가"}
        </button>
      </div>
    </form>
  );
}

export default ConventionRuleForm;