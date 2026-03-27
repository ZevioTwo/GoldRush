import { ShieldCheck, TrendingUp, Info, ChevronRight, User, Award, ShieldAlert } from "lucide-react";
import { motion } from "motion/react";

const redList = [
  { rank: "01", name: "财富猎人·王", score: 998, highlight: true },
  { rank: "02", name: "守信商会A", score: 985 },
  { rank: "03", name: "金诚契约", score: 980 },
  { rank: "04", name: "林某某", score: 975 },
  { rank: "05", name: "极客摸金", score: 970 },
  { rank: "06", name: "信义天成", score: 965 },
  { rank: "07", name: "陈大发", score: 960 },
  { rank: "08", name: "北极星契约", score: 955 },
  { rank: "09", name: "诚信工作室", score: 950 },
  { rank: "10", name: "徐先生", score: 945 },
];

const blackList = [
  { rank: "垫底1", name: "失信者·无名", score: 210, highlight: true },
  { rank: "02", name: "影子猎手", score: 245 },
  { rank: "03", name: "违约黑洞", score: 280 },
  { rank: "04", name: "逃债专家", score: 315 },
  { rank: "05", name: "破产边缘", score: 350 },
  { rank: "06", name: "李某发", score: 390 },
  { rank: "07", name: "暗夜摸金", score: 420 },
  { rank: "08", name: "虚假声明", score: 450 },
  { rank: "09", name: "契约漏洞", score: 480 },
  { rank: "10", name: "赵六子", score: 510 },
];

export default function Ranking() {
  return (
    <div className="pb-32 pt-10 px-6 max-w-md mx-auto min-h-screen bg-background">
      <header className="text-center mb-10 relative">
        <h1 className="text-2xl font-black text-[#212529] tracking-tight">信誉排行榜</h1>
        <div className="absolute top-1 right-0 text-muted-foreground/30 hover:text-[#fcc419] transition-colors">
          <Info size={20} />
        </div>
      </header>

      <div className="bg-[#fff9db]/50 border border-[#fcc419]/10 rounded-[32px] p-6 mb-10 shadow-[0_10px_30px_rgba(252,196,25,0.05)] flex justify-between items-center">
        <div>
           <span className="text-[10px] text-muted-foreground font-black uppercase tracking-wider mb-1 block opacity-60">更新时间</span>
           <span className="text-[15px] font-black text-[#212529]">2026年10月24日 14:30</span>
        </div>
        <div className="bg-[#fcc419] p-2 rounded-xl shadow-lg shadow-[#fcc419]/20">
           <ShieldCheck size={24} className="text-white" />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-8 relative">
        <div className="absolute top-0 bottom-0 left-1/2 w-[1px] bg-[#f1f3f5] -translate-x-1/2" />
        
        {/* Red List */}
        <div className="space-y-6">
          <div className="flex items-center gap-2 mb-8 border-b-4 border-[#fcc419] pb-3 rounded-sm">
             <TrendingUp size={22} className="text-[#fcc419]" strokeWidth={3} />
             <h2 className="text-lg font-black text-[#212529]">红榜</h2>
          </div>

          <div className="space-y-4">
            {redList.map((item, idx) => (
              <motion.div
                key={item.name}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                className={`p-3 rounded-xl transition-all ${
                  item.highlight ? "bg-[#fcf8e8] ring-1 ring-[#fcc419]/30 shadow-md" : "bg-white border border-transparent hover:border-[#fcc419]/10"
                }`}
              >
                <div className="flex justify-between items-center mb-1.5">
                  <span className={`text-[10px] font-black uppercase tracking-widest ${item.highlight ? 'text-[#d9a42b]' : 'text-muted-foreground/40 font-mono'}`}>
                    {item.highlight ? 'TOP 1' : item.rank}
                  </span>
                  <span className={`text-[11px] font-black tabular-nums ${item.highlight ? 'text-[#d9a42b]' : 'text-[#fcc419] opacity-70'}`}>
                    {item.score}分
                  </span>
                </div>
                <h3 className={`text-[13px] font-black ${item.highlight ? 'text-[#212529]' : 'text-muted-foreground'}`}>
                  {item.name}
                </h3>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Black List */}
        <div className="space-y-6">
          <div className="flex items-center gap-2 mb-8 border-b-4 border-[#212529] pb-3 rounded-sm">
             <ShieldAlert size={22} className="text-[#212529]" strokeWidth={3} />
             <h2 className="text-lg font-black text-[#212529]">黑榜</h2>
          </div>

          <div className="space-y-4">
            {blackList.map((item, idx) => (
              <motion.div
                key={item.name}
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.05 }}
                className={`p-3 rounded-xl transition-all ${
                  item.highlight ? "bg-[#1a1a1e] ring-1 ring-white/10 shadow-lg" : "bg-white border border-transparent hover:border-black/5"
                }`}
              >
                <div className="flex justify-between items-center mb-1.5">
                  <span className={`text-[10px] font-black uppercase tracking-widest ${item.highlight ? 'text-[#fa5252] bg-[#fa5252]/10 px-1.5 py-0.5 rounded-sm' : 'text-muted-foreground/40 font-mono'}`}>
                    {item.highlight ? '垫底 1' : item.rank}
                  </span>
                  <span className={`text-[11px] font-black tabular-nums ${item.highlight ? 'text-[#fa5252]' : 'text-[#fa5252]/60'}`}>
                    {item.score}分
                  </span>
                </div>
                <h3 className={`text-[13px] font-black ${item.highlight ? 'text-white' : 'text-muted-foreground/60'}`}>
                  {item.name}
                </h3>
              </motion.div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-12 text-center">
        <p className="text-[10px] text-muted-foreground/30 tracking-tight font-medium uppercase">
          诚信分由多维度契约达成率、用户评价及官方核查综合评定
        </p>
      </div>
    </div>
  );
}
