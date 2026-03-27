import { useState } from "react";
import { Search, Plus, Sparkles, Filter, ChevronRight } from "lucide-react";
import { ContractCard } from "../components/ContractCard";
import { motion, AnimatePresence } from "motion/react";
import { useNavigate } from "react-router";

const mockContracts = [
  {
    id: "8829341",
    opponent: "张三",
    deposit: 500,
    remainingTime: "12:45:30",
    status: "ongoing" as const,
    creditPoints: 1200,
  },
  {
    id: "8829342",
    opponent: "李四",
    deposit: 200,
    status: "pending" as const,
    creditPoints: 85,
  },
  {
    id: "8829345",
    opponent: "王五",
    deposit: 1200,
    status: "arbitration" as const,
    creditPoints: 450,
  },
  {
    id: "8829301",
    opponent: "赵六",
    deposit: 300,
    status: "completed" as const,
    creditPoints: -10,
  },
];

export default function Home() {
  const [activeTab, setActiveTab] = useState("all");
  const [search, setSearch] = useState("");
  const navigate = useNavigate();

  const tabs = [
    { id: "all", label: "全部" },
    { id: "ongoing", label: "进行中" },
    { id: "pending", label: "待生效" },
    { id: "completed", label: "已完成" },
    { id: "arbitration", label: "仲裁中" },
  ];

  const filteredContracts = mockContracts.filter((c) => {
    if (activeTab === "all") return true;
    return c.status === activeTab;
  });

  return (
    <div className="pb-32 pt-8 px-6 max-w-md mx-auto min-h-screen bg-background">
      <header className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-black text-foreground tracking-tighter leading-tight">
          我的契约
        </h1>
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate("/create")}
          className="bg-[#fcc419] text-[#212529] px-6 py-3 rounded-full text-sm font-black flex items-center gap-2 shadow-lg shadow-[#fcc419]/20 hover:scale-105 transition-all"
        >
          <Plus size={18} strokeWidth={4} />
          发起契约
        </motion.button>
      </header>

      <div className="relative mb-8 group">
        <Search className="absolute left-5 top-1/2 -translate-y-1/2 text-muted-foreground group-focus-within:text-[#fcc419] transition-colors" size={20} />
        <input
          type="text"
          placeholder="搜索契约ID或对手昵称"
          className="w-full bg-[#f1f3f5] border-none rounded-2xl py-4.5 pl-14 pr-6 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-[#fcc419]/30 transition-all placeholder:text-muted-foreground/60"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="flex gap-6 mb-8 overflow-x-auto no-scrollbar py-2 -mx-2 px-2 border-b border-black/[0.03]">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`relative py-3 text-sm font-bold transition-all whitespace-nowrap ${
              activeTab === tab.id ? "text-foreground" : "text-muted-foreground"
            }`}
          >
            {tab.label}
            {activeTab === tab.id && (
              <motion.div
                layoutId="activeTabUnderline"
                className="absolute bottom-0 left-0 right-0 h-1 bg-[#fcc419] rounded-full"
              />
            )}
          </button>
        ))}
      </div>

      <div className="space-y-4">
        <AnimatePresence mode="popLayout">
          {filteredContracts.length > 0 ? (
            filteredContracts.map((contract) => (
              <motion.div
                key={contract.id}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{ duration: 0.25, ease: "easeOut" }}
              >
                <ContractCard {...contract} />
              </motion.div>
            ))
          ) : (
            <div className="py-24 text-center">
              <p className="text-muted-foreground font-medium">暂无相关契约记录</p>
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
