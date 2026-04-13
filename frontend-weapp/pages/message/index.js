Page({
  data: {
    messages: []
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/message/index");
    }
  }
});
