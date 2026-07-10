function App() {
  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center px-6">
      <section className="w-full max-w-2xl rounded-2xl border border-slate-200 bg-white p-10 shadow-sm">
        <p className="text-sm font-medium text-slate-500">
          GitHub Learning Log Assistant
        </p>

        <h1 className="mt-3 text-4xl font-bold text-slate-900">
          Repoary
        </h1>

        <p className="mt-4 text-base leading-7 text-slate-600">
          GitHub 커밋과 변경 파일을 분석해 TIL 초안과 월별 README 행 생성을 돕는 서비스입니다.
        </p>

        <button className="mt-8 rounded-xl bg-slate-900 px-5 py-3 text-sm font-medium text-white">
          GitHub로 시작하기
        </button>
      </section>
    </main>
  );
}

export default App;