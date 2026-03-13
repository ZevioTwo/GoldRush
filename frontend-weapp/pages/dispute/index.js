Page({
  data: {
    history: []
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/dispute/index");
    }
  },
  goApply() {
    wx.navigateTo({ url: "/pages/dispute/apply" });
  }
});
