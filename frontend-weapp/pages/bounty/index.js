const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    loading: false,
    hasMore: true,
    claimingId: null
  },
  onShow() {
    this.resetAndFetch();
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/bounty/index");
    }
  },
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.fetchList(true));
  },
  resetAndFetch() {
    this.setData({ page: 1, list: [], hasMore: true }, () => this.fetchList());
  },
  fetchList(append = false) {
    if (this.data.loading) return;
    this.setData({ loading: true });
    request({
      url: "/api/bounty/list",
      method: "GET",
      data: {
        page: this.data.page,
        size: this.data.size
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const rawItems = res.data?.items || [];
          const items = rawItems.map((item) => this.normalizeItem(item));
          const nextList = append ? this.data.list.concat(items) : items;
          const hasMore = items.length >= this.data.size;
          this.setData({ list: nextList, hasMore });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ list: [], hasMore: false });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ list: [], hasMore: false });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  normalizeItem(item) {
    const recruitTargetCount = Number(item.recruitTargetCount) || 0;
    const recruitCurrentCount = Number(item.recruitCurrentCount) || 0;
    const claimed = !!item.claimed;
    const canClaim = !!item.canClaim;
    const status = item.status || (recruitCurrentCount >= recruitTargetCount ? "FULL" : "OPEN");
    let actionText = "接取悬赏";
    if (claimed) {
      actionText = "已接取";
    } else if (!canClaim && status === "FULL") {
      actionText = "已招满";
    } else if (!canClaim) {
      actionText = "暂不可接";
    }

    return {
      ...item,
      recruitTargetCount,
      recruitCurrentCount,
      rewardDisplay: this.formatAmount(item.rewardMojin),
      totalRewardDisplay: this.formatAmount(item.totalRewardMojin),
      statusLabel: status === "FULL" ? "已招满" : "招募中",
      statusClass: status === "FULL" ? "full" : "open",
      actionText,
      actionDisabled: !canClaim,
      createTimeDisplay: this.formatDate(item.createTime)
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
  goCreate() {
    wx.navigateTo({ url: "/pages/bounty/create" });
  },
  goDetail(e) {
    const bountyId = Number(e.currentTarget.dataset.id);
    if (!bountyId) return;
    wx.navigateTo({ url: `/pages/bounty/detail?id=${bountyId}` });
  },
  claimBounty(e) {
    const bountyId = Number(e.currentTarget.dataset.id);
    const index = Number(e.currentTarget.dataset.index);
    if (!bountyId || Number.isNaN(index)) return;
    const current = this.data.list[index];
    if (!current || current.actionDisabled || this.data.claimingId === bountyId) return;

    this.setData({ claimingId: bountyId });
    request({
      url: `/api/bounty/${bountyId}/claim`,
      method: "POST"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "接取成功", icon: "success" });
          const response = res.data || {};
          const recruitCurrentCount = Number(response.recruitCurrentCount) || current.recruitCurrentCount;
          const status = response.status || current.status;
          this.setData({
            [`list[${index}].recruitCurrentCount`]: recruitCurrentCount,
            [`list[${index}].claimed`]: true,
            [`list[${index}].canClaim`]: false,
            [`list[${index}].actionDisabled`]: true,
            [`list[${index}].actionText`]: "已接取",
            [`list[${index}].statusLabel`]: status === "FULL" ? "已招满" : "招募中",
            [`list[${index}].statusClass`]: status === "FULL" ? "full" : "open"
          });
          return;
        }
        wx.showToast({ title: res.message || "接取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ claimingId: null });
      });
  }
});
