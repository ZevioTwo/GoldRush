const { request } = require("../../utils/request");

const mockContracts = [
  {
    contractId: "8829341",
    contractNo: "8829341",
    status: "ACTIVE",
    statusLabel: "进行中",
    depositAmount: 500,
    displayName: "张三",
    remainingTime: "12:45:30",
    createTime: "2026-10-24 14:30"
  },
  {
    contractId: "8829342",
    contractNo: "8829342",
    status: "PENDING",
    statusLabel: "待生效",
    depositAmount: 200,
    displayName: "李四",
    remainingTime: "--",
    createTime: "2026-10-24 10:12"
  },
  {
    contractId: "8829345",
    contractNo: "8829345",
    status: "DISPUTE",
    statusLabel: "仲裁中",
    depositAmount: 1200,
    displayName: "王五",
    remainingTime: "--",
    createTime: "2026-10-20 09:03"
  },
  {
    contractId: "8829301",
    contractNo: "8829301",
    status: "COMPLETED",
    statusLabel: "已完成",
    depositAmount: 300,
    displayName: "赵六",
    remainingTime: "--",
    createTime: "2026-10-18 16:40"
  }
];

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
  onStatusTabChange(e) {
    const index = Number(e.currentTarget.dataset.index) || 0;
    if (index === this.data.statusIndex) return;
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
            statusLabel: this.getStatusLabel(item.status),
            createTime: this.formatDate(item.createTime),
            completeTime: this.formatDate(item.completeTime),
            displayName: item.counterpartyNickname || item.receiverNickname || item.initiatorNickname || ""
          }));
          const nextList = append ? this.data.list.concat(list) : list;
          const hasMore = list.length >= this.data.size;

          if (!nextList.length) {
            this.setData({ list: mockContracts, hasMore: false });
            return;
          }

          this.setData({ list: nextList, hasMore });

          const missing = nextList.filter((item) => !item.displayName && item.contractId);
          if (missing.length) {
            Promise.all(
              missing.map((item) =>
                this.fetchReceiverName(item.contractId).then((name) => ({
                  contractId: item.contractId,
                  name
                }))
              )
            ).then((results) => {
              const nameMap = results.reduce((acc, cur) => {
                if (cur.name) acc[cur.contractId] = cur.name;
                return acc;
              }, {});
              const patchedList = nextList.map((item) => ({
                ...item,
                displayName: item.displayName || nameMap[item.contractId] || "对方用户"
              }));
              this.setData({ list: patchedList });
            });
          }
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ list: mockContracts, hasMore: false });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ list: mockContracts, hasMore: false });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  fetchReceiverName(contractId) {
    if (!contractId) return Promise.resolve("");
    return request({
      url: `/api/contract/${contractId}`,
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          return res.data?.receiver?.nickname || "";
        }
        return "";
      })
      .catch(() => "");
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
  goDetail(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;
    wx.navigateTo({ url: `/pages/contracts/detail?id=${contractId}` });
  },
  goCreate() {
    wx.navigateTo({ url: "/pages/contracts/create" });
  }
});
