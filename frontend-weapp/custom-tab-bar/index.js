Component({
  data: {
    selected: "",
    list: [
      {
        pagePath: "/pages/contracts/list",
        text: "我的契约",
        icon: "/images/contract.png",
        activeIcon: "/images/contract_active.png"
      },
      {
        pagePath: "/pages/hall/list",
        text: "契约大厅",
        icon: "/images/hall.png",
        activeIcon: "/images/hall_active.png"
      },
      {
        pagePath: "/pages/bounty/index",
        text: "悬赏榜单",
        icon: "/images/ranking.png",
        activeIcon: "/images/ranking_active.png"
      },
      {
        pagePath: "/pages/profile/profile",
        text: "个人中心",
        icon: "/images/me.png",
        activeIcon: "/images/me_active.png"
      }
    ]
  },
  methods: {
    switchTab(e) {
      const path = e.currentTarget.dataset.path;
      if (!path || path === this.data.selected) return;
      wx.switchTab({ url: path });
    },
    setSelected(path) {
      if (!path) return;
      this.setData({ selected: path });
    }
  }
});
