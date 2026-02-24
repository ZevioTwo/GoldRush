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
        if (res && (res.code === 0 || res.code === 200)) {
          const detail = res.data || {};
          this.setData({
            detail: {
              ...detail,
              createTime: this.formatDate(detail.createTime),
              startTime: this.formatDate(detail.startTime),
              endTime: this.formatDate(detail.endTime),
              completeTime: this.formatDate(detail.completeTime)
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
  formatDate(value) {
    if (!value) return "";
    const pad = (n) => (n < 10 ? `0${n}` : `${n}`);

    if (Array.isArray(value)) {
      const [y, m, d, hh = 0, mm = 0] = value;
      return `${y}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}`;
    }

    if (typeof value === "string" && value.includes(",")) {
      const parts = value.split(",").map((v) => Number(v.trim()));
      if (parts.length >= 3 && parts.every((v) => !Number.isNaN(v))) {
        const [y, m, d, hh = 0, mm = 0] = parts;
        return `${y}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}`;
      }
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
});
