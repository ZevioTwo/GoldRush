const { request } = require("../../utils/request");
const { setToken } = require("../../utils/auth");

Page({
  data: {
    loading: false
  },
  handleLogin() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    wx.login({
      success: (loginRes) => {
        if (!loginRes.code) {
          this.setData({ loading: false });
          wx.showToast({ title: "登录失败", icon: "none" });
          return;
        }

        request({
          url: "/api/user/login",
          method: "POST",
          data: {
            code: loginRes.code
          }
        })
          .then((res) => {
            if (res && (res.code === 0 || res.code === 200) && res.data && res.data.token) {
              setToken(res.data.token);
              wx.reLaunch({ url: "/pages/profile/profile" });
              return;
            }
            wx.showToast({ title: res.message || "登录失败", icon: "none" });
          })
          .catch(() => {
            wx.showToast({ title: "网络错误", icon: "none" });
          })
          .finally(() => {
            this.setData({ loading: false });
          });
      },
      fail: () => {
        this.setData({ loading: false });
        wx.showToast({ title: "微信登录失败", icon: "none" });
      }
    });
  }
});
