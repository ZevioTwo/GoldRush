const { request } = require("../../utils/request");
const { setToken } = require("../../utils/auth");

Page({
  data: {
    profile: {},
    creditScore: "-",
    stats: {
      activeContracts: "-",
      completedContracts: "-",
      successRate: "-"
    }
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
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
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
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
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
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  goCredit() {
    wx.navigateTo({ url: "/pages/credit/credit" });
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
