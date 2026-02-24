const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    statusIndex: 0,
    statusOptions: ["全部", "我发起", "我接单"],
    statusValues: ["ALL", "INITIATED", "RECEIVED"],
    loading: false,
    hasMore: true
  },
  onShow() {
    this.resetAndFetch();
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/contracts/list");
    }
  },
  onStatusChange(e) {
    const index = Number(e.detail.value) || 0;
    this.setData({ statusIndex: index }, () => this.resetAndFetch());
  },
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.fetchList(true));
  },
  resetAndFetch() {
    this.setData({ page: 1, list: [], hasMore: true }, () => this.fetchList());
  },
  fetchList(append = false) {
    const filter = this.data.statusValues[this.data.statusIndex];
    const data = {
      page: this.data.page,
      size: this.data.size,
      scope: filter
    };

    if (data.scope === "ALL") {
      delete data.scope;
    }

    if (this.data.loading) return;
    this.setData({ loading: true });
    request({
      url: "/api/contract/list",
      method: "GET",
      data
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const rawList = res.data?.contracts || [];
          const list = rawList.map((item) => ({
            ...item,
            createTime: this.formatDate(item.createTime),
            completeTime: this.formatDate(item.completeTime)
          }));
          const nextList = append ? this.data.list.concat(list) : list;
          const hasMore = list.length >= this.data.size;
          this.setData({ list: nextList, hasMore });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
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
  },
  goDetail(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;
    wx.navigateTo({ url: `/pages/contracts/detail?id=${contractId}` });
  },
  goCreate() {
    wx.navigateTo({ url: "/pages/contracts/create" });
  }
});
