Page({
  data: {
    updateTime: "2026年10月24日 14:30",
    redList: [
      { rank: 1, name: "财富猎人·王", score: 998 },
      { rank: 2, name: "守信商会A", score: 985 },
      { rank: 3, name: "金诚契约", score: 980 },
      { rank: 4, name: "林某某", score: 975 },
      { rank: 5, name: "极客摸金", score: 970 },
      { rank: 6, name: "信义天成", score: 965 },
      { rank: 7, name: "陈大发", score: 960 },
      { rank: 8, name: "北极星契约", score: 955 },
      { rank: 9, name: "诚信工作室", score: 950 },
      { rank: 10, name: "徐先生", score: 945 }
    ],
    blackList: [
      { rank: 1, name: "失信者·无名", score: 210 },
      { rank: 2, name: "影子猎手", score: 245 },
      { rank: 3, name: "违约黑洞", score: 280 },
      { rank: 4, name: "逃债专家", score: 315 },
      { rank: 5, name: "破产边缘", score: 350 },
      { rank: 6, name: "李某发", score: 390 },
      { rank: 7, name: "暗夜摸金", score: 420 },
      { rank: 8, name: "虚假声明", score: 450 },
      { rank: 9, name: "契约漏洞", score: 480 },
      { rank: 10, name: "赵六子", score: 510 }
    ]
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/ranking/index");
    }
  }
});
