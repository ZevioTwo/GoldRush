import { useState, useEffect } from "react";
import { useParams, useNavigate, useSearchParams } from "react-router";
import { 
  ChevronLeft, 
  ShieldCheck, 
  Clock, 
  User, 
  Gamepad2, 
  FileText, 
  CheckCircle2, 
  AlertCircle, 
  MessageSquare, 
  Share2, 
  Gavel,
  History,
  ArrowRightLeft,
  Gift,
  X,
  Sparkles
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { ImageWithFallback } from "../components/figma/ImageWithFallback";
import { toast } from "sonner";

// Mock data fetching based on ID
const getContractData = (id: string | undefined) => {
  const allData: Record<string, any> = {
    "MJK-001": {
      title: "狙击手信条 - 连胜契约",
      game: "三角洲行动",
      boss: "摸金小王",
      bossAvatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=1000&auto=format&fit=crop",
      credit: 98,
      remaining: "02:45:10",
      desc: "要求：需配合默契，不压力队友，目标今日10连胜，败场由发起人全额赔付。打手需具备大师以上段位证明，开局前需验证身份。",
      status: "进行中",
      price: "200.00",
      deposit: "50",
      terms: [
        "打手必须保证账号安全，不得使用任何外挂插件",
        "连续两场表现不佳（评分低于6.0）发起人有权单方面解除契约",
        "完成目标后，契约金将在24小时内通过平台托管释放",
        "如遇系统断开连接，需在5分钟内重连，否则视为违约"
      ],
      image: "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop"
    },
    "MJK-002": {
      title: "DNF 团本金牌代打契约",
      game: "DNF",
      boss: "凯丽的邻居",
      bossAvatar: "https://images.unsplash.com/photo-1599566150163-29194dcaad36?q=80&w=1000&auto=format&fit=crop",
      credit: 95,
      remaining: "05:12:00",
      desc: "高端本包通关，若未达成目标，全额退还契约金并额外补偿。需要名望5.0以上，熟悉打法机制。",
      status: "待开始",
      price: "150.00",
      deposit: "30",
      terms: [
        "包通关，若因为打手失误导致翻车，全额退款",
        "翻车赔付金为契约金额的20%",
        "打手需全程开启屏幕录制作为结案证据"
      ],
      image: "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?q=80&w=2165&auto=format&fit=crop"
    },
    "8829341": {
        title: "狙击手信条 - 连胜契约",
        game: "三角洲行动",
        boss: "摸金小王",
        bossAvatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=1000&auto=format&fit=crop",
        credit: 98,
        remaining: "02:45:10",
        desc: "要求：需配合默契，不压力队友，目标今日10连胜，败场由发起人全额赔付。打手需具备大师以上段位证明，开局前需验证身份。",
        status: "进行中",
        price: "200.00",
        deposit: "50",
        terms: [
          "打手必须保证账号安全，不得使用任何外挂插件",
          "连续两场表现不佳（评分低于6.0）发起人有权单方面解除契约",
          "完成目标后，契约金将在24小时内通过平台托管释放",
          "如遇系统断开连接，需在5分钟内重连，否则视为违约"
        ],
        image: "https://images.unsplash.com/photo-1542751371-adc38448a05e?q=80&w=2070&auto=format&fit=crop"
      }
  };
  return allData[id || ""] || allData["MJK-001"];
};

export default function ContractDetail() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const isMine = searchParams.get("view") === "mine";
  const [data, setData] = useState<any>(null);
  const [activeTab, setActiveTab] = useState("terms");

  useEffect(() => {
    setData(getContractData(id));
    window.scrollTo(0, 0);
  }, [id]);

  const [showTipModal, setShowTipModal] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [tipAmount, setTipAmount] = useState(100);

  const handleTip = () => {
    if (tipAmount > 10000) {
      toast.error("单次打赏最高不能超过 10,000 摸金值");
      return;
    }
    const fee = Math.floor(tipAmount * 0.05);
    toast.success(`打赏成功！扣除 ${tipAmount + fee} 摸金值 (含5%手续费)`);
    if (tipAmount >= 1000) {
      toast("🎉 豪气！您的打赏已触发全服公告", {
        description: `老板 对打手 的服务非常满意，豪气打赏了 ${tipAmount} 摸金值！`
      });
    }
    setShowTipModal(false);
  };

  if (!data) return null;

  const handleSign = () => {
    setShowConfirmModal(true);
  };

  const confirmSign = () => {
    toast.success("契约签署成功！请前往“我的契约”查看进度。");
    setShowConfirmModal(false);
    navigate("/");
  };

  return (
    <div className="pb-36 min-h-screen bg-background relative max-w-md mx-auto border-x border-black/5 shadow-2xl">
      {/* Header Overlay - Constrained to max-w-md */}
      <div className="fixed top-0 z-50 w-full max-w-md left-1/2 -translate-x-1/2 flex items-center justify-between px-6 pt-6 pb-4 bg-gradient-to-b from-black/40 to-transparent">
        <button 
          onClick={() => navigate(-1)}
          className="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center border border-white/20 text-white hover:bg-white/30 transition-all"
        >
          <ChevronLeft size={20} />
        </button>
        <div className="flex gap-2">
          <button className="w-10 h-10 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center border border-white/20 text-white hover:bg-white/30 transition-all">
            <Share2 size={18} />
          </button>
        </div>
      </div>

      {/* Banner */}
      <div className="h-56 relative overflow-hidden">
        <ImageWithFallback src={data.image} alt={data.title} className="w-full h-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent" />
        
        {/* Status Badge */}
        <div className="absolute bottom-16 left-6">
          <span className={`px-4 py-1.5 rounded-full text-[11px] font-black tracking-widest shadow-xl border border-white/20 ${
            data.status === '进行中' ? 'bg-[#22c55e] text-white' : 'bg-[#d9a42b] text-white'
          }`}>
            {data.status}
          </span>
        </div>
      </div>

      {/* Content Container */}
      <div className="px-4 -mt-12 relative z-10">
        <div className="bg-white rounded-[48px] p-8 shadow-[0_-20px_60px_rgba(0,0,0,0.08)] border border-black/5 overflow-hidden">
          <div className="mb-6">
             <div className="flex items-center gap-2 mb-2.5">
                <span className="text-[11px] font-black text-[#d9a42b] bg-[#fcf8e8] px-2.5 py-1 rounded-lg border border-[#d9a42b]/10">{data.game}</span>
                <span className="text-[11px] font-bold text-muted-foreground/40 tracking-wider font-mono">ID: {id || 'MJK-001'}</span>
             </div>
             <h1 className="text-[22px] font-black text-foreground leading-tight tracking-tight mb-4">{data.title}</h1>
             
             <div className="flex items-center gap-5 mb-6">
               <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-bold">
                 <Clock size={15} className="text-[#d9a42b]" />
                 <span>剩 {data.remaining}</span>
               </div>
               <div className="flex items-center gap-1.5 text-[12px] text-muted-foreground font-bold">
                 <ShieldCheck size={15} className="text-[#d9a42b]" />
                 <span>信誉分门槛 {data.credit}</span>
               </div>
             </div>

             <div className="mb-10 bg-[#fcf8e8]/40 p-6 rounded-[32px] border border-[#d9a42b]/10 flex items-center justify-between">
                <div className="flex flex-col">
                  <span className="text-[11px] text-[#d9a42b]/60 font-black uppercase tracking-[0.2em] mb-1.5">契约信用风险抵押</span>
                  <div className="flex items-center gap-2">
                    <ShieldCheck size={18} className="text-[#d9a42b]" />
                    <span className="text-[14px] font-bold text-muted-foreground/80">本契约受平台信誉分全额保障</span>
                  </div>
                </div>
                <div className="flex flex-col items-end">
                  <span className="text-[11px] text-[#d9a42b]/60 font-black uppercase tracking-widest mb-1.5">需押信誉分</span>
                  <span className="text-[26px] font-black text-[#d9a42b] tracking-tighter leading-none">{data.deposit} 分</span>
                </div>
             </div>
          </div>

          <div className="h-px w-full bg-black/[0.04] mb-6" />

          {/* Issuer Info */}
          <div className="flex items-center justify-between mb-8 bg-[#fcf8e8]/30 p-4 rounded-3xl border border-[#d9a42b]/5">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl overflow-hidden border-2 border-white shadow-sm ring-2 ring-[#fcf8e8]">
                <ImageWithFallback src={data.bossAvatar} alt={data.boss} className="w-full h-full object-cover" />
              </div>
              <div>
                <div className="text-[14px] font-black text-foreground">{data.boss}</div>
                <div className="text-[10px] text-[#d9a42b] font-bold flex items-center gap-1">
                  <CheckCircle2 size={10} strokeWidth={3} />
                  已实名认证 · 信用优秀
                </div>
              </div>
            </div>
            <button className="bg-white hover:bg-[#fcf8e8] px-3.5 py-2.5 rounded-xl text-[11px] font-black text-[#d9a42b] border border-[#d9a42b]/10 transition-all shadow-sm">
              查看空间
            </button>
          </div>

          {/* Tabs */}
          <div className="flex gap-2 mb-6 bg-[#f8f9fa] p-1.5 rounded-2xl">
            <button 
              onClick={() => setActiveTab("terms")}
              className={`flex-1 py-3 rounded-xl text-[13px] font-black transition-all ${activeTab === 'terms' ? 'bg-white text-[#d9a42b] shadow-sm' : 'text-muted-foreground/60'}`}
            >
              契约条款
            </button>
            <button 
              onClick={() => setActiveTab("progress")}
              className={`flex-1 py-3 rounded-xl text-[13px] font-black transition-all ${activeTab === 'progress' ? 'bg-white text-[#d9a42b] shadow-sm' : 'text-muted-foreground/60'}`}
            >
              {isMine ? '履行记录' : '履约历史'}
            </button>
          </div>

          {/* Tab Content */}
          <div className="min-h-[200px]">
            {activeTab === 'terms' ? (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="space-y-6"
              >
                <div>
                  <h3 className="text-[13px] font-black text-foreground mb-3 flex items-center gap-2">
                    <FileText size={16} className="text-[#d9a42b]" />
                    详细说明
                  </h3>
                  <div className="text-[14px] text-muted-foreground leading-relaxed font-medium bg-[#f8f9fa] p-5 rounded-3xl border border-black/[0.02]">
                    {data.desc}
                  </div>
                </div>

                <div>
                  <h3 className="text-[13px] font-black text-foreground mb-3">契约约束条目</h3>
                  <div className="space-y-3.5">
                    {data.terms.map((term: string, idx: number) => (
                      <div key={idx} className="flex gap-3 items-start">
                        <div className="w-5 h-5 rounded-full bg-[#fcf8e8] flex items-center justify-center shrink-0 mt-0.5">
                          <span className="text-[10px] font-black text-[#d9a42b]">{idx + 1}</span>
                        </div>
                        <p className="text-[13px] text-muted-foreground/80 font-medium leading-relaxed">{term}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="p-5 bg-[#fff9db]/30 rounded-[32px] border border-[#d9a42b]/10 relative overflow-hidden">
                   <div className="absolute top-0 right-0 w-24 h-24 bg-[#d9a42b]/5 rounded-full -mr-12 -mt-12" />
                   <div className="flex items-center gap-2 mb-2.5 text-[#d9a42b] relative z-10">
                      <AlertCircle size={16} strokeWidth={3} />
                      <span className="text-[13px] font-black uppercase tracking-wider">违约仲裁规则</span>
                   </div>
                   <p className="text-[11px] text-[#d9a42b]/70 font-bold leading-relaxed relative z-10">
                     本契约已托管于平台信誉保护机制。任何一方违约，另一方可发起仲裁申请，由3名资深仲裁员根据证据判定契约金及信誉分归属。
                   </p>
                </div>
              </motion.div>
            ) : (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="space-y-6"
              >
                <div className="flex flex-col items-center justify-center py-12">
                   <div className="w-20 h-20 bg-[#f8f9fa] rounded-full flex items-center justify-center mb-5 text-muted-foreground/20 border border-black/[0.02]">
                      <History size={36} />
                   </div>
                   <p className="text-[15px] text-muted-foreground font-black">暂无履约变动记录</p>
                   <p className="text-[11px] text-muted-foreground/40 mt-1.5 font-bold uppercase tracking-widest">REAL-TIME SYNC ACTIVE</p>
                </div>
              </motion.div>
            )}
          </div>
        </div>
      </div>

      {/* Sticky Bottom Footer - Fixed to max-w-md Container Bottom */}
      <div className="fixed bottom-0 z-50 w-full max-w-md left-1/2 -translate-x-1/2 bg-white/90 backdrop-blur-2xl border-t border-black/5 px-6 pt-5 pb-9">
        {isMine ? (
          <div className="flex flex-col gap-4">
            {data.status === '已完成' && (
              <div className="bg-[#fcf8e8] p-3 rounded-2xl border border-[#d9a42b]/10 flex items-center justify-between">
                <div className="flex items-center gap-2">
                   <Gift size={16} className="text-[#d9a42b]" />
                   <span className="text-[11px] font-black text-[#d9a42b]">服务满意？给打手发个摸金红包吧</span>
                </div>
                <button 
                  onClick={() => setShowTipModal(true)}
                  className="bg-[#d9a42b] text-white px-4 py-1.5 rounded-xl text-[11px] font-black shadow-lg shadow-[#d9a42b]/20"
                >
                  发起打赏
                </button>
              </div>
            )}
            <div className="flex gap-3">
              <button 
                onClick={() => navigate('/arbitration')}
                className="flex-1 bg-[#fff5f5] text-[#fa5252] py-4.5 rounded-[24px] text-[14px] font-black flex items-center justify-center gap-2 border border-[#fa5252]/10 active:scale-95 transition-all"
              >
                <Gavel size={18} />
                申请仲裁
              </button>
              <button className="flex-[1.8] bg-[#22c55e] text-white py-4.5 rounded-[24px] text-[14px] font-black shadow-lg shadow-[#22c55e]/20 flex items-center justify-center gap-2 active:scale-95 transition-all">
                <CheckCircle2 size={18} />
                确认完成
              </button>
              <button className="w-14 h-14 bg-[#fcf8e8] rounded-[22px] flex items-center justify-center text-[#d9a42b] border border-[#d9a42b]/10 active:scale-95 transition-all">
                <MessageSquare size={22} strokeWidth={2.5} />
              </button>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between gap-4">
            <div className="flex flex-col">
              <span className="text-[10px] text-muted-foreground/50 font-black uppercase tracking-[0.15em] mb-0.5">风险抵押</span>
              <div className="flex items-baseline gap-1">
                <span className="text-[26px] font-black text-[#d9a42b] tracking-tighter">{data.deposit}</span>
                <span className="text-[12px] font-bold text-[#d9a42b]/60">信誉分</span>
              </div>
            </div>
            <div className="flex gap-2.5 shrink-0">
               <button className="w-14 h-14 bg-[#f8f9fa] rounded-[22px] flex items-center justify-center text-[#d9a42b] border border-black/5 active:scale-95 transition-all">
                  <MessageSquare size={22} strokeWidth={2.5} />
               </button>
               <button 
                onClick={handleSign}
                className="bg-[#d9a42b] hover:bg-[#c49221] active:scale-95 text-white px-9 h-14 rounded-[24px] text-[15px] font-black shadow-xl shadow-[#d9a42b]/20 flex items-center justify-center gap-2 group transition-all"
               >
                 立即签署
                 <ArrowRightLeft size={18} className="group-hover:rotate-180 transition-transform duration-500" />
               </button>
            </div>
          </div>
        )}
      </div>

      {/* Tip Modal */}
      <AnimatePresence>
        {showTipModal && (
          <div className="fixed inset-0 z-[100] flex items-end sm:items-center justify-center p-4 sm:p-6">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowTipModal(false)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ y: "100%", opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: "100%", opacity: 0 }}
              className="w-full max-w-md bg-white rounded-t-[40px] sm:rounded-[40px] p-8 relative z-10 shadow-2xl overflow-hidden"
            >
              <div className="absolute top-0 right-0 w-32 h-32 bg-[#fcf8e8] rounded-full -mr-16 -mt-16 -z-10" />
              
              <div className="flex justify-between items-center mb-8">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 bg-[#fff9db] rounded-2xl flex items-center justify-center text-[#d9a42b]">
                    <Gift size={24} strokeWidth={2.5} />
                  </div>
                  <div>
                    <h2 className="text-[18px] font-black text-foreground">发起打赏</h2>
                    <p className="text-[11px] text-muted-foreground font-bold">契约结束 24h 内限发一次</p>
                  </div>
                </div>
                <button 
                  onClick={() => setShowTipModal(false)}
                  className="w-10 h-10 bg-[#f8f9fa] rounded-full flex items-center justify-center text-muted-foreground hover:bg-[#f1f3f5] transition-all"
                >
                  <X size={20} />
                </button>
              </div>

              <div className="space-y-6">
                <div>
                  <label className="text-[12px] font-black text-muted-foreground uppercase tracking-widest block mb-4">选择打赏金额 (摸金值)</label>
                  <div className="grid grid-cols-3 gap-3">
                    {[100, 500, 1000, 2000, 5000, 10000].map((amt) => (
                      <button 
                        key={amt}
                        onClick={() => setTipAmount(amt)}
                        className={`py-4 rounded-2xl text-[14px] font-black transition-all border ${
                          tipAmount === amt 
                            ? 'bg-[#d9a42b] border-[#d9a42b] text-white shadow-lg shadow-[#d9a42b]/20 scale-[1.05]' 
                            : 'bg-[#f8f9fa] border-black/[0.03] text-muted-foreground hover:bg-white hover:border-[#d9a42b]/20'
                        }`}
                      >
                        {amt}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="bg-[#fcf8e8]/50 p-5 rounded-3xl border border-[#d9a42b]/10">
                   <div className="flex justify-between items-center mb-1">
                      <span className="text-[12px] font-bold text-muted-foreground">打赏金额</span>
                      <span className="text-[14px] font-black text-foreground">{tipAmount}</span>
                   </div>
                   <div className="flex justify-between items-center mb-1">
                      <span className="text-[12px] font-bold text-muted-foreground">平台服务费 (5%)</span>
                      <span className="text-[14px] font-black text-[#fa5252]">+{Math.floor(tipAmount * 0.05)}</span>
                   </div>
                   <div className="h-px bg-[#d9a42b]/10 my-3" />
                   <div className="flex justify-between items-center">
                      <span className="text-[13px] font-black text-foreground">总计扣除</span>
                      <span className="text-[20px] font-black text-[#d9a42b] tabular-nums">{tipAmount + Math.floor(tipAmount * 0.05)}</span>
                   </div>
                </div>

                <button 
                  onClick={handleTip}
                  className="w-full bg-[#d9a42b] text-white py-5 rounded-3xl text-[16px] font-black shadow-xl shadow-[#d9a42b]/20 flex items-center justify-center gap-3 hover:scale-[1.02] active:scale-95 transition-all"
                >
                  <Sparkles size={20} fill="currentColor" />
                  确认豪气打赏
                </button>
                
                <p className="text-[10px] text-center text-muted-foreground font-medium">
                  超过 1,000 摸金值将触发全服横幅公告
                </p>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
      {/* Confirmation Modal */}
      <AnimatePresence>
        {showConfirmModal && (
          <div className="fixed inset-0 z-[100] flex items-center justify-center p-6">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowConfirmModal(false)}
              className="absolute inset-0 bg-black/70 backdrop-blur-md"
            />
            <motion.div 
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              className="w-full max-w-[320px] bg-white rounded-[40px] p-8 relative z-10 shadow-2xl text-center"
            >
              <div className="w-20 h-20 bg-[#fcf8e8] rounded-full flex items-center justify-center mx-auto mb-6">
                 <ShieldCheck size={40} className="text-[#d9a42b]" strokeWidth={2.5} />
              </div>
              
              <h2 className="text-[20px] font-black text-foreground mb-3">确认签署契约？</h2>
              
              <div className="bg-[#f8f9fa] p-5 rounded-3xl border border-black/[0.03] mb-8">
                 <p className="text-[13px] text-muted-foreground font-bold leading-relaxed">
                   签署本契约将临时锁定您 <span className="text-[#d9a42b] font-black">{data.deposit}</span> 点信誉分，直至契约正常结案。
                 </p>
              </div>

              <div className="flex flex-col gap-3">
                <button 
                  onClick={confirmSign}
                  className="w-full bg-[#d9a42b] text-white py-4.5 rounded-2xl text-[15px] font-black shadow-xl shadow-[#d9a42b]/20 active:scale-95 transition-all"
                >
                  确认签署并锁定
                </button>
                <button 
                  onClick={() => setShowConfirmModal(false)}
                  className="w-full py-4 text-[14px] font-bold text-muted-foreground/60 hover:text-muted-foreground transition-all"
                >
                  我再想想
                </button>
              </div>

              <div className="mt-6 flex items-center justify-center gap-1.5 opacity-40">
                 <AlertCircle size={10} />
                 <span className="text-[10px] font-bold uppercase tracking-widest">Trust Protocol v2.0</span>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
