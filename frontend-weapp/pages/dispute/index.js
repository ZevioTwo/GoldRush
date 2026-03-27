Page({
  data: {
    history: [
      {
        id: "882910",
        contractId: "882910",
        party: "张三",
        applyTime: "2026-10-01 14:30",
        status: "已处理",
        result: "判定赔偿 (100.00 USDT)",
        statusType: "resolved"
      },
      {
        id: "771029",
        contractId: "771029",
        party: "李四",
        applyTime: "2026-10-15 09:12",
        status: "进行中",
        result: "待专家核查中...",
        statusType: "inProgress"
      }
    ],
    contractOptions: ["请选择需要申诉的契约...", "8829341 - 与 张三 的契约"],
    contractIndex: 0
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/dispute/index");
    }
  },
  onContractPick(e) {
    const index = Number(e.detail.value) || 0;
    this.setData({ contractIndex: index });
  },
  goApply() {
    wx.navigateTo({ url: "/pages/dispute/apply" });
  }
});
