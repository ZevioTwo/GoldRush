Page({
  data: {
    updateTime: "2026-03-13 10:00",
    activeTab: "red",
    redList: [
      { rank: 1, name: "张三", score: 980 },
      { rank: 2, name: "李四", score: 950 },
      { rank: 3, name: "王五", score: 930 }
    ],
    blackList: [
      { rank: 1, name: "赵六", score: 120 },
      { rank: 2, name: "孙七", score: 160 },
      { rank: 3, name: "周八", score: 200 }
    ]
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/ranking/index");
    }
  },
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab || tab === this.data.activeTab) return;
    this.setData({ activeTab: tab });
  }
});
