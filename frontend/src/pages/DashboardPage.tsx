import { useEffect, useState } from "react";
import type { ChangeEvent } from "react";
import { fetchConnectedRepositories } from "../api/client";
import Layout from "../components/Layout";
import type { ConnectedRepository } from "../types";

const SELECTED_REPOSITORY_KEY =
  "repoarySelectedConnectedRepositoryId";

function DashboardPage() {
  const [repositories, setRepositories] = useState<
    ConnectedRepository[]
  >([]);
  const [selectedRepositoryId, setSelectedRepositoryId] =
    useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  const selectedRepository =
    repositories.find(
      (repository) => repository.id === selectedRepositoryId,
    ) ?? null;

  const handleRepositoryChange = (
    event: ChangeEvent<HTMLSelectElement>,
  ) => {
    const repositoryId = Number(event.target.value);

    setSelectedRepositoryId(repositoryId);
    localStorage.setItem(
      SELECTED_REPOSITORY_KEY,
      String(repositoryId),
    );
  };

  useEffect(() => {
    const loadDashboard = async () => {
      setLoading(true);
      setErrorMessage("");

      try {
        const connectedRepositories =
          await fetchConnectedRepositories();

        setRepositories(connectedRepositories);

        if (connectedRepositories.length === 0) {
          window.location.replace("/repositories");
          return;
        }

        const savedRepositoryId = Number(
          localStorage.getItem(SELECTED_REPOSITORY_KEY),
        );

        const savedRepositoryExists =
          Number.isInteger(savedRepositoryId) &&
          connectedRepositories.some(
            (repository) =>
              repository.id === savedRepositoryId,
          );

        const initialRepositoryId = savedRepositoryExists
          ? savedRepositoryId
          : connectedRepositories[0].id;

        setSelectedRepositoryId(initialRepositoryId);
        localStorage.setItem(
          SELECTED_REPOSITORY_KEY,
          String(initialRepositoryId),
        );
      } catch (error) {
        setErrorMessage(
          error instanceof Error
            ? error.message
            : "대시보드를 불러오는 중 오류가 발생했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    void loadDashboard();
  }, []);

  return (
    <Layout>
      {errorMessage && (
        <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      )}

      {loading ? (
        <p className="text-sm text-slate-500">
          대시보드를 불러오는 중입니다.
        </p>
      ) : (
        <>
          <section>
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <h2 className="text-xl font-semibold text-slate-900">
                  오늘의 학습 기록
                </h2>

                <p className="mt-2 text-sm text-slate-500">
                  분석할 저장소를 선택하고 학습 기록을 관리합니다.
                </p>
              </div>

              <button
                type="button"
                onClick={() =>
                  window.location.assign("/repositories")
                }
                className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white"
              >
                저장소 관리
              </button>
            </div>
          </section>

          <section className="mt-6 rounded-xl border border-slate-200 p-5">
            <label
              htmlFor="repository"
              className="text-sm font-medium text-slate-700"
            >
              현재 저장소
            </label>

            <select
              id="repository"
              value={selectedRepositoryId ?? ""}
              onChange={handleRepositoryChange}
              className="mt-2 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900"
            >
              {repositories.map((repository) => (
                <option
                  key={repository.id}
                  value={repository.id}
                >
                  {repository.fullName}
                </option>
              ))}
            </select>

            {selectedRepository && (
              <p className="mt-2 text-sm text-slate-500">
                기본 브랜치:{" "}
                {selectedRepository.defaultBranch}
                {selectedRepository.privateRepository
                  ? " · private"
                  : " · public"}
              </p>
            )}
          </section>

          <section className="mt-6 grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              disabled={!selectedRepository}
              onClick={() => {
                if (!selectedRepository) {
                  return;
                }

                window.location.assign(
                  `/repositories/${selectedRepository.id}/rules`,
                );
              }}
              className="rounded-xl border border-slate-200 p-5 text-left transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <p className="font-semibold text-slate-900">
                규칙 관리
              </p>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                경로 규칙과 커밋 규칙을 확인하고 수정합니다.
              </p>
            </button>

            <button
              type="button"
              disabled
              className="rounded-xl border border-slate-200 p-5 text-left opacity-50"
            >
              <p className="font-semibold text-slate-900">
                커밋 분석
              </p>

              <p className="mt-2 text-sm leading-6 text-slate-500">
                날짜별 커밋 분석 화면은 다음 단계에서 연결합니다.
              </p>
            </button>
          </section>
        </>
      )}
    </Layout>
  );
}

export default DashboardPage;