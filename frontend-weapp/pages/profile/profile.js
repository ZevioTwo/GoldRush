const { request } = require("../../utils/request");
const { setToken } = require("../../utils/auth");

Page({
  data: {
    profile: {},
    creditScore: "985",
    stats: {
      activeContracts: "12",
      completedContracts: "156",
      successRate: "100%"
    },
    checkinRewards: [
      { label: "2", active: true },
      { label: "4", active: true },
      { label: "6", active: true },
      { label: "8", active: false },
      { label: "10", active: false },
      { label: "12", active: false },
      { label: "?", active: false }
    ]
  },
  onShow() {
    this.fetchProfile();
    this.fetchCredit();
    this.fetchStats();
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/profile/profile");
    }
  },
  fetchProfile() {
    request({
      url: "/api/user/profile",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ profile: res.data || {} });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ profile: { nickname: "张晓明", userId: "8888 6666", userLevel: "高保户" } });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ profile: { nickname: "张晓明", userId: "8888 6666", userLevel: "高保户" } });
      });
  },
  fetchCredit() {
    request({
      url: "/api/user/credit",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ creditScore: res.data?.currentScore ?? "-" });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ creditScore: "985" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ creditScore: "985" });
      });
  },
  fetchStats() {
    request({
      url: "/api/user/stats",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const data = res.data || {};
          const successRate = typeof data.successRate === "number"
            ? `${data.successRate}%`
            : data.successRate ?? "-";
          this.setData({
            stats: {
              activeContracts: data.activeContracts ?? "-",
              completedContracts: data.completedContracts ?? "-",
              successRate
            }
          });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ stats: { activeContracts: "12", completedContracts: "156", successRate: "100%" } });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ stats: { activeContracts: "12", completedContracts: "156", successRate: "100%" } });
      });
  },
  goCredit() {
    wx.navigateTo({ url: "/pages/credit/credit" });
  },
  goArbitration() {
    wx.navigateTo({ url: "/pages/dispute/index" });
  },
  goUserInfo() {
    wx.navigateTo({ url: "/pages/profile/info" });
  },
  goVipCenter() {
    wx.navigateTo({ url: "/pages/profile/vip" });
  },
  goAccounts() {
    wx.navigateTo({ url: "/pages/profile/accounts" });
  },
  goContracts() {
    wx.navigateTo({ url: "/pages/contracts/list" });
  },
  goExchange() {
    wx.showToast({ title: "功能开发中", icon: "none" });
  },
  goCreditHistory() {
    wx.showToast({ title: "功能开发中", icon: "none" });
  },
  goContractReport() {
    wx.showToast({ title: "功能开发中", icon: "none" });
  },
  goNotifySettings() {
    wx.showToast({ title: "功能开发中", icon: "none" });
  },
  goAbout() {
    wx.showToast({ title: "功能开发中", icon: "none" });
  },
  logout() {
    setToken("");
    wx.reLaunch({ url: "/pages/login/login" });
  }
});
