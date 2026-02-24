const { request } = require("../../utils/request");
const { setToken } = require("../../utils/auth");

Page({
  data: {
    profile: {},
    creditScore: "-"
  },
  onShow() {
    this.fetchProfile();
    this.fetchCredit();
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
  goCredit() {
    wx.navigateTo({ url: "/pages/credit/credit" });
  },
  goContracts() {
    wx.navigateTo({ url: "/pages/contracts/list" });
  },
  goAccountManage() {
    wx.navigateTo({ url: "/pages/profile/accounts" });
  },
  logout() {
    setToken("");
    wx.reLaunch({ url: "/pages/login/login" });
  }
});
