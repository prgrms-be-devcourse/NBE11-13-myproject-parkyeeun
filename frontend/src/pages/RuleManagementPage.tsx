import { useCallback, useEffect, useState } from "react";
import {
  createClassificationRule,
  createConventionRule,
  deleteClassificationRule,
  deleteConventionRule,
  fetchClassificationRules,
  fetchConventionRules,
  restoreDefaultRules,
  updateClassificationRule,
  updateClassificationRuleEnabled,
  updateConventionRule,
  updateConventionRuleEnabled,
} from "../api/ruleApi";
import Layout from "../components/Layout";
import ClassificationRuleForm from "../components/rules/ClassificationRuleForm";
import ClassificationRuleList from "../components/rules/ClassificationRuleList";
import ConventionRuleForm from "../components/rules/ConventionRuleForm";
import ConventionRuleList from "../components/rules/ConventionRuleList";
import type {
  ClassificationRule,
  ClassificationRuleRequest,
  ConventionRule,
  ConventionRuleRequest,
} from "../types";

type RuleManagementPageProps = {
  connectedRepositoryId: number;
};

type RuleTab = "classification" | "convention";

function RuleManagementPage({
  connectedRepositoryId,
}: RuleManagementPageProps) {
  const [classificationRules, setClassificationRules] = useState<
    ClassificationRule[]
  >([]);
  const [conventionRules, setConventionRules] = useState<ConventionRule[]>([]);

  const [
    editingClassificationRule,
    setEditingClassificationRule,
  ] = useState<ClassificationRule | null>(null);
  const [editingConventionRule, setEditingConventionRule] =
    useState<ConventionRule | null>(null);

  const [activeTab, setActiveTab] =
    useState<RuleTab>("classification");

  const [showClassificationForm, setShowClassificationForm] =
    useState(false);
  const [showConventionForm, setShowConventionForm] =
    useState(false);

  const [loading, setLoading] = useState(true);
  const [restoring, setRestoring] = useState(false);

  const [
    submittingClassificationRule,
    setSubmittingClassificationRule,
  ] = useState(false);
  const [submittingConventionRule, setSubmittingConventionRule] =
    useState(false);

  const [
    processingClassificationRuleId,
    setProcessingClassificationRuleId,
  ] = useState<number | null>(null);
  const [
    processingConventionRuleId,
    setProcessingConventionRuleId,
  ] = useState<number | null>(null);

  const [errorMessage, setErrorMessage] = useState("");

  const loadRules = useCallback(async () => {
    setLoading(true);
    setErrorMessage("");

    try {
      const [classifications, conventions] = await Promise.all([
        fetchClassificationRules(connectedRepositoryId),
        fetchConventionRules(connectedRepositoryId),
      ]);

      setClassificationRules(classifications);
      setConventionRules(conventions);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "규칙을 불러오는 중 오류가 발생했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [connectedRepositoryId]);

  const handleSubmitClassificationRule = async (
    request: ClassificationRuleRequest,
  ) => {
    setSubmittingClassificationRule(true);
    setErrorMessage("");

    try {
      if (editingClassificationRule) {
        const updatedRule = await updateClassificationRule(
          connectedRepositoryId,
          editingClassificationRule.id,
          request,
        );

        setClassificationRules((prev) =>
          prev.map((rule) =>
            rule.id === updatedRule.id ? updatedRule : rule,
          ),
        );

        setEditingClassificationRule(null);
        setShowClassificationForm(false);
        return;
      }

      const createdRule = await createClassificationRule(
        connectedRepositoryId,
        request,
      );

      setClassificationRules((prev) => [
        ...prev,
        createdRule,
      ]);
      setShowClassificationForm(false);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "경로 규칙 저장 중 오류가 발생했습니다.",
      );

      throw error;
    } finally {
      setSubmittingClassificationRule(false);
    }
  };

  const handleToggleClassificationRule = async (
    rule: ClassificationRule,
  ) => {
    setProcessingClassificationRuleId(rule.id);
    setErrorMessage("");

    try {
      const updatedRule =
        await updateClassificationRuleEnabled(
          connectedRepositoryId,
          rule.id,
          !rule.enabled,
        );

      setClassificationRules((prev) =>
        prev.map((item) =>
          item.id === updatedRule.id ? updatedRule : item,
        ),
      );

      if (
        editingClassificationRule?.id === updatedRule.id
      ) {
        setEditingClassificationRule(updatedRule);
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "경로 규칙 상태 변경 중 오류가 발생했습니다.",
      );
    } finally {
      setProcessingClassificationRuleId(null);
    }
  };

  const handleDeleteClassificationRule = async (
    rule: ClassificationRule,
  ) => {
    const confirmed = window.confirm(
      `${rule.pathPattern} 경로 규칙을 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    setProcessingClassificationRuleId(rule.id);
    setErrorMessage("");

    try {
      await deleteClassificationRule(
        connectedRepositoryId,
        rule.id,
      );

      setClassificationRules((prev) =>
        prev.filter((item) => item.id !== rule.id),
      );

      if (rule.defaultRule) {
        setHasMissingDefaultRules(true);
    }

      if (editingClassificationRule?.id === rule.id) {
        setEditingClassificationRule(null);
        setShowClassificationForm(false);
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "경로 규칙 삭제 중 오류가 발생했습니다.",
      );
    } finally {
      setProcessingClassificationRuleId(null);
    }
  };

  const handleEditClassificationRule = (
    rule: ClassificationRule,
  ) => {
    setEditingClassificationRule(rule);
    setShowClassificationForm(true);
  };

  const handleAddClassificationRule = () => {
    setEditingClassificationRule(null);
    setShowClassificationForm(true);
  };

  const handleCancelClassificationForm = () => {
    setEditingClassificationRule(null);
    setShowClassificationForm(false);
  };

  const handleSubmitConventionRule = async (
    request: ConventionRuleRequest,
  ) => {
    setSubmittingConventionRule(true);
    setErrorMessage("");

    try {
      if (editingConventionRule) {
        const updatedRule = await updateConventionRule(
          connectedRepositoryId,
          editingConventionRule.id,
          request,
        );

        setConventionRules((prev) =>
          prev.map((rule) =>
            rule.id === updatedRule.id ? updatedRule : rule,
          ),
        );

        setEditingConventionRule(null);
        setShowConventionForm(false);
        return;
      }

      const createdRule = await createConventionRule(
        connectedRepositoryId,
        request,
      );

      setConventionRules((prev) => [
        ...prev,
        createdRule,
      ]);
      setShowConventionForm(false);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "커밋 규칙 저장 중 오류가 발생했습니다.",
      );

      throw error;
    } finally {
      setSubmittingConventionRule(false);
    }
  };

  const handleToggleConventionRule = async (
    rule: ConventionRule,
  ) => {
    setProcessingConventionRuleId(rule.id);
    setErrorMessage("");

    try {
      const updatedRule =
        await updateConventionRuleEnabled(
          connectedRepositoryId,
          rule.id,
          !rule.enabled,
        );

      setConventionRules((prev) =>
        prev.map((item) =>
          item.id === updatedRule.id ? updatedRule : item,
        ),
      );

      if (editingConventionRule?.id === updatedRule.id) {
        setEditingConventionRule(updatedRule);
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "커밋 규칙 상태 변경 중 오류가 발생했습니다.",
      );
    } finally {
      setProcessingConventionRuleId(null);
    }
  };

  const handleDeleteConventionRule = async (
    rule: ConventionRule,
  ) => {
    const confirmed = window.confirm(
      `${rule.messagePattern} 커밋 규칙을 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    setProcessingConventionRuleId(rule.id);
    setErrorMessage("");

    try {
      await deleteConventionRule(
        connectedRepositoryId,
        rule.id,
      );

      setConventionRules((prev) =>
        prev.filter((item) => item.id !== rule.id),
      );

    if (rule.defaultRule) {
        setHasMissingDefaultRules(true);
    }

      if (editingConventionRule?.id === rule.id) {
        setEditingConventionRule(null);
        setShowConventionForm(false);
      }
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "커밋 규칙 삭제 중 오류가 발생했습니다.",
      );
    } finally {
      setProcessingConventionRuleId(null);
    }
  };

  const handleEditConventionRule = (
    rule: ConventionRule,
  ) => {
    setEditingConventionRule(rule);
    setShowConventionForm(true);
  };

  const handleAddConventionRule = () => {
    setEditingConventionRule(null);
    setShowConventionForm(true);
  };

  const handleCancelConventionForm = () => {
    setEditingConventionRule(null);
    setShowConventionForm(false);
  };

  const [hasMissingDefaultRules, setHasMissingDefaultRules] =
  useState(true);

  const handleRestoreDefaultRules = async () => {
  const confirmed = window.confirm(
    "삭제된 기본 규칙을 복원하시겠습니까?",
  );

  if (!confirmed) {
    return;
  }

  const previousDefaultRuleCount =
    classificationRules.filter((rule) => rule.defaultRule).length +
    conventionRules.filter((rule) => rule.defaultRule).length;

  setRestoring(true);
  setErrorMessage("");

  try {
    await restoreDefaultRules(connectedRepositoryId);

    const [classifications, conventions] = await Promise.all([
      fetchClassificationRules(connectedRepositoryId),
      fetchConventionRules(connectedRepositoryId),
    ]);

    setClassificationRules(classifications);
    setConventionRules(conventions);

    const currentDefaultRuleCount =
      classifications.filter((rule) => rule.defaultRule).length +
      conventions.filter((rule) => rule.defaultRule).length;

    const restored = currentDefaultRuleCount > previousDefaultRuleCount;

    setHasMissingDefaultRules(false);

    window.alert(
      restored
        ? "기본 규칙을 복원했습니다."
        : "복원할 기본 규칙이 없습니다.",
    );
  } catch (error) {
    setErrorMessage(
      error instanceof Error
        ? error.message
        : "기본 규칙 복원 중 오류가 발생했습니다.",
    );
  } finally {
    setRestoring(false);
  }
};

  useEffect(() => {
    setHasMissingDefaultRules(true);
    void loadRules();
  }, [loadRules]);

  return (
    <Layout>
      <div className="mt-8 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-slate-900">
            저장소 규칙 관리
          </h2>

          <p className="mt-2 text-sm text-slate-500">
            커밋과 변경 파일을 분류할 경로 규칙과 커밋 규칙을
            관리합니다.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={handleRestoreDefaultRules}
            disabled={restoring || !hasMissingDefaultRules}
            className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {restoring
                ? "복원 중..."
                : hasMissingDefaultRules
                    ? "기본 규칙 복원"
                    : "복원할 기본 규칙 없음"}
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      )}

      {loading ? (
        <p className="mt-8 text-sm text-slate-500">
          규칙을 불러오는 중입니다.
        </p>
      ) : (
        <>
          <div className="mt-8 flex border-b border-slate-200">
            <button
              type="button"
              onClick={() => setActiveTab("classification")}
              className={`border-b-2 px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "classification"
                  ? "border-slate-900 text-slate-900"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              경로 규칙
              <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs">
                {classificationRules.length}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab("convention")}
              className={`border-b-2 px-4 py-3 text-sm font-medium transition-colors ${
                activeTab === "convention"
                  ? "border-slate-900 text-slate-900"
                  : "border-transparent text-slate-500 hover:text-slate-700"
              }`}
            >
              커밋 규칙
              <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs">
                {conventionRules.length}
              </span>
            </button>
          </div>

          {activeTab === "classification" ? (
            <section className="mt-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">
                    경로 규칙
                  </h3>

                  <p className="mt-1 text-sm text-slate-500">
                    변경 파일 경로를 기준으로 category와 scope를
                    분류합니다.
                  </p>
                </div>

                {!showClassificationForm && (
                  <button
                    type="button"
                    onClick={handleAddClassificationRule}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white"
                  >
                    규칙 추가
                  </button>
                )}
              </div>

              {showClassificationForm && (
                <ClassificationRuleForm
                  editingRule={editingClassificationRule}
                  submitting={submittingClassificationRule}
                  onSubmit={handleSubmitClassificationRule}
                  onCancelEdit={handleCancelClassificationForm}
                />
              )}

              <ClassificationRuleList
                rules={classificationRules}
                processingRuleId={
                  processingClassificationRuleId
                }
                onEdit={handleEditClassificationRule}
                onToggleEnabled={
                  handleToggleClassificationRule
                }
                onDelete={handleDeleteClassificationRule}
              />
            </section>
          ) : (
            <section className="mt-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">
                    커밋 규칙
                  </h3>

                  <p className="mt-1 text-sm text-slate-500">
                    커밋 메시지를 기준으로 type, category와
                    scope를 분류합니다.
                  </p>
                </div>

                {!showConventionForm && (
                  <button
                    type="button"
                    onClick={handleAddConventionRule}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white"
                  >
                    규칙 추가
                  </button>
                )}
              </div>

              {showConventionForm && (
                <ConventionRuleForm
                  editingRule={editingConventionRule}
                  submitting={submittingConventionRule}
                  onSubmit={handleSubmitConventionRule}
                  onCancelEdit={handleCancelConventionForm}
                />
              )}

              <ConventionRuleList
                rules={conventionRules}
                processingRuleId={
                  processingConventionRuleId
                }
                onEdit={handleEditConventionRule}
                onToggleEnabled={
                  handleToggleConventionRule
                }
                onDelete={handleDeleteConventionRule}
              />
            </section>
          )}
        </>
      )}
    </Layout>
  );
}

export default RuleManagementPage;
