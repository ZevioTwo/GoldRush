import { Calendar, ChevronRight, Clock, UserCheck, ShieldAlert, CheckCircle2, Sparkles } from "lucide-react";
import { motion } from "motion/react";
import { useNavigate } from "react-router";

interface ContractCardProps {
  id: string;
  opponent: string;
  deposit: number;
  time?: string;
  remainingTime?: string;
  status: "ongoing" | "pending" | "completed" | "arbitration";
  creditPoints?: number;
}

export function ContractCard({ id, opponent, deposit, remainingTime, status, creditPoints = 100 }: ContractCardProps) {
  const navigate = useNavigate();

  const getCreditLabel = (pts: number) => {
    if (pts <= 0) return { label: "无保户", color: "text-gray-400", bg: "bg-gray-100" };
    if (pts < 100) return { label: "低保户", color: "text-blue-500", bg: "bg-blue-50" };
    if (pts < 1000) return { label: "高保户", color: "text-[#d9a42b]", bg: "bg-[#fff9db]" };
    return { label: "特保户", color: "text-[#d9a42b]", bg: "bg-[#fff9db]", isSpecial: true };
  };

  const creditLabel = getCreditLabel(creditPoints);

  const statusConfig = {
    ongoing: {
      label: "进行中",
      bg: "bg-[#fff9db]",
      text: "text-[#d9a42b]",
      icon: Clock,
      footer: `剩余时间: ${remainingTime || "12:45:30"}`,
      footerIcon: Clock,
    },
    pending: {
      label: "待生效",
      bg: "bg-[#e7f5ff]",
      text: "text-[#339af0]",
      icon: UserCheck,
      footer: "等待对方确认",
      footerIcon: UserCheck,
    },
    arbitration: {
      label: "仲裁中",
      bg: "bg-[#fff5f5]",
      text: "text-[#fa5252]",
      icon: ShieldAlert,
      footer: "官方正在介入处理",
      footerIcon: ShieldAlert,
    },
    completed: {
      label: "已完成",
      bg: "bg-[#f1f3f5]",
      text: "text-[#868e96]",
      icon: CheckCircle2,
      footer: "契约已圆满结束",
      footerIcon: CheckCircle2,
      actionLabel: "查看记录"
    },
  };

  const config = statusConfig[status];
  const Icon = config.icon;
  const FooterIcon = config.footerIcon;

  return (
    <motion.div
      whileTap={{ scale: 0.98 }}
      onClick={() => navigate(`/contract/${id}?view=mine`)}
      className="bg-card rounded-[24px] p-6 mb-4 shadow-sm border border-black/5 hover:shadow-md transition-all duration-300 cursor-pointer"
    >
      <div className="flex justify-between items-center mb-4">
        <div className="flex items-center gap-3">
          <span className={`px-2.5 py-0.5 rounded-md text-[12px] font-bold ${config.bg} ${config.text}`}>
            {config.label}
          </span>
          <div className="flex items-center gap-1.5">
            <span className={`px-2 py-0.5 rounded-md text-[10px] font-black border uppercase tracking-wider flex items-center gap-1 ${creditLabel.bg} ${creditLabel.color} border-black/5`}>
              {creditLabel.isSpecial && <Sparkles size={10} className="text-[#d9a42b]" fill="currentColor" />}
              {creditLabel.label}
            </span>
            <span className="text-muted-foreground text-[10px] font-mono opacity-40">ID: {id}</span>
          </div>
        </div>
        <span className="text-muted-foreground text-[12px]">押信誉分</span>
      </div>

      <div className="flex justify-between items-start mb-6">
        <h3 className="text-[18px] font-bold text-foreground/90 tracking-tight leading-tight">
          与 <span className="text-foreground">{opponent}</span> 的契约
        </h3>
        <span className="text-[20px] font-black text-foreground tabular-nums tracking-tighter">
          {deposit.toLocaleString()} 信誉分
        </span>
      </div>

      <div className="pt-4 border-t border-black/[0.03] flex justify-between items-center">
        <div className="flex items-center gap-2 text-[13px] text-muted-foreground font-medium">
          <FooterIcon size={16} />
          <span>{config.footer}</span>
        </div>
        <button className="flex items-center gap-1 text-[13px] font-bold text-[#fcc419] hover:opacity-80 transition-opacity">
          {config.actionLabel || "查看详情"}
          <ChevronRight size={16} />
        </button>
      </div>
    </motion.div>
  );
}
