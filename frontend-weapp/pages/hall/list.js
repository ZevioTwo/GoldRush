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
    gameTabs: [
      { label: "全部", value: "all" },
      { label: "三角洲行动", value: "三角洲行动" },
      { label: "和平精英", value: "和平精英" },
      { label: "王者荣耀", value: "王者荣耀" },
      { label: "绝地求生", value: "绝地求生" },
      { label: "无畏契约", value: "无畏契约" },
      { label: "DNF", value: "dnf" },
      { label: "穿越火线", value: "穿越火线" },
      { label: "逃离塔科夫", value: "逃离塔科夫" },
      { label: "使命召唤", value: "使命召唤" },
      { label: "APEX 英雄", value: "APEX 英雄" },
      { label: "英雄联盟", value: "英雄联盟" },
      { label: "流放之路", value: "流放之路" },
      { label: "魔兽世界", value: "魔兽世界" },
      { label: "命运2", value: "命运2" },
      { label: "逆水寒", value: "逆水寒" },
      { label: "梦幻西游", value: "梦幻西游" },
      { label: "原神", value: "原神" },
      { label: "崩坏", value: "崩坏" },
      { label: "暗区突围", value: "暗区突围" },
      { label: "萤火突击", value: "萤火突击" },
      { label: "灰区行动", value: "灰区行动" },
      { label: "永劫无间", value: "永劫无间" },
      { label: "剑网3", value: "剑网3" },
      { label: "星铁", value: "星铁" },
      { label: "鸣潮", value: "鸣潮" },
      { label: "绝区零", value: "绝区零" },
      { label: "其他", value: "其他" }
    ]
  },
  onShow() {
    this.resetAndFetch();
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/hall/list");
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
          const list = rawList.map((item) => ({
            ...item,
            statusLabel: this.getStatusLabel(item.status),
            createTime: this.formatDate(item.createTime),
            requirementPreview: this.truncateRequirement(item.successCondition)
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
    this.setData({ activeTab: tab }, () => this.resetAndFetch());
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
  }
});
