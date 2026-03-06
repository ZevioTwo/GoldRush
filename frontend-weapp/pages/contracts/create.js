const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    depositRequiredOptions: ["需要", "不需要"],
    depositRequiredIndex: 0,
    form: {
      title: "",
      depositAmount: "",
      successCondition: ""
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  onDepositRequiredChange(e) {
    const index = Number(e.detail.value);
    const isRequired = index === 0;
    this.setData({
      depositRequiredIndex: index,
      "form.depositAmount": isRequired ? this.data.form.depositAmount : ""
    });
  },
  validateForm(form) {
    if (!form.title) return "请填写标题";
    if (form.title.length > 100) return "标题不能超过100字";

    if (this.data.depositRequiredIndex === 0) {
      if (!form.depositAmount) return "请填写保证金";
      const deposit = Number(form.depositAmount);
      if (Number.isNaN(deposit) || deposit < 10 || deposit > 200) return "保证金需在10-200之间";
    }

    if (form.successCondition && form.successCondition.length > 500) return "契约达成条件不能超过500字";
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

    const depositAmount = this.data.depositRequiredIndex === 0 ? form.depositAmount : 0;

    this.setData({ loading: true });
    request({
      url: "/api/contract/create",
      method: "POST",
      data: {
        title: form.title,
        depositAmount,
        successCondition: form.successCondition
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
