import { createBrowserRouter, Outlet } from "react-router";
import { Toaster } from "sonner";
import Home from "./pages/Home";
import Market from "./pages/Market";
import Profile from "./pages/Profile";
import Arbitration from "./pages/Arbitration";
import Ranking from "./pages/Ranking";
import CreateContract from "./pages/CreateContract";
import TopUp from "./pages/TopUp";
import ContractDetail from "./pages/ContractDetail";
import { BottomNav } from "./components/BottomNav";

function Root() {
  return (
    <div className="min-h-screen bg-background text-foreground selection:bg-primary selection:text-primary-foreground">
      <Toaster position="top-center" expand={false} richColors />
      <main className="mx-auto max-w-md bg-background relative overflow-x-hidden min-h-screen">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  );
}

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Home },
      { path: "market", Component: Market },
      { path: "arbitration", Component: Arbitration },
      { path: "ranking", Component: Ranking },
      { path: "profile", Component: Profile },
      { path: "create", Component: CreateContract },
      { path: "topup", Component: TopUp },
      { path: "contract/:id", Component: ContractDetail },
      { path: "*", Component: () => <div className="p-8 text-center text-muted-foreground mt-20">404 - 页面未找到</div> },
    ],
  },
]);
