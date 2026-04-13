const { request } = require("../../utils/request");

Page({
  data: {
    id: "",
    detail: {},
    defaultBanner: "/images/contract.png",
    defaultAvatar: "/images/user_avatar.png",
    fromHall: false,
    stampVisible: false,
    confirming: false,
    countdownText: "",
    countdownTimer: null,
    activeTab: "terms"
  },
  onLoad(options) {
    if (options && options.id) {
      const fromHall = options.from === "hall";
      this.setData({ id: options.id, fromHall }, () => this.fetchDetail());
      return;
    }
    this.setData({ detail: {} });
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
          const canFinishBySigner = detail.canFinishBySigner
            ?? (detail.signerId ? detail.signerId !== String(detail.currentUserId || "") : false);
          this.setData({
            detail: this.normalizeDetail({
              ...detail,
              canSign,
              canFinishBySigner,
              createTime: this.formatDate(detail.createTime),
              startTime: this.formatDate(detail.startTime),
              endTime: this.formatDate(detail.endTime),
              completeTime: this.formatDate(detail.completeTime)
            })
          }, () => this.startCountdown(expireTime));
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ detail: {} });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ detail: {} });
      });
  },
  normalizeDetail(detail) {
    const statusMap = {
      PENDING: { label: "待接单", className: "info" },
      PAID: { label: "待开始", className: "info" },
      ACTIVE: { label: "进行中", className: "success" },
      IN_GAME: { label: "进行中", className: "success" },
      COMPLETED: { label: "已完成", className: "success" },
      CANCELLED: { label: "已取消", className: "warning" },
      DISPUTE: { label: "仲裁中", className: "danger" },
      VIOLATED: { label: "已违约", className: "danger" }
    };
    const mapped = statusMap[detail.status] || {
      label: detail.statusLabel || detail.status || "进行中",
      className: "warning"
    };
    return {
      ...detail,
      title: detail.title || "未命名契约",
      image: this.normalizeImageUrl(detail.coverUrl || detail.image || ""),
      bossAvatar: this.normalizeImageUrl(detail.bossAvatar || detail.initiator?.avatarUrl || detail.initiator?.avatar || ""),
      credit: detail.credit ?? detail.minCredit ?? 0,
      desc: detail.description || detail.requirements || detail.desc || detail.successCondition || "暂无详细说明",
      statusLabel: detail.statusLabel || mapped.label,
      statusClass: detail.statusClass || mapped.className,
      terms: this.buildTerms(detail)
    };
  },
  normalizeImageUrl(url) {
    const value = typeof url === "string" ? url.trim() : "";
    if (!value || value.includes("default-avatar.com")) {
      return "";
    }
    return value;
  },
  buildTerms(detail) {
    if (Array.isArray(detail.terms) && detail.terms.length) {
      return detail.terms;
    }

    const sections = [
      detail.requirements ? `契约要求：${detail.requirements}` : "",
      detail.successCondition ? `达成条件：${detail.successCondition}` : "",
      detail.failureCondition ? `违约说明：${detail.failureCondition}` : "",
      detail.guaranteeItem ? `保底条目：${detail.guaranteeItem}` : ""
    ].filter(Boolean);

    return sections;
  },
  onBannerError() {
    this.setData({ "detail.image": "" });
  },
  onAvatarError() {
    this.setData({ "detail.bossAvatar": this.data.defaultAvatar });
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
          setTimeout(() => this.fetchDetail(), 300);
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
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab || tab === this.data.activeTab) return;
    this.setData({ activeTab: tab });
  },
  goBack() {
    wx.navigateBack({ delta: 1 });
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
