Component({
  data: {
    selected: "",
    list: [
      { pagePath: "/pages/contracts/list", text: "我的契约", icon: "契" },
      { pagePath: "/pages/hall/list", text: "契约大厅", icon: "厅" },
      { pagePath: "/pages/dispute/index", text: "契约仲裁", icon: "裁" },
      { pagePath: "/pages/profile/profile", text: "个人中心", icon: "我" }
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
