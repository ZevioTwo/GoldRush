const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    editingId: null,
    accounts: [],
    gameTypeIndex: 0,
    gameTypeOptions: ["三角洲行动", "暗区突围", "逃离塔科夫"],
    gameTypeValues: ["DELTA", "AREA18", "TARKOV"],
    form: {
      gameType: "DELTA",
      gameRegion: "",
      gameId: "",
      remark: ""
    }
  },
  onShow() {
    this.fetchAccounts();
  },
  fetchAccounts() {
    request({
      url: "/api/user/game-accounts",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const list = (res.data || []).map((item) => ({
            ...item,
            gameTypeName: this.getGameTypeName(item.gameType)
          }));
          this.setData({ accounts: list });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  getGameTypeName(code) {
    const map = { DELTA: "三角洲行动", AREA18: "暗区突围", TARKOV: "逃离塔科夫" };
    return map[code] || code;
  },
  onGameTypeChange(e) {
    const index = Number(e.detail.value) || 0;
    this.setData({
      gameTypeIndex: index,
      "form.gameType": this.data.gameTypeValues[index]
    });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  validate(form) {
    if (!form.gameType) return "请选择游戏类型";
    if (!form.gameRegion) return "请填写游戏大区";
    if (!form.gameId) return "请填写游戏ID";
    if (form.remark && form.remark.length > 100) return "备注不能超过100字";
    return "";
  },
  submit() {
    if (this.data.loading) return;
    const form = this.data.form;
    const error = this.validate(form);
    if (error) {
      wx.showToast({ title: error, icon: "none" });
      return;
    }

    this.setData({ loading: true });
    const isEdit = !!this.data.editingId;
    const url = isEdit ? `/api/user/game-accounts/${this.data.editingId}` : "/api/user/game-accounts";
    const method = isEdit ? "PUT" : "POST";

    request({
      url,
      method,
      data: form
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: isEdit ? "更新成功" : "新增成功", icon: "success" });
          this.resetForm();
          this.fetchAccounts();
          return;
        }
        wx.showToast({ title: res.message || "保存失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  editAccount(e) {
    const id = e.currentTarget.dataset.id;
    const item = this.data.accounts.find((acc) => acc.id === id);
    if (!item) return;
    const index = this.data.gameTypeValues.indexOf(item.gameType);
    this.setData({
      editingId: item.id,
      gameTypeIndex: index >= 0 ? index : 0,
      form: {
        gameType: item.gameType,
        gameRegion: item.gameRegion,
        gameId: item.gameId,
        remark: item.remark || ""
      }
    });
  },
  deleteAccount(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: "确认删除",
      content: "确定删除该账号吗？",
      success: (res) => {
        if (!res.confirm) return;
        request({
          url: `/api/user/game-accounts/${id}`,
          method: "DELETE"
        })
          .then((resp) => {
            if (resp && (resp.code === 0 || resp.code === 200)) {
              wx.showToast({ title: "删除成功", icon: "success" });
              this.fetchAccounts();
              return;
            }
            wx.showToast({ title: resp.message || "删除失败", icon: "none" });
          })
          .catch(() => {
            wx.showToast({ title: "网络错误", icon: "none" });
          });
      }
    });
  },
  resetForm() {
    this.setData({
      editingId: null,
      gameTypeIndex: 0,
      form: {
        gameType: "DELTA",
        gameRegion: "",
        gameId: "",
        remark: ""
      }
    });
  }
});
