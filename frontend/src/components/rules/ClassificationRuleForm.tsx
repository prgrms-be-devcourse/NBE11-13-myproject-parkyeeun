import { useEffect, useState } from "react";
import type {
  ClassificationRule,
  ClassificationRuleRequest,
} from "../../types";

type ClassificationRuleFormProps = {
  editingRule: ClassificationRule | null;
  submitting: boolean;
  onSubmit: (request: ClassificationRuleRequest) => Promise<void>;
  onCancelEdit: () => void;
};

const INITIAL_FORM: ClassificationRuleRequest = {
  pathPattern: "",
  category: "",
  scope: null,
  priority: 100,
};

function ClassificationRuleForm({
  editingRule,
  submitting,
  onSubmit,
  onCancelEdit,
}: ClassificationRuleFormProps) {
  const [form, setForm] =
    useState<ClassificationRuleRequest>(INITIAL_FORM);
  const [errorMessage, setErrorMessage] = useState("");

  const isEditing = editingRule !== null;

  useEffect(() => {
    if (!editingRule) {
      setForm(INITIAL_FORM);
      setErrorMessage("");
      return;
    }

    setForm({
      pathPattern: editingRule.pathPattern,
      category: editingRule.category,
      scope: editingRule.scope,
      priority: editingRule.priority,
    });
    setErrorMessage("");
  }, [editingRule]);

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement>,
  ) => {
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
    if (!form.pathPattern.trim()) {
      return "경로 패턴을 입력해주세요.";
    }

    if (!form.category.trim()) {
      return "category를 입력해주세요.";
    }

    if (
      !Number.isInteger(form.priority) ||
      form.priority < 0
    ) {
      return "priority는 0 이상의 정수여야 합니다.";
    }

    return "";
  };

  const handleSubmit = async (
    event: React.FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();

    const validationMessage = validate();

    if (validationMessage) {
      setErrorMessage(validationMessage);
      return;
    }

    setErrorMessage("");

    await onSubmit({
      pathPattern: form.pathPattern.trim(),
      category: form.category.trim(),
      scope: form.scope?.trim() || null,
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
            {isEditing ? "경로 규칙 수정" : "경로 규칙 추가"}
          </h4>

          <p className="mt-1 text-sm text-slate-500">
            변경 파일 경로를 분류할 패턴과 category, scope를
            입력합니다.
          </p>
        </div>
      </div>

      <div className="mt-4 grid gap-4 sm:grid-cols-2">
        <label className="sm:col-span-2">
          <span className="text-sm font-medium text-slate-700">
            경로 패턴
          </span>

          <input
            type="text"
            name="pathPattern"
            value={form.pathPattern}
            onChange={handleChange}
            placeholder="예: practice/**"
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
            value={form.category}
            onChange={handleChange}
            placeholder="예: practice"
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

export default ClassificationRuleForm;