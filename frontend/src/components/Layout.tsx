import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  clearAuthenticationStorage,
  fetchMe,
  getAccessToken,
} from "../api/client";
import type { User } from "../types";

type LayoutProps = {
  children: ReactNode;
};

function Layout({ children }: LayoutProps) {
  const accessToken = getAccessToken();
  const [user, setUser] = useState<User | null>(null);

  const handleHomeClick = () => {
    window.location.assign("/");
  };

  const handleLogout = () => {
    clearAuthenticationStorage();
    window.location.replace("/");
  };

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    const loadUser = async () => {
      try {
        const me = await fetchMe();
        setUser(me);
      } catch (error) {
        console.error(error);
      }
    };

    void loadUser();
  }, [accessToken]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100/70 px-4 py-6 sm:px-6">
      <section className="w-full max-w-6xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm font-medium text-slate-500">
              GitHub Learning Log Assistant
            </p>

            <button
              type="button"
              onClick={handleHomeClick}
              className="mt-3 block text-left"
              aria-label="Repoary 홈으로 이동"
            >
              <h1 className="text-4xl font-bold text-slate-900 transition-colors hover:text-slate-600">
                Repoary
              </h1>
            </button>
          </div>

          {accessToken && (
            <div className="flex items-center gap-3">
              <div className="text-right">
                <p className="mt-0.5 text-xs text-slate-400">
                  GitHub 계정
                </p>

                <p className="text-sm font-medium text-slate-700">
                  {user?.githubLogin ?? "GitHub 사용자"}
                </p>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md border border-red-200 bg-red-50 px-2.5 py-1.5 text-xs font-medium text-red-600 transition-colors hover:bg-red-100"              >
                로그아웃
              </button>
            </div>
          )}
        </div>

        <p className="mt-4 text-base leading-7 text-slate-600">
          GitHub 커밋과 변경 파일을 분석해 TIL 초안과 월별 README 행 생성을
          돕는 서비스입니다.
        </p>

        <div className="my-8 border-t border-slate-200" />

        {children}
      </section>
    </main>
  );
}

export default Layout;
