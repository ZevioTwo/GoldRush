Page({
  data: {
    messages: [
      {
        id: 1,
        name: "仲裁助手",
        tag: "系统",
        time: "10:24",
        lastMessage: "您的仲裁申请已受理，请耐心等待。",
        avatar: "/images/avatar_1.png",
        unread: true,
        highlight: true
      },
      {
        id: 2,
        name: "契约小秘书",
        tag: "系统",
        time: "昨天",
        lastMessage: "您有新的契约待确认。",
        avatar: "/images/avatar_2.png",
        unread: false,
        highlight: false
      },
      {
        id: 3,
        name: "张三",
        tag: "对方",
        time: "周一",
        lastMessage: "我已签订契约，请查看。",
        avatar: "/images/avatar_3.png",
        unread: false,
        highlight: false
      }
    ]
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/message/index");
    }
  }
});
