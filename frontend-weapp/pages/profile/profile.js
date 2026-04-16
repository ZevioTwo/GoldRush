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
    },
    checkinRewards: [],
    checkinDays: 0,
    checkinLoading: false,
    checkinSignedToday: false
  },
  onShow() {
    this.fetchProfile();
    this.fetchCredit();
    this.fetchStats();
    this.fetchCheckinStatus();
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
        this.setData({ profile: {} });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ profile: {} });
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
        this.setData({ creditScore: "-" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ creditScore: "-" });
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
        this.setData({ stats: { activeContracts: "-", completedContracts: "-", successRate: "-" } });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ stats: { activeContracts: "-", completedContracts: "-", successRate: "-" } });
      });
  },
  fetchCheckinStatus() {
    request({
      url: "/api/user/checkin/status",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const data = res.data || {};
          this.setData({
            checkinRewards: Array.isArray(data.rewards) ? data.rewards : [],
            checkinDays: data.totalDays ?? 0,
            checkinSignedToday: !!data.signedToday
          });
          return;
        }
        wx.showToast({ title: res.message || "获取签到失败", icon: "none" });
        this.setData({ checkinRewards: [], checkinDays: 0, checkinSignedToday: false });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ checkinRewards: [], checkinDays: 0, checkinSignedToday: false });
      });
  },
  claimCheckin() {
    if (this.data.checkinLoading || this.data.checkinSignedToday) return;
    this.setData({ checkinLoading: true });
    request({
      url: "/api/user/checkin/claim",
      method: "POST"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const data = res.data || {};
          this.setData({
            checkinRewards: Array.isArray(data.rewards) ? data.rewards : this.data.checkinRewards,
            checkinDays: data.totalDays ?? this.data.checkinDays,
            checkinSignedToday: !!data.signedToday
          });
          wx.showToast({ title: "签到成功", icon: "success" });
          return;
        }
        wx.showToast({ title: res.message || "签到失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ checkinLoading: false });
      });
  },
  goCredit() {
    wx.navigateTo({ url: "/pages/credit/credit" });
  },
  goArbitration() {
    wx.navigateTo({ url: "/pages/dispute/index" });
  },
  goRanking() {
    wx.navigateTo({ url: "/pages/ranking/index" });
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
    wx.navigateTo({ url: "/pages/credit/exchange" });
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
