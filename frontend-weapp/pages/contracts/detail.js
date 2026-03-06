const { request } = require("../../utils/request");

Page({
  data: {
    id: "",
    detail: {},
    fromHall: false,
    stampVisible: false,
    confirming: false,
    countdownText: "",
    countdownTimer: null
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
          const canSign = detail.canSign ?? (detail.status === "PAID");
          const expireTime = detail.acceptExpireTime
            || (detail.status === "PAID" && detail.updateTime
            ? this.parseToTimestamp(detail.updateTime) + 30 * 60 * 1000
            : null);
          this.setData({
            detail: {
              ...detail,
              canSign,
              statusLabel: this.getStatusLabel(detail.status),
              createTime: this.formatDate(detail.createTime),
              startTime: this.formatDate(detail.startTime),
              endTime: this.formatDate(detail.endTime),
              completeTime: this.formatDate(detail.completeTime)
            }
          }, () => this.startCountdown(expireTime));
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
          wx.redirectTo({ url: `/pages/contracts/detail?id=${contractId}` });
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
  startCountdown(expireTime) {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
    if (!expireTime) {
      this.setData({ countdownText: "", countdownTimer: null });
      return;
    }
    const expireAt = this.parseToTimestamp(expireTime);
    if (!expireAt) {
      this.setData({ countdownText: "", countdownTimer: null });
      return;
    }
    const update = () => {
      const now = Date.now();
      const diff = expireAt - now;
      if (diff <= 0) {
        this.setData({ countdownText: "已超时", countdownTimer: null });
        clearInterval(this.data.countdownTimer);
        return;
      }
      const totalSeconds = Math.floor(diff / 1000);
      const minutes = Math.floor(totalSeconds / 60);
      const seconds = totalSeconds % 60;
      this.setData({
        countdownText: `接单后剩余 ${minutes}分${seconds < 10 ? "0" : ""}${seconds}秒`
      });
    };
    update();
    const timer = setInterval(update, 1000);
    this.setData({ countdownTimer: timer });
  },
  parseToTimestamp(value) {
    if (!value) return null;
    if (Array.isArray(value)) {
      const [y, m, d, hh = 0, mm = 0, ss = 0] = value;
      if ([y, m, d].every((v) => typeof v === "number")) {
        return new Date(y, m - 1, d, hh, mm, ss).getTime();
      }
    }
    if (typeof value === "string" && value.includes(",")) {
      const parts = value.split(",").map((v) => Number(v.trim()));
      if (parts.length >= 3 && parts.every((v) => !Number.isNaN(v))) {
        const [y, m, d, hh = 0, mm = 0, ss = 0] = parts;
        return new Date(y, m - 1, d, hh, mm, ss).getTime();
      }
    }
    const date = new Date(value);
    const ts = date.getTime();
    return Number.isNaN(ts) ? null : ts;
  },
  getStatusLabel(status) {
    const map = {
      PENDING: "待接单",
      PAID: "待开始",
      ACTIVE: "进行中",
      IN_GAME: "进行中",
      COMPLETED: "已完成",
      CANCELLED: "已取消",
      DISPUTE: "争议中",
      VIOLATED: "已违约"
    };
    return map[status] || status || "";
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
  },
  onUnload() {
    if (this.data.countdownTimer) {
      clearInterval(this.data.countdownTimer);
    }
  }
});
