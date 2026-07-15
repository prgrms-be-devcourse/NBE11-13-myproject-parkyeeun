import { requestGitHubLoginUrl } from "../api/client";
import Layout from "../components/Layout";

function LoginPage() {
  const handleGitHubLogin = async () => {
    try {
      const loginUrl = await requestGitHubLoginUrl();
      window.location.href = loginUrl;
    } catch (error) {
      console.error(error);
      alert("GitHub 로그인 요청 중 오류가 발생했습니다.");
    }
  };

  return (
    <Layout>
      <button
        type="button"
        onClick={handleGitHubLogin}
        className="mt-8 rounded-xl bg-slate-900 px-5 py-3 text-sm font-medium text-white"
      >
        GitHub로 시작하기
      </button>
    </Layout>
  );
}

export default LoginPage;