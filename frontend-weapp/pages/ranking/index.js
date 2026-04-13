Page({
  data: {
    updateTime: "",
    redList: [],
    blackList: []
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/ranking/index");
    }
  }
});
