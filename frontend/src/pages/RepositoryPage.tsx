import { useEffect, useState } from "react";
import {
  connectRepository,
  disconnectRepository,
  fetchConnectedRepositories,
  fetchGitHubRepositories,
  fetchMe,
  removeAccessToken,
} from "../api/client";
import Layout from "../components/Layout";
import type { ConnectedRepository, GitHubRepository, User } from "../types";

function RepositoryPage() {
  const [user, setUser] = useState<User | null>(null);
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [connectedRepositories, setConnectedRepositories] = useState<
    ConnectedRepository[]
  >([]);
  const [loading, setLoading] = useState(true);

  const handleLogout = () => {
    removeAccessToken();
    window.location.replace("/");
  };

  const handleConnectRepository = async (repository: GitHubRepository) => {
    try {
      const connectedRepository = await connectRepository(repository);

      setConnectedRepositories((prev) => {
        const exists = prev.some(
          (item) =>
            item.githubRepositoryId === connectedRepository.githubRepositoryId
        );

        if (exists) {
          return prev;
        }

        return [connectedRepository, ...prev];
      });
    } catch (error) {
      console.error(error);
      alert("저장소 연결 중 오류가 발생했습니다.");
    }
  };

  const handleDisconnectRepository = async (repository: GitHubRepository) => {
    try {
      await disconnectRepository(repository.id);

      setConnectedRepositories((prev) =>
        prev.filter(
          (connectedRepository) =>
            connectedRepository.githubRepositoryId !== repository.id
        )
      );
    } catch (error) {
      console.error(error);
      alert("저장소 연결 해제 중 오류가 발생했습니다.");
    }
  };

  useEffect(() => {
    const loadData = async () => {
      try {
        const [me, githubRepositories, connected] = await Promise.all([
          fetchMe(),
          fetchGitHubRepositories(),
          fetchConnectedRepositories(),
        ]);

        setUser(me);
        setRepositories(githubRepositories);
        setConnectedRepositories(connected);
      } catch (error) {
        console.error(error);
        removeAccessToken();
        window.location.replace("/");
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const isConnected = (repository: GitHubRepository) => {
    return connectedRepositories.some(
      (connectedRepository) =>
        connectedRepository.githubRepositoryId === repository.id
    );
  };

  return (
    <Layout>
      <div className="mt-8 flex items-center justify-between rounded-xl bg-green-50 px-5 py-3 text-sm text-green-700">
        <p>
          {user
            ? `${user.githubLogin} 계정으로 로그인되었습니다.`
            : "로그인 정보를 확인하는 중입니다."}
        </p>

        <button
          type="button"
          onClick={handleLogout}
          className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700"
        >
          로그아웃
        </button>
      </div>

      <div className="mt-8">
        <h2 className="text-xl font-semibold text-slate-900">
          분석할 저장소 선택
        </h2>

        <p className="mt-2 text-sm text-slate-500">
          GitHub 저장소 중 Repoary에서 학습 기록을 분석할 저장소를 연결합니다.
        </p>
      </div>

      {loading ? (
        <p className="mt-6 text-sm text-slate-500">
          저장소 목록을 불러오는 중입니다.
        </p>
      ) : (
        <ul className="mt-6 space-y-3">
          {repositories.map((repository) => (
            <li
              key={repository.id}
              className="flex items-center justify-between rounded-xl border border-slate-200 p-4"
            >
              <div>
                <p className="font-medium text-slate-900">
                  {repository.fullName}
                </p>

                <p className="mt-1 text-sm text-slate-500">
                  기본 브랜치: {repository.defaultBranch}
                  {repository.privateRepository ? " · private" : " · public"}
                </p>
              </div>

              {isConnected(repository) ? (
                <button
                  type="button"
                  onClick={() => handleDisconnectRepository(repository)}
                  className="rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700"
                >
                  연결 해제
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => handleConnectRepository(repository)}
                  className="rounded-lg bg-slate-900 px-4 py-2 text-sm font-medium text-white"
                >
                  연결
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </Layout>
  );
}

export default RepositoryPage;