const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    keyword: "",
    contractNo: "",
    loading: false,
    hasMore: true,
    activeTab: "all",
    showSearch: false,
    dropdownOpen: false,
    searchTerm: "",
    filteredGames: [],
    activeTabLabel: "选择游戏类型",
    gameTabs: [
      { label: "全部", value: "all" },
      { label: "三角洲行动", value: "三角洲行动" },
      { label: "DNF", value: "DNF" },
      { label: "王者荣耀", value: "王者荣耀" },
      { label: "和平精英", value: "和平精英" },
      { label: "绝地求生", value: "绝地求生" },
      { label: "永劫无间", value: "永劫无间" },
      { label: "英雄联盟", value: "英雄联盟" },
      { label: "金铲铲之战", value: "金铲铲之战" },
      { label: "原神", value: "原神" },
      { label: "无畏契约", value: "无畏契约" },
      { label: "永劫无间手游", value: "永劫无间手游" },
      { label: "魔兽世界", value: "魔兽世界" },
      { label: "CS2", value: "CS2" },
      { label: "逃离塔科夫", value: "逃离塔科夫" },
      { label: "暗区突围", value: "暗区突围" }
    ]
  },
  onShow() {
    this.resetAndFetch();
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/hall/list");
    }
    this.setData({ filteredGames: this.data.gameTabs });
  },
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.fetchList(true));
  },
  resetAndFetch() {
    this.setData({ page: 1, list: [], hasMore: true }, () => this.fetchList());
  },
  fetchList(append = false) {
    const data = {
      page: this.data.page,
      size: this.data.size
    };
    if (this.data.activeTab !== "all") {
      data.gameType = this.data.activeTab;
    }
    if (this.data.keyword) {
      data.keyword = this.data.keyword;
    }

    if (this.data.loading) return;
    this.setData({ loading: true });
    request({
      url: "/api/contract/hall",
      method: "GET",
      data
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const rawList = res.data?.contracts || [];
          const list = rawList.map((item) => this.normalizeJob({
            ...item,
            statusLabel: this.getStatusLabel(item.status),
            createTime: this.formatDate(item.createTime),
            requirementPreview: this.truncateRequirement(item.successCondition)
          }));
          const nextList = append ? this.data.list.concat(list) : list;
          const hasMore = list.length >= this.data.size;

          if (!nextList.length) {
            this.setData({ list: [], hasMore: false });
            return;
          }

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
  normalizeJob(item) {
    const statusMap = {
      ACTIVE: "success",
      IN_GAME: "success",
      PENDING: "warning",
      PAID: "info",
      COMPLETED: "success",
      CANCELLED: "danger",
      DISPUTE: "danger",
      VIOLATED: "danger"
    };
    return {
      ...item,
      title: item.title || item.contractTitle || "未命名契约",
      game: item.game || item.gameType || "未分类",
      boss: item.boss || item.initiatorNickname || item.publisherNickname || "匿名发起人",
      credit: item.credit || item.minCredit || 0,
      deposit: item.deposit || item.depositAmount || 0,
      remaining: item.remaining || item.remainingTime || "",
      desc: item.desc || item.requirementPreview || item.successCondition || "暂无详细要求说明",
      image: item.image || item.coverImage || "",
      statusClass: item.statusClass || statusMap[item.status] || "warning"
    };
  },
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },
  onKeywordConfirm() {
    this.resetAndFetch();
  },
  onContractNoInput(e) {
    this.setData({ contractNo: e.detail.value });
  },
  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    if (!tab || tab === this.data.activeTab) return;
    const current = this.data.gameTabs.find((item) => item.value === tab);
    this.setData({
      activeTab: tab,
      activeTabLabel: current ? current.label : "选择游戏类型",
      dropdownOpen: false
    }, () => this.resetAndFetch());
  },
  onSearch() {
    this.resetAndFetch();
  },
  onSearchIcon() {
    this.setData({ showSearch: !this.data.showSearch }, () => {
      if (this.data.showSearch && !this.data.keyword) {
        this.resetAndFetch();
      }
    });
  },
  toggleDropdown() {
    this.setData({ dropdownOpen: !this.data.dropdownOpen });
  },
  onSearchInput(e) {
    const term = e.detail.value || "";
    const filtered = this.data.gameTabs.filter((item) =>
      item.label.toLowerCase().includes(term.toLowerCase())
    );
    this.setData({ searchTerm: term, filteredGames: filtered });
  },
  truncateRequirement(text) {
    if (!text) return "";
    const trimmed = String(text).trim();
    if (trimmed.length <= 30) return trimmed;
    return `${trimmed.slice(0, 30)}...`;
  },
  goDetail(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;
    wx.navigateTo({ url: `/pages/contracts/detail?id=${contractId}&from=hall` });
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
    if (Number.isNaN(date.getTime())) return "";
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
  }
});
