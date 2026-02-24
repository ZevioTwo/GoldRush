const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    accounts: [],
    accountOptions: ["请选择我的账号"],
    accountIndex: 0,
    form: {
      initiatorAccountId: null,
      gameType: "",
      gameTypeName: "",
      gameRegion: "",
      title: "",
      depositAmount: "",
      guaranteeItem: "",
      successCondition: "",
      failureCondition: ""
    }
  },
  onLoad() {
    this.fetchAccounts();
  },
  fetchAccounts() {
    request({
      url: "/api/user/game-accounts",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const accounts = res.data || [];
          const options = ["请选择我的账号"].concat(
            accounts.map((item) => `${this.getGameTypeName(item.gameType)} | ${item.gameRegion} | ${item.gameId}`)
          );
          this.setData({ accounts, accountOptions: options });
          return;
        }
        wx.showToast({ title: res.message || "获取账号失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  getGameTypeName(code) {
    const map = { DELTA: "三角洲行动", AREA18: "暗区突围", TARKOV: "逃离塔科夫" };
    return map[code] || code;
  },
  onAccountChange(e) {
    const index = Number(e.detail.value) || 0;
    if (index === 0) {
      this.setData({
        accountIndex: 0,
        form: {
          ...this.data.form,
          initiatorAccountId: null,
          gameType: "",
          gameTypeName: "",
          gameRegion: ""
        }
      });
      return;
    }
    const account = this.data.accounts[index - 1];
    this.setData({
      accountIndex: index,
      form: {
        ...this.data.form,
        initiatorAccountId: account.id,
        gameType: account.gameType,
        gameTypeName: this.getGameTypeName(account.gameType),
        gameRegion: account.gameRegion
      }
    });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  validateForm(form) {
    if (!form.initiatorAccountId) return "请选择我的账号";
    if (!form.gameRegion) return "请选择游戏大区";
    if (!form.gameType) return "请选择游戏类型";
    if (!form.title) return "请填写标题";
    if (form.title.length > 100) return "标题不能超过100字";
    if (!form.depositAmount) return "请填写保证金";
    const deposit = Number(form.depositAmount);
    if (Number.isNaN(deposit) || deposit < 10 || deposit > 200) return "保证金需在10-200之间";
    if (!form.guaranteeItem) return "请填写保底物品";
    if (form.successCondition && form.successCondition.length > 500) return "成功条件不能超过500字";
    if (form.failureCondition && form.failureCondition.length > 500) return "失败条件不能超过500字";
    return "";
  },
  submit() {
    if (this.data.loading) return;
    const form = this.data.form;
    const error = this.validateForm(form);
    if (error) {
      wx.showToast({ title: error, icon: "none" });
      return;
    }

    this.setData({ loading: true });
    request({
      url: "/api/contract/create",
      method: "POST",
      data: {
        initiatorAccountId: form.initiatorAccountId,
        gameType: form.gameType,
        gameRegion: form.gameRegion,
        title: form.title,
        depositAmount: form.depositAmount,
        guaranteeItem: form.guaranteeItem,
        successCondition: form.successCondition,
        failureCondition: form.failureCondition
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "创建成功", icon: "success" });
          wx.switchTab({ url: "/pages/contracts/list" });
          return;
        }
        wx.showToast({ title: res.message || "创建失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
