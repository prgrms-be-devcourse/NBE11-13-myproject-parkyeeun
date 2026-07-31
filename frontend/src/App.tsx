import { getAccessToken } from "./api/client";
import CallbackPage from "./pages/CallbackPage";
import DashboardPage from "./pages/DashboardPage";
import LoginPage from "./pages/LoginPage";
import RepositoryPage from "./pages/RepositoryPage";
import RuleManagementPage from "./pages/RuleManagementPage";

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

function App() {
  const pathname = window.location.pathname;
  const accessToken = getAccessToken();

  if (pathname === "/auth/callback") {
    return <CallbackPage />;
  }

  if (!accessToken) {
    return <LoginPage />;
  }

  if (pathname === "/repositories") {
    return <RepositoryPage />;
  }

  const ruleRepositoryId = getRuleRepositoryId(pathname);

  if (ruleRepositoryId !== null) {
    return (
      <RuleManagementPage
        connectedRepositoryId={ruleRepositoryId}
      />
    );
  }

  return <DashboardPage />;
}

export default App;