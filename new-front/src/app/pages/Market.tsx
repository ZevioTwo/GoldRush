import { useState, useRef, useEffect } from "react";
import { Search, Bell, ChevronRight, User, Gamepad2, ShieldCheck, Clock, ChevronDown, Check } from "lucide-react";
import { useNavigate } from "react-router";
import { motion, AnimatePresence } from "motion/react";
import { ImageWithFallback } from "../components/figma/ImageWithFallback";

const jobs = [
  {
    title: "狙击手信条 - 连胜契约",
    game: "三角洲行动",
    boss: "摸金小王",
    credit: 98,
    price: "200.00",
    deposit: "50",
    remaining: "02:45:10",
    desc: "要求：需配合默契，不压力队友，目标今日10连胜，败场由发起人全额赔付。",
    status: "进行中",
    image: "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop",
    id: "MJK-001"
  },
  {
    title: "DNF 团本金牌代打契约",
    game: "DNF",
    boss: "凯丽的邻居",
    credit: 95,
    price: "150.00",
    deposit: "30",
    remaining: "05:12:00",
    desc: "高端本包通关，若未达成目标，全额退还契约金并额外补偿。",
    status: "待开始",
    image: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=2165&auto=format&fit=crop",
    id: "MJK-002"
  },
  {
    title: "王者荣耀 - 巅峰赛保分",
    game: "王者荣耀",
    boss: "野王求带",
    credit: 92,
    price: "300.00",
    deposit: "60",
    remaining: "01:20:45",
    desc: "巅峰2000分段求稳健边路，诚信契约，输赢共同承担，信誉保证。",
    status: "进行中",
    image: "https://images.unsplash.com/photo-1511512578047-dfb367046420?q=80&w=2071&auto=format&fit=crop",
    id: "MJK-003"
  }
];

const GAMES = [
  "全部", "三角洲行动", "DNF", "王者荣耀", "和平精英", "绝地求生", "永劫无间", 
  "英雄联盟", "金铲铲之战", "原神", "无畏契约", "永劫无间手游", "魔兽世界",
  "CS2", "逃离塔科夫", "暗区突围"
];

export default function Market() {
  const navigate = useNavigate();
  const [activeCategory, setActiveCategory] = useState("全部");
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);

  const filteredGames = GAMES.filter(game => 
    game.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredJobs = jobs.filter(job => 
    activeCategory === "全部" || job.game === activeCategory
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      setSearchTerm("");
    };
  }, [isDropdownOpen]);

  return (
    <div className="pb-32 pt-6 px-4 max-w-md mx-auto min-h-screen bg-background">
      <header className="flex justify-between items-center mb-6">
        <div className="bg-[#fcf8e8] p-2 rounded-full">
           <Search size={20} className="text-[#d9a42b]" />
        </div>
        <h1 className="text-lg font-black text-foreground tracking-tight">契约大厅</h1>
        <div className="bg-[#fcf8e8] p-2 rounded-full relative">
           <Bell size={20} className="text-[#d9a42b]" />
           <span className="absolute top-1 right-1 w-2.5 h-2.5 bg-destructive rounded-full border-2 border-background" />
        </div>
      </header>

      {/* Game Selection Dropdown with Search */}
      <div className="relative mb-6" ref={dropdownRef}>
        <div 
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          className={`w-full bg-white border rounded-2xl py-3.5 px-5 flex items-center justify-between cursor-pointer transition-all shadow-sm ${isDropdownOpen ? 'border-[#d9a42b] ring-1 ring-[#d9a42b]/10' : 'border-black/[0.05]'}`}
        >
          <div className="flex items-center gap-2.5">
            <Gamepad2 size={18} className={activeCategory !== '全部' ? 'text-[#d9a42b]' : 'text-[#adb5bd]'} />
            <span className="text-[14px] font-bold text-[#212529]">
              {activeCategory === '全部' ? '选择游戏类型' : activeCategory}
            </span>
          </div>
          <ChevronDown 
            size={18} 
            className={`text-[#adb5bd] transition-transform duration-300 ${isDropdownOpen ? 'rotate-180 text-[#d9a42b]' : ''}`} 
          />
        </div>

        <AnimatePresence>
          {isDropdownOpen && (
            <motion.div
              initial={{ opacity: 0, y: 5, scale: 0.98 }}
              animate={{ opacity: 1, y: 5, scale: 1 }}
              exit={{ opacity: 0, y: 5, scale: 0.98 }}
              className="absolute z-50 w-full bg-white border border-[#e9ecef] rounded-2xl shadow-2xl overflow-hidden mt-2 shadow-black/10"
            >
              {/* Search Input */}
              <div className="p-3 border-b border-[#f1f3f5] sticky top-0 bg-white z-10">
                <div className="relative">
                  <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#adb5bd]" />
                  <input
                    autoFocus
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder="搜索游戏..."
                    className="w-full bg-[#f8f9fa] border-none rounded-xl py-2.5 pl-9 pr-4 text-[13px] font-medium outline-none placeholder:text-[#adb5bd]"
                  />
                </div>
              </div>

              {/* List */}
              <div className="p-1.5 max-h-64 overflow-y-auto custom-scrollbar">
                {filteredGames.length > 0 ? (
                  filteredGames.map((game) => (
                    <div
                      key={game}
                      onClick={() => {
                        setActiveCategory(game);
                        setIsDropdownOpen(false);
                        setSearchTerm("");
                      }}
                      className={`flex items-center justify-between px-4 py-3 rounded-xl cursor-pointer transition-colors mb-0.5 last:mb-0 ${
                        activeCategory === game 
                          ? 'bg-[#fff9db] text-[#d9a42b]' 
                          : 'hover:bg-[#f8f9fa] text-[#495057]'
                      }`}
                    >
                      <span className="text-[14px] font-bold">{game}</span>
                      {activeCategory === game && (
                        <Check size={16} strokeWidth={3} />
                      )}
                    </div>
                  ))
                ) : (
                  <div className="py-10 text-center text-[#adb5bd] text-[13px] font-medium">
                    未搜索到相关游戏
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className="space-y-6">
        {filteredJobs.length > 0 ? (
          filteredJobs.map((job) => (
            <motion.div
              key={job.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              whileTap={{ scale: 0.98 }}
              className="bg-white rounded-[32px] overflow-hidden shadow-[0_4px_25px_rgba(0,0,0,0.03)] border border-black/5"
            >
              <div className="h-44 relative">
                <ImageWithFallback src={job.image} alt={job.title} className="w-full h-full object-cover" />
                <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
              </div>
              
              <div className="p-6">
                <div className="flex justify-between items-start mb-4">
                  <h2 className="text-[17px] font-black text-foreground leading-tight">{job.title}</h2>
                  <span className={`px-2.5 py-1 rounded-lg text-[10px] font-bold ${job.status === '进行中' ? 'bg-[#e7f9ee] text-[#22c55e]' : 'bg-[#fff9db] text-[#d9a42b]'}`}>
                    {job.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-y-3 mb-5">
                  <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-medium">
                    <User size={14} className="text-[#d9a42b]" />
                    <span>发起人：{job.boss}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-medium">
                    <Gamepad2 size={14} className="text-[#d9a42b]" />
                    <span>游戏：{job.game}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-medium">
                    <ShieldCheck size={14} className="text-[#d9a42b]" />
                    <span>信誉分门槛：<span className="text-[#d9a42b] font-black">{job.credit}</span></span>
                  </div>
                  <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-medium">
                    <Clock size={14} className="text-muted-foreground/60" />
                    <span>剩 {job.remaining}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between mb-6 bg-[#fcf8e8]/30 p-4 rounded-2xl border border-[#d9a42b]/5">
                  <div className="flex flex-col">
                    <span className="text-[10px] text-[#d9a42b]/60 font-black uppercase tracking-wider mb-0.5">契约信用保障</span>
                    <span className="text-[13px] font-bold text-muted-foreground">违约即扣除对应信誉分</span>
                  </div>
                  <div className="flex flex-col items-end">
                    <span className="text-[10px] text-[#d9a42b]/60 font-black uppercase tracking-wider mb-0.5">需押信誉分</span>
                    <span className="text-[18px] font-black text-[#d9a42b] tracking-tighter">{job.deposit} 信誉分</span>
                  </div>
                </div>

                <p className="text-[13px] text-muted-foreground leading-relaxed mb-6 font-medium bg-[#f8f9fa] p-4 rounded-2xl border border-black/[0.02]">
                  {job.desc}
                </p>

                <button 
                  onClick={() => navigate(`/contract/${job.id}`)}
                  className="w-full bg-[#d9a42b] hover:bg-[#c49221] text-white py-4 rounded-2xl text-[15px] font-black transition-all flex items-center justify-center gap-2 group shadow-lg shadow-[#d9a42b]/20"
                >
                  查看详情
                  <ChevronRight size={18} className="group-hover:translate-x-1 transition-transform" />
                </button>
              </div>
            </motion.div>
          ))
        ) : (
          <div className="py-20 text-center">
            <div className="w-20 h-20 bg-[#f8f9fa] rounded-full flex items-center justify-center mx-auto mb-4 border border-black/[0.02]">
              <Gamepad2 size={32} className="text-[#adb5bd]" />
            </div>
            <p className="text-muted-foreground font-bold">该类别下暂无活跃契约</p>
          </div>
        )}
      </div>
    </div>
  );
}
