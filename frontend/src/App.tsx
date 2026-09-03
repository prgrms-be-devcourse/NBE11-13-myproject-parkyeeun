import { getAccessToken } from "./api/client";
import CallbackPage from "./pages/CallbackPage";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import RepositoryPage from "./pages/RepositoryPage";
import RuleManagementPage from "./pages/RuleManagementPage";
import AnalysisPage from "./pages/AnalysisPage";
import TilEditorPage from "./pages/TilEditorPage";

const getRuleRepositoryId = (pathname: string) => {
  const match = pathname.match(
    /^\/repositories\/(\d+)\/rules$/,
  );

  if (!match) {
    return null;
  }

  const repositoryId = Number(match[1]);

  return Number.isInteger(repositoryId) && repositoryId > 0
    ? repositoryId
    : null;
};

const getAnalysisRepositoryId = (pathname: string) => {
  const match = pathname.match(
    /^\/repositories\/(\d+)\/analysis$/,
  );

  if (!match) {
    return null;
  }

  const repositoryId = Number(match[1]);

  return Number.isInteger(repositoryId) && repositoryId > 0
    ? repositoryId
    : null;
};

const getTilEditorIds = (pathname: string) => {
  const match = pathname.match(
    /^\/repositories\/(\d+)\/til\/(\d+)$/,
  );

  if (!match) {
    return null;
  }

  const connectedRepositoryId = Number(match[1]);
  const tilDocumentId = Number(match[2]);

  if (
    !Number.isInteger(connectedRepositoryId) ||
    connectedRepositoryId <= 0 ||
    !Number.isInteger(tilDocumentId) ||
    tilDocumentId <= 0
  ) {
    return null;
  }

  return { connectedRepositoryId, tilDocumentId };
};

function App() {
  const pathname = window.location.pathname;
  const accessToken = getAccessToken();

  if (pathname === "/auth/callback") {
    return <CallbackPage />;
  }

  if (!accessToken) {
    if (pathname !== "/") {
      window.location.replace("/");
    }

    return <LoginPage />;
  }

  if (pathname === "/repositories") {
    return <RepositoryPage />;
  }

  const tilEditorIds = getTilEditorIds(pathname);

  if (tilEditorIds !== null) {
    return <TilEditorPage {...tilEditorIds} />;
  }

  const ruleRepositoryId = getRuleRepositoryId(pathname);

  if (ruleRepositoryId !== null) {
    return (
      <RuleManagementPage
        connectedRepositoryId={ruleRepositoryId}
      />
    );
  }

  const analysisRepositoryId =
    getAnalysisRepositoryId(pathname);

  if (analysisRepositoryId !== null) {
    return (
      <AnalysisPage
        connectedRepositoryId={analysisRepositoryId}
      />
    );
  }

  return <DashboardPage />;
}

export default App;
