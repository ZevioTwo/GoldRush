const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    rawList: [],
    page: 1,
    size: 10,
    statusIndex: 0,
    statusTabs: ["全部", "进行中", "待生效", "已完成", "仲裁中"],
    keyword: "",
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
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value || "" }, () => this.applyFilters());
  },
  onKeywordConfirm() {
    this.applyFilters();
  },
  onStatusTabChange(e) {
    const index = Number(e.currentTarget.dataset.index) || 0;
    if (index === this.data.statusIndex) return;
    this.setData({ statusIndex: index }, () => this.applyFilters());
  },
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.fetchList(true));
  },
  resetAndFetch() {
    this.setData({ page: 1, list: [], rawList: [], hasMore: true }, () => this.fetchList());
  },
  fetchList(append = false) {
    const data = {
      page: this.data.page,
      size: this.data.size
    };

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
          const list = rawList.map((item) => this.normalizeContract({
            ...item,
            createTime: this.formatDate(item.createTime),
            completeTime: this.formatDate(item.completeTime),
            displayName: item.counterpartyNickname || item.receiverNickname || item.initiatorNickname || ""
          }));
          const nextRawList = append ? this.data.rawList.concat(list) : list;
          const hasMore = list.length >= this.data.size;

          if (!nextRawList.length) {
            this.setData({ rawList: [], hasMore: false }, () => this.applyFilters([]));
            return;
          }

          this.setData({ rawList: nextRawList, hasMore }, () => this.applyFilters(nextRawList));

          const missing = nextRawList.filter((item) => !item.displayName && item.contractId);
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
              const patchedRawList = nextRawList.map((item) =>
                this.normalizeContract({
                  ...item,
                  displayName: item.displayName || nameMap[item.contractId] || "对方用户"
                })
              );
              this.setData({ rawList: patchedRawList }, () => this.applyFilters(patchedRawList));
            });
          }
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ rawList: [], hasMore: false }, () => this.applyFilters([]));
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ rawList: [], hasMore: false }, () => this.applyFilters([]));
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  applyFilters(sourceList) {
    const targetList = sourceList || this.data.rawList || [];
    const activeGroup = ["all", "ongoing", "pending", "completed", "arbitration"][this.data.statusIndex] || "all";
    const keyword = (this.data.keyword || "").trim().toLowerCase();

    const filtered = targetList.filter((item) => {
      const matchedGroup = activeGroup === "all" ? true : item.statusGroup === activeGroup;
      if (!matchedGroup) return false;
      if (!keyword) return true;
      const haystack = [
        item.contractNo,
        item.contractId,
        item.displayName,
        item.statusLabel
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(keyword);
    });

    this.setData({ list: filtered });
  },
  normalizeContract(item) {
    const statusInfo = this.getStatusInfo(item.status);
    const creditInfo = this.getCreditInfo(item.creditPoints ?? item.credit ?? item.minCredit ?? 650);
    return {
      ...item,
      contractNo: item.contractNo || item.contractId || "",
      displayName: item.displayName || item.counterpartyNickname || item.receiverNickname || item.initiatorNickname || "对方用户",
      depositAmount: Number(item.depositAmount || item.deposit || 0),
      remainingTime: item.remainingTime || item.remaining || "",
      statusLabel: statusInfo.label,
      statusGroup: statusInfo.group,
      statusClass: statusInfo.className,
      footerLabel: statusInfo.footerLabel(item),
      footerIcon: statusInfo.footerIcon,
      creditTag: creditInfo.label,
      creditClass: creditInfo.className
    };
  },
  getStatusInfo(status) {
    const defaultFooter = (item) => item.remainingTime || item.remaining || "";
    const config = {
      PENDING: {
        group: "pending",
        label: "待生效",
        className: "status-info",
        footerIcon: "👤",
        footerLabel: () => "等待对方确认"
      },
      PAID: {
        group: "pending",
        label: "待生效",
        className: "status-info",
        footerIcon: "👤",
        footerLabel: () => "等待双方开始履约"
      },
      ACTIVE: {
        group: "ongoing",
        label: "进行中",
        className: "status-warning",
        footerIcon: "⏱",
        footerLabel: (item) => `剩余时间: ${defaultFooter(item)}`
      },
      IN_GAME: {
        group: "ongoing",
        label: "进行中",
        className: "status-warning",
        footerIcon: "⏱",
        footerLabel: (item) => `剩余时间: ${defaultFooter(item)}`
      },
      COMPLETED: {
        group: "completed",
        label: "已完成",
        className: "status-neutral",
        footerIcon: "✓",
        footerLabel: () => "契约已圆满结束"
      },
      CANCELLED: {
        group: "completed",
        label: "已取消",
        className: "status-neutral",
        footerIcon: "•",
        footerLabel: () => "契约已取消"
      },
      DISPUTE: {
        group: "arbitration",
        label: "仲裁中",
        className: "status-danger",
        footerIcon: "⚖",
        footerLabel: () => "官方正在介入处理"
      },
      VIOLATED: {
        group: "arbitration",
        label: "仲裁中",
        className: "status-danger",
        footerIcon: "⚖",
        footerLabel: () => "违约争议待平台裁定"
      }
    };
    return config[status] || {
      group: "ongoing",
      label: status || "进行中",
      className: "status-warning",
      footerIcon: "⏱",
      footerLabel: (item) => `剩余时间: ${defaultFooter(item)}`
    };
  },
  getCreditInfo(score) {
    const value = Number(score);
    if (!Number.isFinite(value) || value <= 0) {
      return { label: "无保户", className: "credit-none" };
    }
    if (value < 100) {
      return { label: "低保户", className: "credit-low" };
    }
    if (value < 1000) {
      return { label: "高保户", className: "credit-high" };
    }
    return { label: "特保户", className: "credit-special" };
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
  goDetail(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;
    wx.navigateTo({ url: `/pages/contracts/detail?id=${contractId}` });
  },
  goCreate() {
    wx.navigateTo({ url: "/pages/contracts/create" });
  }
});
