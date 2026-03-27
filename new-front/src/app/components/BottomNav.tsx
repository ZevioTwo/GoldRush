import { NavLink, useLocation } from "react-router";
import { FileText, Compass, Gavel, BarChart3, User } from "lucide-react";

export function BottomNav() {
  const location = useLocation();
  const hidePaths = ["/contract", "/topup", "/create"];
  const shouldHide = hidePaths.some(path => location.pathname.startsWith(path));

  if (shouldHide) return null;

  const navItems = [
    { icon: FileText, label: "我的契约", path: "/" },
    { icon: Compass, label: "契约大厅", path: "/market" },
    { icon: BarChart3, label: "信誉排行", path: "/ranking" },
    { icon: User, label: "个人中心", path: "/profile" },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 glass z-50 shadow-[0_-5px_20px_rgba(0,0,0,0.03)] pb-safe">
      <div className="max-w-md mx-auto flex justify-around items-center h-[72px] px-1">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex flex-col items-center justify-center flex-1 transition-all duration-300 gap-1.5 ${
                isActive ? "text-primary scale-110 font-bold" : "text-muted-foreground hover:text-foreground"
              }`
            }
          >
            {({ isActive }) => (
              <>
                <item.icon size={22} strokeWidth={isActive ? 2.5 : 2} />
                <span className="text-[11px] leading-none">{item.label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
