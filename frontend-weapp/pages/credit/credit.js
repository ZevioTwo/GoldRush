const { request } = require("../../utils/request");

Page({
  data: {
    creditScore: "985",
    presets: [
      { points: 100, price: 100, label: "初级契约包", bonus: "送5分" },
      { points: 500, price: 488, label: "专业老板包", bonus: "送30分", popular: true },
      { points: 1000, price: 958, label: "顶奢打手包", bonus: "送80分" },
      { points: 2000, price: 1888, label: "荣耀金牌包", bonus: "送200分" }
    ],
    selectedPreset: 1,
    paymentMethod: "wechat",
    loading: false
  },
  onShow() {
    this.fetchCredit();
  },
  fetchCredit() {
    request({
      url: "/api/user/credit",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ creditScore: res.data?.currentScore ?? "985" });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ creditScore: "985" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ creditScore: "985" });
      });
  },
  selectPreset(e) {
    const index = Number(e.currentTarget.dataset.index) || 0;
    this.setData({ selectedPreset: index });
  },
  choosePayment(e) {
    const method = e.currentTarget.dataset.method;
    if (!method || method === this.data.paymentMethod) return;
    this.setData({ paymentMethod: method });
  },
  submitRecharge() {
    const preset = this.data.presets[this.data.selectedPreset];
    if (!preset) return;
    if (this.data.loading) return;
    this.setData({ loading: true });
    request({
      url: "/api/payment/prepay",
      method: "POST",
      data: {
        orderType: "CREDIT_RECHARGE",
        amount: Number(preset.price).toFixed(2),
        payMethod: this.data.paymentMethod
      }
    })
      .then((res) => {
        if (!(res && (res.code === 0 || res.code === 200))) {
          wx.showToast({ title: res.message || "充值失败", icon: "none" });
          return;
        }
        wx.showToast({ title: "支付流程模拟", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
