const { request } = require("../../utils/request");

Page({
  data: {
    id: "",
    detail: {},
    fromHall: false,
    stampVisible: false,
    confirming: false
  },
  onLoad(options) {
    if (options && options.id) {
      const fromHall = options.from === "hall";
      this.setData({ id: options.id, fromHall }, () => this.fetchDetail());
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
  acceptContract() {
    if (this.data.confirming) return;
    const contractId = this.data.id;
    if (!contractId) return;

    this.setData({ confirming: true, stampVisible: false });
    request({
      url: "/api/contract/accept",
      method: "POST",
      data: {
        contractId
      }
    })
      .then((resp) => {
        if (resp && (resp.code === 0 || resp.code === 200)) {
          this.setData({ stampVisible: true });
          wx.showToast({ title: "接单成功", icon: "success" });
          this.fetchDetail();
          return;
        }
        wx.showToast({ title: resp.message || "接单失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        setTimeout(() => this.setData({ confirming: false }), 800);
      });
  },
  signContract() {
    if (this.data.confirming) return;
    const contractId = this.data.id;
    if (!contractId) return;

    this.setData({ confirming: true });
    request({
      url: "/api/contract/sign",
      method: "POST",
      data: {
        contractId
      }
    })
      .then((resp) => {
        if (resp && (resp.code === 0 || resp.code === 200)) {
          wx.showToast({ title: "签订成功", icon: "success" });
          this.fetchDetail();
          return;
        }
        wx.showToast({ title: resp.message || "签订失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        setTimeout(() => this.setData({ confirming: false }), 800);
      });
  },
  finishContract() {
    if (this.data.confirming) return;
    const contractId = this.data.id;
    if (!contractId) return;

    wx.showModal({
      title: "确认完成",
      content: "当契约完成时，如有契约金，则会原路退回",
      confirmText: "确认",
      cancelText: "取消",
      success: (res) => {
        if (!res.confirm) return;
        this.setData({ confirming: true });
        request({
          url: "/api/contract/finish",
          method: "POST",
          data: {
            contractId
          }
        })
          .then((resp) => {
            if (resp && (resp.code === 0 || resp.code === 200)) {
              wx.showToast({ title: "已提交完成", icon: "success" });
              this.fetchDetail();
              return;
            }
            wx.showToast({ title: resp.message || "完成失败", icon: "none" });
          })
          .catch(() => {
            wx.showToast({ title: "网络错误", icon: "none" });
          })
          .finally(() => {
            setTimeout(() => this.setData({ confirming: false }), 800);
          });
      }
    });
  },
  goDisputeApply() {
    if (!this.data.id) {
      wx.showToast({ title: "缺少契约ID", icon: "none" });
      return;
    }
    wx.navigateTo({ url: `/pages/dispute/apply?contractId=${this.data.id}` });
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
