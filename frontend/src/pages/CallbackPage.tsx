import { useEffect } from "react";
import { saveAccessToken } from "../api/client";
import Layout from "../components/Layout";

function CallbackPage() {
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get("token");

    if (!token) {
      window.location.replace("/");
      return;
    }

    saveAccessToken(token);
    window.history.replaceState({}, "", "/repositories");
    window.location.replace("/repositories");
  }, []);

  return (
    <Layout>
      <p className="mt-8 rounded-xl bg-slate-50 px-5 py-3 text-sm text-slate-600">
        GitHub 로그인 처리 중입니다.
      </p>
    </Layout>
  );
}

export default CallbackPage;