import { getAccessToken } from "./api/client";
import CallbackPage from "./pages/CallbackPage";
import LoginPage from "./pages/LoginPage";
import RepositoryPage from "./pages/RepositoryPage";

function App() {
  const pathname = window.location.pathname;
  const accessToken = getAccessToken();

  if (pathname === "/auth/callback") {
    return <CallbackPage />;
  }

  if (accessToken) {
    return <RepositoryPage />;
  }

  return <LoginPage />;
}

export default App;