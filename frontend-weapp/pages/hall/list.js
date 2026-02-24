const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    keyword: "",
    contractNo: "",
    initiatorGameId: "",
    loading: false,
    hasMore: true
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
    if (this.data.keyword) {
      data.keyword = this.data.keyword;
    }
    if (this.data.contractNo) {
      data.contractNo = this.data.contractNo;
    }
    if (this.data.initiatorGameId) {
      data.initiatorGameId = this.data.initiatorGameId;
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
            createTime: this.formatDate(item.createTime)
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
  onContractNoInput(e) {
    this.setData({ contractNo: e.detail.value });
  },
  onInitiatorInput(e) {
    this.setData({ initiatorGameId: e.detail.value });
  },
  getGameTypeName(code) {
    const map = { DELTA: "三角洲行动", AREA18: "暗区突围", TARKOV: "逃离塔科夫" };
    return map[code] || code;
  },
  onSearch() {
    this.resetAndFetch();
  },
  acceptContract(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;

    request({
      url: "/api/user/game-accounts",
      method: "GET"
    })
      .then((res) => {
        if (!res || (res.code !== 0 && res.code !== 200)) {
          wx.showToast({ title: res?.message || "获取账号失败", icon: "none" });
          return null;
        }
        const accounts = res.data || [];
        if (accounts.length === 0) {
          wx.showToast({ title: "请先在账号管理中添加账号", icon: "none" });
          return null;
        }
        const options = accounts.map((item) => `${this.getGameTypeName(item.gameType)} | ${item.gameRegion} | ${item.gameId}`);
        return new Promise((resolve) => {
          wx.showActionSheet({
            itemList: options,
            success: (sheet) => resolve(accounts[sheet.tapIndex]),
            fail: () => resolve(null)
          });
        });
      })
      .then((account) => {
        if (!account) return;
        request({
          url: "/api/contract/accept",
          method: "POST",
          data: {
            contractId,
            receiverAccountId: account.id
          }
        })
          .then((resp) => {
            if (resp && (resp.code === 0 || resp.code === 200)) {
              wx.showToast({ title: "接单成功", icon: "success" });
              this.fetchList();
              return;
            }
            wx.showToast({ title: resp.message || "接单失败", icon: "none" });
          })
          .catch(() => {
            wx.showToast({ title: "网络错误", icon: "none" });
          });
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
  }
});
