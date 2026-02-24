const { request } = require("../../utils/request");

Page({
  data: {
    id: "",
    detail: {}
  },
  onLoad(options) {
    if (options && options.id) {
      this.setData({ id: options.id }, () => this.fetchDetail());
    }
  },
  fetchDetail() {
    request({
      url: `/api/contract/${this.data.id}`,
      method: "GET"
    })
      .then((res) => {
        if (res && res.code === 0) {
          this.setData({ detail: res.data || {} });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  }
});
