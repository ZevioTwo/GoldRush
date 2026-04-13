const { request } = require("../../utils/request");

const POLL_INTERVAL = 1500;
const MAX_POLL_TIMES = 6;

Page({
  data: {
    creditScore: "-",
    creditDisplay: "--",
    creditLevel: "待评估",
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
          const score = res.data?.currentScore ?? "-";
          this.setData({
            creditScore: score,
            creditDisplay: score === "-" ? "--" : String(score),
            creditLevel: this.getCreditLevel(score)
          });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ creditScore: "-", creditDisplay: "--", creditLevel: "待评估" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ creditScore: "-", creditDisplay: "--", creditLevel: "待评估" });
      });
  },
  getCreditLevel(score) {
    const value = Number(score);
    if (!Number.isFinite(value)) return "待评估";
    if (value >= 900) return "极好";
    if (value >= 750) return "优秀";
    if (value >= 600) return "稳定";
    return "待提升";
  },
  selectPreset(e) {
    const index = Number(e.currentTarget.dataset.index) || 0;
    this.setData({ selectedPreset: index });
  },
  choosePayment(e) {
    const method = e.currentTarget.dataset.method;
    if (!method || method === this.data.paymentMethod) return;
    if (method !== "wechat") {
      wx.showToast({ title: "当前仅开通微信支付", icon: "none" });
      return;
    }
    this.setData({ paymentMethod: method });
  },
  submitRecharge() {
    const preset = this.data.presets[this.data.selectedPreset];
    if (!preset) return;
    if (this.data.loading) return;
    if (this.data.paymentMethod !== "wechat") {
      wx.showToast({ title: "当前仅支持微信支付", icon: "none" });
      return;
    }

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
          throw new Error(res.message || "充值失败");
        }
        const orderNo = res.data?.orderNo;
        const payParams = res.data?.payParams;
        if (!orderNo || !payParams) {
          throw new Error("支付参数缺失");
        }
        return this.invokeWechatPayment(orderNo, payParams);
      })
      .then(() => {
        this.fetchCredit();
        wx.showToast({ title: "充值成功", icon: "success" });
      })
      .catch((err) => {
        const message = err && err.message ? err.message : "";
        if (message) {
          wx.showToast({ title: message, icon: "none" });
          return;
        }
        this.handlePaymentError(err);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },
  invokeWechatPayment(orderNo, payParams) {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: String(payParams.timeStamp || ""),
        nonceStr: payParams.nonceStr || "",
        package: payParams.package || "",
        signType: payParams.signType || "RSA",
        paySign: payParams.paySign || "",
        success: () => {
          this.pollPaymentStatus(orderNo)
            .then(resolve)
            .catch(reject);
        },
        fail: reject
      });
    });
  },
  pollPaymentStatus(orderNo, attempt = 0) {
    return request({
      url: `/api/payment/status/${orderNo}`,
      method: "GET"
    }).then((res) => {
      if (!(res && (res.code === 0 || res.code === 200))) {
        throw new Error(res.message || "支付结果查询失败");
      }

      const data = res.data || {};
      if (data.paid || data.paymentStatus === "SUCCESS") {
        return data;
      }

      if (data.paymentStatus === "CLOSED" || data.paymentStatus === "FAILED") {
        throw new Error(data.tradeStateDesc || "支付未完成");
      }

      if (attempt >= MAX_POLL_TIMES) {
        throw new Error("支付结果确认中，请稍后刷新查看");
      }

      return this.delay(POLL_INTERVAL).then(() => this.pollPaymentStatus(orderNo, attempt + 1));
    });
  },
  handlePaymentError(err) {
    const errMsg = err && err.errMsg ? err.errMsg : "";
    if (errMsg.includes("cancel")) {
      wx.showToast({ title: "已取消支付", icon: "none" });
      return;
    }
    wx.showToast({
      title: errMsg || "网络错误",
      icon: "none"
    });
  },
  delay(duration) {
    return new Promise((resolve) => {
      setTimeout(resolve, duration);
    });
  },
  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
