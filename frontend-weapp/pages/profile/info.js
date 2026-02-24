const { request } = require("../../utils/request");

Page({
  data: {
    profile: {}
  },
  onShow() {
    this.fetchProfile();
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
  }
});
