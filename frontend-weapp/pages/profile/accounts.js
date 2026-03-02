const { request } = require("../../utils/request");

Page({
  data: {
    accounts: [],
    editingId: null,
    loading: false,
    form: {
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
          this.setData({ accounts: res.data || [] });
          return;
        }
        wx.showToast({ title: res.message || "获取账号失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  validate(form) {
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
    request({
      url: isEdit ? `/api/user/game-accounts/${this.data.editingId}` : "/api/user/game-accounts",
      method: isEdit ? "PUT" : "POST",
      data: {
        remark: form.remark || undefined
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: isEdit ? "更新成功" : "新增成功", icon: "success" });
          this.setData({ editingId: null, form: { remark: "" } });
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
    const item = this.data.accounts.find((account) => account.id === id);
    if (!item) return;
    this.setData({
      editingId: item.id,
      form: {
        remark: item.remark || ""
      }
    });
  },
  deleteAccount(e) {
    const id = e.currentTarget.dataset.id;
    if (!id) return;
    wx.showModal({
      title: "确认删除",
      content: "删除后无法恢复",
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
  }
});
