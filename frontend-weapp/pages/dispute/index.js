Page({
  data: {
    history: [],
    contractOptions: ["请选择需要申诉的契约..."],
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
