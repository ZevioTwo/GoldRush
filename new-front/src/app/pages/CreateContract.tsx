import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import { ArrowLeft, Lock, Shield, Info, ChevronDown, Check, Search } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";

interface ContractFormData {
  title: string;
  game: string;
  needsBetCredit: boolean;
  betAmount: number;
  terms: string;
  conditions: string;
  needsMinCredit: boolean;
  minCredit: number;
}

const GAMES = [
  "三角洲行动", "DNF", "王者荣耀", "和平精英", "绝地求生", "永劫无间", 
  "英雄联盟", "金铲铲之战", "原神", "无畏契约", "永劫无间手游", "魔兽世界",
  "CS2", "逃离塔科夫", "暗区突围"
];

export default function CreateContract() {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGameDropdownOpen, setIsGameDropdownOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);
  
  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<ContractFormData>({
    defaultValues: {
      game: "三角洲行动",
      needsBetCredit: true,
      needsMinCredit: true,
      betAmount: 100,
      minCredit: 650
    }
  });

  const selectedGame = watch("game");
  const needsBetCredit = watch("needsBetCredit");
  const needsMinCredit = watch("needsMinCredit");

  const filteredGames = GAMES.filter(game => 
    game.toLowerCase().includes(searchTerm.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsGameDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      setSearchTerm(""); // Reset search on close
    };
  }, [isGameDropdownOpen]);

  const onSubmit = (data: ContractFormData) => {
    setIsSubmitting(true);
    setTimeout(() => {
      toast.success("契约发起成功！");
      navigate("/");
      setIsSubmitting(false);
    }, 1500);
  };

  return (
    <div className="pb-32 pt-6 px-5 max-w-md mx-auto min-h-screen bg-white">
      {/* Header */}
      <header className="flex items-center justify-between mb-8">
        <button onClick={() => navigate(-1)} className="p-2 -ml-2">
          <ArrowLeft size={24} className="text-[#212529]" />
        </button>
        <h1 className="text-[17px] font-black text-[#212529]">发起契约</h1>
        <div className="w-10" /> {/* Spacer */}
      </header>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Title */}
        <section className="space-y-2.5">
          <label className="text-[14px] font-black text-[#212529]">契约标题</label>
          <input
            {...register("title", { required: "请输入标题" })}
            placeholder="请输入契约标题，如：诚信借贷协议"
            className="w-full bg-[#f8f9fa] border border-[#e9ecef] rounded-xl py-4 px-5 text-[14px] focus:ring-1 focus:ring-[#d9a42b] outline-none transition-all placeholder:text-[#adb5bd]"
          />
        </section>

        {/* Game Selection Dropdown with Search */}
        <section className="space-y-2.5" ref={dropdownRef}>
          <label className="text-[14px] font-black text-[#212529]">契约类型</label>
          <div className="relative">
            <div 
              onClick={() => setIsGameDropdownOpen(!isGameDropdownOpen)}
              className={`w-full bg-[#f8f9fa] border rounded-xl py-4 px-5 flex items-center justify-between cursor-pointer transition-all ${isGameDropdownOpen ? 'border-[#d9a42b] bg-white ring-1 ring-[#d9a42b]/10' : 'border-[#e9ecef]'}`}
            >
              <span className="text-[14px] font-bold text-[#212529]">{selectedGame}</span>
              <ChevronDown 
                size={18} 
                className={`text-[#adb5bd] transition-transform duration-300 ${isGameDropdownOpen ? 'rotate-180 text-[#d9a42b]' : ''}`} 
              />
            </div>

            <AnimatePresence>
              {isGameDropdownOpen && (
                <motion.div
                  initial={{ opacity: 0, y: 5, scale: 0.98 }}
                  animate={{ opacity: 1, y: 5, scale: 1 }}
                  exit={{ opacity: 0, y: 5, scale: 0.98 }}
                  className="absolute z-50 w-full bg-white border border-[#e9ecef] rounded-xl shadow-2xl overflow-hidden mt-1 shadow-black/5"
                >
                  {/* Search Input */}
                  <div className="p-2 border-b border-[#f1f3f5] sticky top-0 bg-white z-10">
                    <div className="relative">
                      <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#adb5bd]" />
                      <input
                        autoFocus
                        type="text"
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        placeholder="搜索游戏..."
                        className="w-full bg-[#f8f9fa] border-none rounded-lg py-2.5 pl-9 pr-4 text-[13px] font-medium outline-none placeholder:text-[#adb5bd]"
                      />
                    </div>
                  </div>

                  {/* List */}
                  <div className="p-1.5 max-h-60 overflow-y-auto">
                    {filteredGames.length > 0 ? (
                      filteredGames.map((game) => (
                        <div
                          key={game}
                          onClick={() => {
                            setValue("game", game);
                            setIsGameDropdownOpen(false);
                            setSearchTerm("");
                          }}
                          className={`flex items-center justify-between px-4 py-3 rounded-lg cursor-pointer transition-colors ${
                            selectedGame === game 
                              ? 'bg-[#fff9db] text-[#d9a42b]' 
                              : 'hover:bg-[#f8f9fa] text-[#495057]'
                          }`}
                        >
                          <span className="text-[14px] font-bold">{game}</span>
                          {selectedGame === game && (
                            <Check size={16} strokeWidth={3} />
                          )}
                        </div>
                      ))
                    ) : (
                      <div className="py-8 text-center text-[#adb5bd] text-[13px] font-medium">
                        未搜索到相关游戏
                      </div>
                    )}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </section>

        {/* Bet Credit Score Toggle */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-[#fff9db] rounded-xl flex items-center justify-center">
                <Lock size={20} className="text-[#d9a42b]" />
              </div>
              <div>
                <p className="text-[14px] font-black text-[#212529]">是否需要押信誉分</p>
                <p className="text-[11px] text-[#868e96]">双方达成契约后需冻结相应分值</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setValue("needsBetCredit", !needsBetCredit)}
              className={`w-12 h-6 rounded-full transition-colors relative ${needsBetCredit ? 'bg-[#d9a42b]' : 'bg-[#dee2e6]'}`}
            >
              <motion.div 
                animate={{ x: needsBetCredit ? 26 : 2 }}
                className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-sm"
              />
            </button>
          </div>

          {needsBetCredit && (
            <div className="space-y-2.5">
              <label className="text-[14px] font-black text-[#212529]">押信誉分要求</label>
              <div className="relative">
                <input
                  {...register("betAmount")}
                  type="number"
                  className="w-full bg-[#f8f9fa] border border-[#e9ecef] rounded-xl py-4 px-5 pr-12 text-[14px] outline-none"
                />
                <span className="absolute right-5 top-1/2 -translate-y-1/2 text-[13px] text-[#adb5bd] font-bold">分</span>
              </div>
            </div>
          )}
        </section>

        {/* Terms */}
        <section className="space-y-2.5">
          <label className="text-[14px] font-black text-[#212529]">契约详情/条款</label>
          <textarea
            {...register("terms")}
            rows={4}
            placeholder="请详细描述契约内容及双方约定的具体条款内容..."
            className="w-full bg-[#f8f9fa] border border-[#e9ecef] rounded-xl py-4 px-5 text-[14px] outline-none resize-none"
          />
        </section>

        {/* Conditions */}
        <section className="space-y-2.5">
          <label className="text-[14px] font-black text-[#212529]">契约达成条件</label>
          <textarea
            {...register("conditions")}
            rows={3}
            placeholder="请说明在何种情况下此契约视为正式达成生效"
            className="w-full bg-[#f8f9fa] border border-[#e9ecef] rounded-xl py-4 px-5 text-[14px] outline-none resize-none"
          />
        </section>

        {/* Min Credit Toggle */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-[#fff9db] rounded-xl flex items-center justify-center">
                <Shield size={20} className="text-[#d9a42b]" />
              </div>
              <div>
                <p className="text-[14px] font-black text-[#212529]">是否需要信誉分</p>
                <p className="text-[11px] text-[#868e96]">仅允许达到信用门槛的用户参与</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setValue("needsMinCredit", !needsMinCredit)}
              className={`w-12 h-6 rounded-full transition-colors relative ${needsMinCredit ? 'bg-[#d9a42b]' : 'bg-[#dee2e6]'}`}
            >
              <motion.div 
                animate={{ x: needsMinCredit ? 26 : 2 }}
                className="absolute top-1 w-4 h-4 bg-white rounded-full shadow-sm"
              />
            </button>
          </div>

          {needsMinCredit && (
            <div className="space-y-2.5">
              <label className="text-[14px] font-black text-[#212529]">最低信誉分要求</label>
              <div className="relative">
                <input
                  {...register("minCredit")}
                  type="number"
                  className="w-full bg-[#f8f9fa] border border-[#e9ecef] rounded-xl py-4 px-5 pr-12 text-[14px] outline-none"
                />
                <span className="absolute right-5 top-1/2 -translate-y-1/2 text-[13px] text-[#adb5bd] font-bold">分</span>
              </div>
            </div>
          )}
        </section>

        {/* Legal Disclaimer */}
        <div className="bg-[#fff9db]/50 border border-[#fff3bf] p-4 rounded-xl flex gap-3">
          <Info size={16} className="text-[#d9a42b] shrink-0 mt-0.5" />
          <p className="text-[11px] text-[#8b6e27] leading-relaxed">
            温馨提示：契约一经发起并达成，将具有法律效力并计入摸金信誉档案，请谨慎填写条款内容。
          </p>
        </div>

        {/* Submit Button */}
        <button
          disabled={isSubmitting}
          type="submit"
          className="w-full bg-[#d9a42b] hover:bg-[#c19225] text-white py-4.5 rounded-xl text-[15px] font-black shadow-lg shadow-[#d9a42b]/20 flex items-center justify-center gap-2 transition-all active:scale-[0.98]"
        >
          {isSubmitting ? (
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              <Shield size={18} fill="currentColor" className="opacity-80" />
              <span>确认发起契约</span>
            </>
          )}
        </button>
      </form>
    </div>
  );
}
