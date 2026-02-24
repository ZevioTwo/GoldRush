const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    keyword: "",
    contractNo: "",
    initiatorGameId: ""
  },
  onShow() {
    this.fetchList();
  },
  prevPage() {
    if (this.data.page <= 1) return;
    this.setData({ page: this.data.page - 1 }, () => this.fetchList());
  },
  nextPage() {
    this.setData({ page: this.data.page + 1 }, () => this.fetchList());
  },
  fetchList() {
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
          this.setData({ list });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
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
  onSearch() {
    this.setData({ page: 1 }, () => this.fetchList());
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
