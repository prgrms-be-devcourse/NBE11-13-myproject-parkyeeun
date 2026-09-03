import { useEffect, useState } from "react";
import {
  connectRepository,
  disconnectRepository,
  fetchConnectedRepositories,
  fetchGitHubRepositories,
} from "../api/client";
import Layout from "../components/Layout";
import type {
  ConnectedRepository,
  GitHubRepository,
} from "../types";

const SELECTED_REPOSITORY_KEY =
  "repoarySelectedConnectedRepositoryId";

function RepositoryPage() {
  const [repositories, setRepositories] = useState<
    GitHubRepository[]
  >([]);
  const [connectedRepositories, setConnectedRepositories] =
    useState<ConnectedRepository[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingRepositoryId, setProcessingRepositoryId] =
    useState<number | null>(null);

  const handleConnectRepository = async (
    repository: GitHubRepository,
  ) => {
    setProcessingRepositoryId(repository.id);

    try {
      const connectedRepository =
        await connectRepository(repository);

      setConnectedRepositories((prev) => {
        const exists = prev.some(
          (item) =>
            item.githubRepositoryId ===
            connectedRepository.githubRepositoryId,
        );

        if (exists) {
          return prev;
        }

        return [connectedRepository, ...prev];
      });
    } catch (error) {
      console.error(error);
      window.alert("저장소 연결 중 오류가 발생했습니다.");
    } finally {
      setProcessingRepositoryId(null);
    }
  };

  const handleDisconnectRepository = async (
    repository: GitHubRepository,
  ) => {
    const confirmed = window.confirm(
      `${repository.fullName} 저장소 연결을 해제하시겠습니까?\n\n연결을 해제하면 해당 저장소의 규칙과 분석 기능을 사용할 수 없습니다.`,
    );

    if (!confirmed) {
      return;
    }

    const disconnectedRepository =
      connectedRepositories.find(
        (connectedRepository) =>
          connectedRepository.githubRepositoryId ===
          repository.id,
      );

    setProcessingRepositoryId(repository.id);

    try {
      await disconnectRepository(repository.id);

      setConnectedRepositories((prev) =>
        prev.filter(
          (connectedRepository) =>
            connectedRepository.githubRepositoryId !==
            repository.id,
        ),
      );

      const selectedRepositoryId = localStorage.getItem(
        SELECTED_REPOSITORY_KEY,
      );

      if (
        disconnectedRepository &&
        selectedRepositoryId ===
          String(disconnectedRepository.id)
      ) {
        localStorage.removeItem(SELECTED_REPOSITORY_KEY);
      }
    } catch (error) {
      console.error(error);
      window.alert(
        "저장소 연결 해제 중 오류가 발생했습니다.",
      );
    } finally {
      setProcessingRepositoryId(null);
    }
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        const [githubRepositories, connected] =
          await Promise.all([
            fetchGitHubRepositories(),
            fetchConnectedRepositories(),
          ]);

        setRepositories(githubRepositories);
        setConnectedRepositories(connected);
      } catch (error) {
        console.error(error);
        window.alert(
          "저장소 목록을 불러오는 중 오류가 발생했습니다.",
        );
      } finally {
        setLoading(false);
      }
    };

    void loadData();
  }, []);

  const getConnectedRepository = (
    repository: GitHubRepository,
  ) => {
    return connectedRepositories.find(
      (connectedRepository) =>
        connectedRepository.githubRepositoryId ===
        repository.id,
    );
  };

  return (
    <Layout>
      <div>
        <h2 className="text-xl font-semibold text-slate-900">
          저장소 관리
        </h2>

        <p className="mt-2 text-sm text-slate-500">
          GitHub 저장소 중 Repoary에서 학습 기록을 분석할
          저장소를 연결합니다.
        </p>
      </div>

      {loading ? (
        <p className="mt-6 text-sm text-slate-500">
          저장소 목록을 불러오는 중입니다.
        </p>
      ) : repositories.length === 0 ? (
        <p className="mt-6 rounded-xl border border-dashed border-slate-300 px-4 py-8 text-center text-sm text-slate-500">
          조회할 수 있는 GitHub 저장소가 없습니다.
        </p>
      ) : (
        <ul className="mt-6 space-y-3">
          {repositories.map((repository) => {
            const connectedRepository =
              getConnectedRepository(repository);
            const processing =
              processingRepositoryId === repository.id;

            return (
              <li
                key={repository.id}
                className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 p-4"
              >
                <div className="min-w-0">
                  <p className="break-all font-medium text-slate-900">
                    {repository.fullName}
                  </p>

                  <p className="mt-1 text-sm text-slate-500">
                    기본 브랜치: {repository.defaultBranch}
                    {repository.privateRepository
                      ? " · private"
                      : " · public"}
                  </p>
                </div>

                {connectedRepository ? (
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() =>
                        window.location.assign(
                          `/repositories/${connectedRepository.id}/rules`,
                        )
                      }
                      disabled={processing}
                      className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      규칙 관리
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        void handleDisconnectRepository(
                          repository,
                        )
                      }
                      disabled={processing}
                      className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {processing
                        ? "해제 중..."
                        : "연결 해제"}
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() =>
                      void handleConnectRepository(repository)
                    }
                    disabled={processing}
                    className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {processing ? "연결 중..." : "연결"}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </Layout>
  );
}

export default RepositoryPage;
