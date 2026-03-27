import { Gavel, History, PlusCircle, ShieldAlert, Upload, ChevronRight, FileCheck2 } from "lucide-react";
import { motion } from "motion/react";

const arbitrationHistory = [
  {
    id: "882910",
    defendant: "张三",
    date: "2026-10-01 14:30",
    status: "已处理",
    result: "判定赔偿 (100.00 USDT)",
    statusColor: "bg-[#e7f9ee] text-[#22c55e]",
    dotColor: "bg-[#22c55e]"
  },
  {
    id: "771029",
    defendant: "李四",
    date: "2026-10-15 09:12",
    status: "进行中",
    result: "待专家核查中...",
    statusColor: "bg-[#fff9db] text-[#d9a42b]",
    dotColor: "bg-[#fcc419]"
  }
];

export default function Arbitration() {
  return (
    <div className="pb-32 pt-10 px-6 max-w-md mx-auto min-h-screen bg-background">
      <header className="flex justify-between items-center mb-10">
        <h1 className="text-2xl font-black text-[#212529] tracking-tight">仲裁中心</h1>
        <motion.button
          whileTap={{ scale: 0.95 }}
          className="bg-[#fa5252] text-white px-5 py-2.5 rounded-full text-sm font-black flex items-center gap-1.5 shadow-lg shadow-[#fa5252]/20"
        >
          申请仲裁
        </motion.button>
      </header>

      <section className="mb-10">
        <div className="flex items-center gap-2 mb-6">
          <History size={20} className="text-[#fcc419]" />
          <h2 className="text-lg font-black text-[#212529]">仲裁历史</h2>
        </div>

        <div className="space-y-6 relative ml-3 border-l-2 border-[#f1f3f5] pl-6 pb-2">
          {arbitrationHistory.map((item, idx) => (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: idx * 0.1 }}
              className="bg-white rounded-2xl p-5 shadow-[0_4px_20px_rgba(0,0,0,0.02)] border border-black/5 relative group"
            >
              <div className={`absolute -left-[31px] top-6 w-3 h-3 rounded-full ring-4 ring-white ${item.dotColor}`} />
              
              <div className="flex justify-between items-start mb-3">
                <span className="text-[12px] text-muted-foreground font-bold">契约ID: {item.id}</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-black ${item.statusColor}`}>
                  {item.status}
                </span>
              </div>

              <div className="space-y-1 mb-4">
                <div className="flex items-center gap-1.5">
                  <span className="text-[14px] font-bold text-[#212529]">违约方：{item.defendant}</span>
                </div>
                <div className="text-[11px] text-muted-foreground font-medium">
                  申请时间：{item.date}
                </div>
              </div>

              <div className="pt-3 border-t border-black/[0.03] text-[13px]">
                <span className="font-bold text-[#212529]">结果：</span>
                <span className={item.status === '已处理' ? 'text-[#fa5252] font-black' : 'text-muted-foreground italic'}>
                  {item.result}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="bg-white rounded-[32px] p-8 shadow-[0_10px_40px_rgba(0,0,0,0.03)] border border-black/5">
        <div className="flex items-center gap-2 mb-8">
           <PlusCircle size={22} className="text-[#fcc419]" strokeWidth={3} />
           <h2 className="text-lg font-black text-[#212529]">发起新仲裁</h2>
        </div>

        <div className="space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-black text-[#212529]">选择关联契约</label>
            <div className="relative">
              <select className="w-full bg-[#f8f9fa] border-none rounded-xl py-4 px-5 text-sm font-medium appearance-none focus:ring-2 focus:ring-[#fcc419]/30 text-muted-foreground">
                <option>请选择需要申诉的契约...</option>
                <option>8829341 - 与 张三 的契约</option>
              </select>
              <ChevronRight size={18} className="absolute right-4 top-1/2 -translate-y-1/2 rotate-90 text-muted-foreground/40 pointer-events-none" />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-black text-[#212529]">违约描述</label>
            <textarea 
              rows={4}
              placeholder="请详细描述违约情况，包括时间、地点、具体行为等..."
              className="w-full bg-[#f8f9fa] border-none rounded-xl py-4 px-5 text-sm font-medium focus:ring-2 focus:ring-[#fcc419]/30 resize-none placeholder:text-muted-foreground/40"
            />
          </div>

          <div className="space-y-2">
            <label className="text-sm font-black text-[#212529]">证据上传</label>
            <div className="grid grid-cols-2 gap-4">
               <button className="aspect-square bg-[#f8f9fa] border-2 border-dashed border-[#e9ecef] rounded-2xl flex flex-col items-center justify-center gap-2 group hover:border-[#fcc419] transition-all">
                  <ShieldAlert size={28} className="text-muted-foreground/40 group-hover:text-[#fcc419] transition-colors" />
                  <span className="text-[11px] font-bold text-muted-foreground/60 uppercase group-hover:text-[#fcc419]">截图/图片</span>
               </button>
               <button className="aspect-square bg-[#f8f9fa] border-2 border-dashed border-[#e9ecef] rounded-2xl flex flex-col items-center justify-center gap-2 group hover:border-[#fcc419] transition-all">
                  <Upload size={28} className="text-muted-foreground/40 group-hover:text-[#fcc419] transition-colors" />
                  <span className="text-[11px] font-bold text-muted-foreground/60 uppercase group-hover:text-[#fcc419]">录屏/视频</span>
               </button>
            </div>
            <input 
              type="text"
              placeholder="可粘贴外部证据链接 (如网盘/网页)"
              className="w-full bg-[#f8f9fa] border-none rounded-xl py-4 px-5 text-sm font-medium focus:ring-2 focus:ring-[#fcc419]/30 mt-3 placeholder:text-muted-foreground/40"
            />
          </div>

          <button className="w-full bg-[#22c55e] hover:bg-[#1a9d4b] text-white py-5 rounded-2xl text-[15px] font-black transition-all shadow-lg shadow-[#22c55e]/20 flex items-center justify-center gap-2 group">
            <FileCheck2 size={20} className="group-hover:scale-110 transition-transform" />
            提交仲裁申请
          </button>
          
          <p className="text-[10px] text-muted-foreground text-center font-medium opacity-60">
            提交后专家将在24小时内介入调查，请耐心等待
          </p>
        </div>
      </section>
    </div>
  );
}
