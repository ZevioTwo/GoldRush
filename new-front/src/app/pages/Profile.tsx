import { useState } from "react";
import { ShieldCheck, TrendingUp, BarChart3, Settings, ChevronRight, LogOut, Wallet, Gift, Bell, Info, Gavel, CheckCircle2, Calendar, ArrowRightLeft } from "lucide-react";
import { motion } from "motion/react";
import { useNavigate } from "react-router";
import { ImageWithFallback } from "../components/figma/ImageWithFallback";
import { toast } from "sonner";

export default function Profile() {
  const navigate = useNavigate();
  const [mojinValue, setMojinValue] = useState({ total: 12500, locked: 450 });
  const [creditPoints, setCreditPoints] = useState(985);
  const [checkInDays, setCheckInDays] = useState(3);
  const [checkedToday, setCheckedToday] = useState(false);

  const getCreditLevel = (pts: number) => {
    if (pts <= 0) return { label: "无保户", color: "text-gray-400", bg: "bg-gray-100", canPublish: false };
    if (pts < 100) return { label: "低保户", color: "text-blue-500", bg: "bg-blue-50", canPublish: true };
    if (pts < 1000) return { label: "高保户", color: "text-[#d9a42b]", bg: "bg-[#fff9db]", canPublish: true };
    return { label: "特保户", color: "text-[#d9a42b]", bg: "bg-[#fff9db]", isSpecial: true, canPublish: true };
  };

  const handleCheckIn = () => {
    if (checkedToday) return;
    const rewards = [2, 4, 6, 8, 10, 12, Math.floor(Math.random() * 9999)];
    const reward = rewards[checkInDays % 7];
    setMojinValue(prev => ({ ...prev, locked: prev.locked + reward }));
    setCheckedToday(true);
    setCheckInDays(prev => prev + 1);
    toast.success(`签到成功！获得 ${reward} 摸金值 (不可赠送)`);
  };

  const stats = [
    { label: "进行中契约", value: "12", color: "text-blue-500" },
    { label: "已完成总数", value: "156", color: "text-[#22c55e]" },
    { label: "履约胜率", value: "100%", color: "text-[#d9a42b]" },
  ];

  const menuItems = [
    { icon: Gavel, label: "仲裁中心", desc: "处理契约纠纷与申诉", path: "/arbitration" },
    { icon: Wallet, label: "我的信誉分记录", desc: "保证金与酬劳流水", path: "/credits" },
    { icon: BarChart3, label: "契约统计报告", desc: "近期契约执行情况", path: "/stats" },
    { icon: ShieldCheck, label: "账号与安全", desc: "实名认证与密码设置", path: "/security" },
    { icon: Bell, label: "消息通知设置", desc: "推送通知开关", path: "/notifications" },
    { icon: Info, label: "关于摸金小契", desc: "平台介绍与服务协议", path: "/about" },
  ];

  const levelData = getCreditLevel(creditPoints);

  return (
    <div className="pb-32 pt-10 px-6 max-w-md mx-auto min-h-screen bg-background relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 right-0 w-64 h-64 bg-[#fcf8e8]/50 rounded-full -mr-32 -mt-32 blur-3xl -z-10" />
      
      <header className="flex flex-col items-center mb-10 text-center">
        <div className="relative mb-4">
          <div className="w-24 h-24 rounded-full overflow-hidden border-4 border-white shadow-2xl ring-4 ring-[#fcf8e8] relative group">
            <ImageWithFallback 
              src="https://images.unsplash.com/photo-1639240445146-9bdf576c11bb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxmYW50YXN5JTIwY2hhcmFjdGVyJTIwYXZhdGFyJTIwZ2FtaW5nJTIwYm9zcyUyMG1lcmNlbmFyeSUyMHBvcnRyYWl0JTIwcG9ydHJhaXQtZGFyay1iYWNrZ3JvdW5kfGVufDF8fHx8MTc3NDMyNDExNHww&ixlib=rb-4.1.0&q=80&w=1080&utm_source=figma&utm_medium=referral" 
              alt="Profile" 
              className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" 
            />
          </div>
          <div className="absolute -bottom-1 -right-1 bg-[#d9a42b] p-1.5 rounded-full border-2 border-white shadow-lg">
            <ShieldCheck size={16} className="text-white" />
          </div>
        </div>
        <h1 className="text-2xl font-black text-foreground tracking-tight flex items-center gap-2">
          张晓明
          <span className={`text-[10px] px-2 py-0.5 rounded-full font-black uppercase ${levelData.bg} ${levelData.color} border border-black/5`}>
            {levelData.label}
          </span>
        </h1>
        <p className="text-muted-foreground text-[14px] font-medium mt-1 uppercase tracking-widest font-mono">
          ID: 8888 6666
        </p>
      </header>

      {/* Credit Section */}
      <section className="bg-white rounded-[32px] p-7 shadow-[0_15px_45px_rgba(0,0,0,0.04)] border border-black/5 mb-8 overflow-hidden relative">
        <div className="flex justify-between items-start mb-8 relative z-10">
          <div className="flex flex-col">
            <span className="text-[11px] text-muted-foreground font-black uppercase tracking-widest mb-1 opacity-50">摸金信誉分数</span>
            <div className="flex items-center gap-2.5">
               <span className="text-5xl font-black text-[#212529] tracking-tighter tabular-nums">985</span>
               <div className="bg-[#fff9db] p-1.5 rounded-xl border border-[#d9a42b]/10">
                 <ShieldCheck size={20} className="text-[#d9a42b]" strokeWidth={2.5} />
               </div>
            </div>
          </div>
          <div className="text-right">
            <span className="text-[11px] text-muted-foreground font-black uppercase tracking-widest mb-1 opacity-50 block">摸金值余额</span>
            <span className="text-[20px] font-black text-foreground block">12,500</span>
            <span className="text-[10px] text-[#d9a42b] font-bold bg-[#fcf8e8] px-1.5 py-0.5 rounded-lg border border-[#d9a42b]/5">
              含锁定 {mojinValue.locked}
            </span>
          </div>
        </div>
        
        {/* Check-in Ladder */}
        <div className="mb-8 p-5 bg-[#f8f9fa] rounded-3xl border border-black/[0.02]">
           <div className="flex justify-between items-center mb-4">
              <span className="text-[12px] font-black text-[#212529]">7日签到福利</span>
              <span className="text-[10px] font-bold text-muted-foreground">已累计: {checkInDays}天</span>
           </div>
           <div className="flex justify-between gap-1.5 mb-5">
              {[2, 4, 6, 8, 10, 12, "???"].map((val, i) => (
                <div key={i} className="flex-1 flex flex-col items-center gap-1.5">
                   <div className={`w-full aspect-square rounded-xl flex items-center justify-center text-[10px] font-black border transition-all ${
                     i < checkInDays % 7 
                      ? 'bg-[#d9a42b] border-[#d9a42b] text-white' 
                      : i === checkInDays % 7 && !checkedToday 
                        ? 'bg-[#fff9db] border-[#d9a42b]/20 text-[#d9a42b] animate-pulse' 
                        : 'bg-white border-black/5 text-muted-foreground/30'
                   }`}>
                      {i === 6 ? <Gift size={14} /> : val}
                   </div>
                   <span className="text-[9px] font-bold text-muted-foreground/40 uppercase">D{i+1}</span>
                </div>
              ))}
           </div>
           <button 
             onClick={handleCheckIn}
             disabled={checkedToday}
             className={`w-full py-3.5 rounded-2xl text-[13px] font-black flex items-center justify-center gap-2 shadow-sm transition-all ${
               checkedToday 
                ? 'bg-[#f1f3f5] text-muted-foreground border border-black/5 cursor-not-allowed' 
                : 'bg-white hover:bg-[#fff9db] text-[#d9a42b] border border-[#d9a42b]/20 hover:scale-[1.02]'
             }`}
           >
              {checkedToday ? <CheckCircle2 size={16} /> : <Calendar size={16} />}
              {checkedToday ? '今日已签' : '签到领取锁定摸金值'}
           </button>
        </div>

        <div className="flex gap-4">
           <button 
             onClick={() => navigate('/topup')}
             className="flex-1 bg-[#d9a42b] text-white py-4.5 rounded-[22px] text-sm font-black flex items-center justify-center gap-2 shadow-lg shadow-[#d9a42b]/20 hover:scale-[1.02] transition-all"
           >
              <TrendingUp size={18} />
              摸金充值
           </button>
           <button className="flex-1 bg-white text-[#d9a42b] py-4.5 rounded-[22px] text-sm font-black flex items-center justify-center gap-2 border border-[#d9a42b]/10 hover:scale-[1.02] hover:bg-[#fcf8e8] transition-all">
              <ArrowRightLeft size={18} />
              金兑信誉
           </button>
        </div>
      </section>

      {/* Stats Section */}
      <div className="grid grid-cols-3 gap-1 mb-8 bg-white/50 p-1 rounded-3xl border border-black/5">
        {stats.map((stat) => (
          <div
            key={stat.label}
            className="p-5 rounded-2xl text-center bg-white shadow-sm border border-black/5"
          >
            <span className="text-xl font-black text-[#212529] block mb-1 tracking-tighter tabular-nums">{stat.value}</span>
            <span className="text-[11px] text-muted-foreground/80 font-bold whitespace-nowrap">{stat.label}</span>
          </div>
        ))}
      </div>

      {/* Menu Section */}
      <div className="bg-white border border-black/5 rounded-[32px] overflow-hidden shadow-[0_5px_30px_rgba(0,0,0,0.02)] mb-10">
        {menuItems.map((item, index) => (
          <motion.button
            key={item.label}
            whileHover={{ backgroundColor: "#fcf8e8" }}
            onClick={() => item.path && navigate(item.path)}
            className={`w-full flex items-center justify-between p-6 text-left group transition-all ${
              index !== menuItems.length - 1 ? "border-b border-black/[0.03]" : ""
            }`}
          >
            <div className="flex gap-4 items-center">
              <div className="w-10 h-10 rounded-2xl bg-[#f1f3f5] flex items-center justify-center text-muted-foreground group-hover:bg-white group-hover:text-[#d9a42b] transition-all border border-transparent group-hover:border-[#d9a42b]/10">
                <item.icon size={20} />
              </div>
              <h3 className="text-[15px] font-bold text-[#212529]">{item.label}</h3>
            </div>
            <ChevronRight size={18} className="text-muted-foreground/30 group-hover:text-[#d9a42b] transition-colors" />
          </motion.button>
        ))}
      </div>

      <button className="w-full mb-8 flex items-center justify-center gap-2 py-5 rounded-3xl text-destructive text-[15px] font-black hover:bg-destructive/5 transition-all border border-black/5 group">
        <LogOut size={20} className="group-hover:-translate-x-1 transition-transform" />
        退出当前账号
      </button>
    </div>
  );
}
