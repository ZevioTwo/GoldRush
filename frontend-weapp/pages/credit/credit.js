const { request } = require("../../utils/request");

Page({
  data: {
    creditScore: "-",
    profile: {}
  },
  onShow() {
    this.fetchCredit();
    this.fetchProfile();
  },
  fetchCredit() {
    request({
      url: "/api/user/credit",
      method: "GET"
    })
      .then((res) => {
        if (res && res.code === 0) {
          this.setData({ creditScore: res.data?.currentScore ?? "-" });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  fetchProfile() {
    request({
      url: "/api/user/profile",
      method: "GET"
    })
      .then((res) => {
        if (res && res.code === 0) {
          this.setData({ profile: res.data || {} });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  }
});
