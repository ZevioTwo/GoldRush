const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    form: {
      wechatId: "",
      phone: ""
    }
  },
  onLoad() {
    this.fetchProfile();
  },
  fetchProfile() {
    request({
      url: "/api/user/profile",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const data = res.data || {};
          this.setData({
            form: {
              wechatId: data.wechatId || "",
              phone: data.phone || ""
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
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  validate(form) {
    if (form.wechatId && form.wechatId.length > 50) return "微信号不能超过50字";
    if (form.phone && form.phone.length > 20) return "手机号不能超过20字";
    return "";
  },
  submit() {
    if (this.data.loading) return;
    const form = this.data.form;
    const error = this.validate(form);
    if (error) {
      wx.showToast({ title: error, icon: "none" });
      return;
    }

    this.setData({ loading: true });
    request({
      url: "/api/user/profile/update",
      method: "POST",
      data: {
        wechatId: form.wechatId || undefined,
        phone: form.phone || undefined
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "保存成功", icon: "success" });
          wx.navigateBack();
          return;
        }
        wx.showToast({ title: res.message || "保存失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
