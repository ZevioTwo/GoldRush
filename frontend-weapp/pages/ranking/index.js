const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    updateTime: "",
    redList: [],
    blackList: [],
    defaultAvatar: "/images/user_avatar.png"
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/ranking/index");
    }
    this.fetchRanking();
  },
  fetchRanking() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    request({
      url: "/api/user/ranking",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const data = res.data || {};
          this.setData({
            updateTime: data.updateTime || "",
            redList: this.normalizeRankingList(data.redList),
            blackList: this.normalizeRankingList(data.blackList)
          });
          return;
        }

        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ updateTime: "", redList: [], blackList: [] });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ updateTime: "", redList: [], blackList: [] });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  normalizeRankingList(list) {
    return (Array.isArray(list) ? list : []).map((item, index) => ({
      rank: Number(item.rank) || index + 1,
      name: item.name || `用户${item.userId || index + 1}`,
      avatarUrl: item.avatarUrl || "",
      score: typeof item.score === "number" ? item.score : Number(item.score) || 0
    }));
  }
});
