const { request } = require("../../utils/request");

Page({
  data: {
    id: "",
    detail: null,
    loading: true,
    claiming: false
  },
  onLoad(options) {
    const id = options && options.id ? String(options.id) : "";
    if (!id) {
      this.setData({ loading: false, detail: null });
      wx.showToast({ title: "缺少悬赏ID", icon: "none" });
      return;
    }
    this.setData({ id }, () => this.fetchDetail());
  },
  fetchDetail() {
    if (!this.data.id) return;
    this.setData({ loading: true });
    request({
      url: `/api/bounty/${this.data.id}`,
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ detail: this.normalizeDetail(res.data || {}) });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ detail: null });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ detail: null });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  normalizeDetail(detail) {
    const recruitTargetCount = Number(detail.recruitTargetCount) || 0;
    const recruitCurrentCount = Number(detail.recruitCurrentCount) || 0;
    const claimed = !!detail.claimed;
    const canClaim = !!detail.canClaim;
    const status = detail.status || (recruitCurrentCount >= recruitTargetCount ? "FULL" : "OPEN");
    const progressPercent = recruitTargetCount > 0
      ? Math.min(100, Math.max(0, (recruitCurrentCount / recruitTargetCount) * 100))
      : 0;

    let actionText = "接取悬赏";
    if (claimed) {
      actionText = "已接取";
    } else if (!canClaim && status === "FULL") {
      actionText = "已招满";
    } else if (!canClaim) {
      actionText = "暂不可接";
    }

    return {
      ...detail,
      recruitTargetCount,
      recruitCurrentCount,
      claimed,
      canClaim,
      status,
      statusLabel: status === "FULL" ? "已招满" : "招募中",
      statusClass: status === "FULL" ? "full" : "open",
      rewardDisplay: this.formatAmount(detail.rewardMojin),
      totalRewardDisplay: this.formatAmount(detail.totalRewardMojin),
      createTimeDisplay: this.formatDate(detail.createTime),
      actionText,
      actionDisabled: !canClaim,
      progressText: recruitTargetCount > 0 ? `${recruitCurrentCount}/${recruitTargetCount}` : "0/0",
      progressPercentText: `${progressPercent}%`,
      descriptionDisplay: detail.description || "暂无悬赏描述"
    };
  },
  formatAmount(value) {
    const amount = Number(value);
    if (!Number.isFinite(amount)) return "0.00";
    return amount.toFixed(2);
  },
  formatDate(value) {
    if (!value) return "";
    const pad = (n) => (n < 10 ? `0${n}` : `${n}`);
    if (Array.isArray(value)) {
      const [y, m, d, hh = 0, mm = 0] = value;
      return `${y}-${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}`;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  },
  claimBounty() {
    const detail = this.data.detail;
    if (!detail || detail.actionDisabled || this.data.claiming) return;

    this.setData({ claiming: true });
    request({
      url: `/api/bounty/${this.data.id}/claim`,
      method: "POST"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "接取成功", icon: "success" });
          this.fetchDetail();
          return;
        }
        wx.showToast({ title: res.message || "接取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ claiming: false });
      });
  }
});
