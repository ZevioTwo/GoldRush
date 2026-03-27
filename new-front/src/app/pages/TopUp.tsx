import { useState } from "react";
import { useNavigate } from "react-router";
import { ChevronLeft, Coins, ShieldCheck, Check, Wallet, Info, AlertCircle } from "lucide-react";
import { motion } from "motion/react";

const PRESETS = [
  { points: 100, price: 100, label: "初级契约包", bonus: "送5分" },
  { points: 500, price: 488, label: "专业老板包", bonus: "送30分", popular: true },
  { points: 1000, price: 958, label: "顶奢打手包", bonus: "送80分" },
  { points: 2000, price: 1888, label: "荣耀金牌包", bonus: "送200分" },
];

export default function TopUp() {
  const navigate = useNavigate();
  const [selectedId, setSelectedId] = useState(1);
  const [paymentMethod, setPaymentMethod] = useState("wechat");

  return (
    <div className="pb-10 pt-6 px-6 max-w-md mx-auto min-h-screen bg-background relative overflow-x-hidden">
      {/* Header */}
      <header className="flex items-center justify-between mb-8">
        <button 
          onClick={() => navigate(-1)}
          className="w-10 h-10 bg-white rounded-full flex items-center justify-center shadow-sm border border-black/5"
        >
          <ChevronLeft size={20} className="text-foreground" />
        </button>
        <h1 className="text-lg font-black text-foreground tracking-tight">信誉分充值</h1>
        <div className="w-10 h-10 flex items-center justify-center text-[#d9a42b]">
          <Info size={20} />
        </div>
      </header>

      {/* User Info Card */}
      <section className="bg-white rounded-[32px] p-6 shadow-[0_10px_40px_rgba(0,0,0,0.03)] border border-black/5 mb-8 overflow-hidden relative">
        <div className="absolute top-0 right-0 w-32 h-32 bg-[#fff9db] rounded-full -mr-16 -mt-16 blur-3xl opacity-50" />
        <div className="relative flex items-center gap-4 mb-4">
          <div className="w-14 h-14 rounded-2xl bg-[#fcf8e8] flex items-center justify-center text-[#d9a42b] border border-[#d9a42b]/10">
            <ShieldCheck size={28} strokeWidth={2.5} />
          </div>
          <div>
            <div className="text-[12px] text-muted-foreground font-bold uppercase tracking-wider mb-0.5 opacity-60">当前信誉等级</div>
            <div className="flex items-center gap-2">
              <span className="text-2xl font-black text-foreground">985 分</span>
              <span className="bg-[#e7f9ee] text-[#22c55e] text-[10px] px-2 py-0.5 rounded-full font-black border border-[#22c55e]/10">极好</span>
            </div>
          </div>
        </div>
        <p className="text-[11px] text-muted-foreground/60 font-medium leading-relaxed">
          信誉分可作为契约签署时的诚信担保，1分等值于1元人民币。信誉分越高，在契约纠纷仲裁中拥有更高的信任权重。
        </p>
      </section>

      {/* Top-up Options */}
      <section className="mb-8">
        <div className="flex items-center gap-2 mb-4">
          <Coins size={18} className="text-[#d9a42b]" />
          <h2 className="text-sm font-black text-foreground">选择充值包</h2>
        </div>

        <div className="grid grid-cols-2 gap-4">
          {PRESETS.map((item, idx) => (
            <motion.div
              key={idx}
              whileTap={{ scale: 0.98 }}
              onClick={() => setSelectedId(idx)}
              className={`relative bg-white border-2 rounded-[28px] p-5 cursor-pointer transition-all ${
                selectedId === idx 
                  ? "border-[#d9a42b] bg-[#fcf8e8]/30 shadow-lg shadow-[#d9a42b]/5" 
                  : "border-black/[0.03] hover:border-black/10"
              }`}
            >
              {item.popular && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-[#d9a42b] text-white text-[9px] px-3 py-1 rounded-full font-black tracking-widest uppercase">
                  最受欢迎
                </div>
              )}
              <div className="flex flex-col items-center text-center">
                <span className="text-[11px] font-bold text-muted-foreground/60 mb-1">{item.label}</span>
                <div className="flex items-end gap-0.5 mb-1">
                  <span className="text-2xl font-black text-foreground">{item.points}</span>
                  <span className="text-[11px] font-black text-foreground/40 mb-1.5">分</span>
                </div>
                <div className="text-[#d9a42b] text-[13px] font-black">¥{item.price}</div>
                <div className="mt-2 text-[10px] bg-white px-2 py-0.5 rounded-full border border-[#d9a42b]/20 text-[#d9a42b] font-bold">
                  {item.bonus}
                </div>
              </div>
              {selectedId === idx && (
                <div className="absolute top-3 right-3 text-[#d9a42b]">
                  <Check size={16} strokeWidth={3} />
                </div>
              )}
            </motion.div>
          ))}
        </div>
      </section>

      {/* Payment Method */}
      <section className="mb-10">
        <div className="flex items-center gap-2 mb-4">
          <Wallet size={18} className="text-[#d9a42b]" />
          <h2 className="text-sm font-black text-foreground">支付方式</h2>
        </div>

        <div className="space-y-3">
          <button 
            onClick={() => setPaymentMethod("wechat")}
            className={`w-full flex items-center justify-between p-4 rounded-2xl border transition-all ${
              paymentMethod === 'wechat' ? 'bg-white border-[#d9a42b] shadow-sm' : 'bg-[#f8f9fa] border-transparent'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#07c160]/10 flex items-center justify-center text-[#07c160]">
                <svg viewBox="0 0 24 24" className="w-6 h-6 fill-current">
                  <path d="M8.35 11.2c.45 0 .82-.37.82-.82a.82.82 0 0 0-.82-.82c-.45 0-.82.37-.82.82 0 .45.37.82.82.82zm3.67 0c.45 0 .82-.37.82-.82a.82.82 0 0 0-.82-.82c-.45 0-.82.37-.82.82 0 .45.37.82.82.82zM12 4c-4.42 0-8 3.06-8 6.84 0 2.12 1.12 4.02 2.88 5.34l-.56 1.76 2-.96c.55.15 1.1.22 1.68.22 4.42 0 8-3.06 8-6.84S16.42 4 12 4zm5.04 12.16c1.16-.88 1.9-2.14 1.9-3.56 0-2.52-2.34-4.56-5.22-4.56-2.88 0-5.22 2.04-5.22 4.56 0 1.42.74 2.68 1.9 3.56l-.37 1.18 1.33-.64c.37.1.74.15 1.12.15 2.88 0 5.22-2.04 5.22-4.56l.04-.13c.23 0 .46.23.46.46 0 .23-.23.46-.46.46-.37 0-.74-.15-1.12-.15-2.88 0-5.22 2.04-5.22 4.56 0 1.42.74 2.68 1.9 3.56l-.37 1.18 1.33-.64c.37.1.74.15 1.12.15 2.88 0 5.22-2.04 5.22-4.56z"/>
                </svg>
              </div>
              <span className="text-[14px] font-bold text-[#212529]">微信支付</span>
            </div>
            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === 'wechat' ? 'border-[#d9a42b] bg-[#d9a42b]' : 'border-black/10'}`}>
              {paymentMethod === 'wechat' && <Check size={12} className="text-white" strokeWidth={4} />}
            </div>
          </button>

          <button 
            onClick={() => setPaymentMethod("alipay")}
            className={`w-full flex items-center justify-between p-4 rounded-2xl border transition-all ${
              paymentMethod === 'alipay' ? 'bg-white border-[#d9a42b] shadow-sm' : 'bg-[#f8f9fa] border-transparent'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#1677ff]/10 flex items-center justify-center text-[#1677ff]">
                <svg viewBox="0 0 24 24" className="w-6 h-6 fill-current">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1.68 13.98l-2.08 2.08-1.42-1.42 2.08-2.08 1.42 1.42zm.84-1.2l-2.08 2.08-1.42-1.42 2.08-2.08 1.42 1.42zm2.14-5.06c-.46-.72-1.12-1.3-1.92-1.68l.84-.84c.32-.32.32-.84 0-1.16-.32-.32-.84-.32-1.16 0l-.84.84c-.38-.8-.96-1.46-1.68-1.92l.84-.84c.32-.32.32-.84 0-1.16-.32-.32-.84-.32-1.16 0l-.84.84c-.8-.38-1.46-.96-1.92-1.68l-.84.84c-.32.32-.32.84 0 1.16.32.32.84.32 1.16 0l.84-.84c.38.8.96 1.46 1.68 1.92l-.84.84c-.32.32-.32.84 0 1.16.32.32.84.32 1.16 0l.84-.84c.72.46 1.3 1.12 1.68 1.92l-.84.84c-.32.32-.32.84 0 1.16.32.32.84.32 1.16 0l.84-.84c.46.72 1.12 1.3 1.92 1.68l-.84.84c-.32.32-.32.84 0 1.16.32.32.84.32 1.16 0l.84-.84z"/>
                </svg>
              </div>
              <span className="text-[14px] font-bold text-[#212529]">支付宝</span>
            </div>
            <div className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${paymentMethod === 'alipay' ? 'border-[#d9a42b] bg-[#d9a42b]' : 'border-black/10'}`}>
              {paymentMethod === 'alipay' && <Check size={12} className="text-white" strokeWidth={4} />}
            </div>
          </button>
        </div>
      </section>

      {/* Warning */}
      <div className="flex gap-3 p-4 bg-destructive/5 rounded-2xl border border-destructive/10 mb-8">
        <AlertCircle size={18} className="text-destructive shrink-0" />
        <p className="text-[11px] text-destructive font-medium leading-relaxed">
          温馨提示：信誉分充值成功后无法直接提现，仅可用于签署契约时的信誉抵扣或仲裁赔付。请根据实际需求充值。
        </p>
      </div>

      {/* Footer Button */}
      <div className="mt-auto">
        <button className="w-full bg-[#d9a42b] hover:bg-[#c49221] text-white py-5 rounded-[24px] text-[16px] font-black transition-all shadow-xl shadow-[#d9a42b]/20 flex items-center justify-center gap-2 group">
          立即支付 ¥{PRESETS[selectedId].price}
          <ChevronLeft size={20} className="rotate-180 group-hover:translate-x-1 transition-transform" />
        </button>
      </div>
    </div>
  );
}
