const { request } = require("../../utils/request");

const POLL_INTERVAL = 1500;
const MAX_POLL_TIMES = 6;

Page({
  data: {
    profile: {},
    creditDisplay: "--",
    mojinBalanceDisplay: "0.00",
    presets: [
      { mojin: 100, price: 10, label: "试水补给包", note: "到账100摸金币" },
      { mojin: 500, price: 50, label: "常用储备包", note: "到账500摸金币", popular: true },
      { mojin: 1000, price: 100, label: "进阶冲刺包", note: "到账1000摸金币" },
      { mojin: 2000, price: 200, label: "重装储备包", note: "到账2000摸金币" }
    ],
    selectedPreset: 1,
    paymentMethod: "wechat",
    loading: false
  },
  onShow() {
    this.fetchProfile();
  },
  fetchProfile() {
    request({
      url: "/api/user/profile",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const profile = res.data || {};
          this.setData({
            profile,
            creditDisplay: profile.creditScore ?? "--",
            mojinBalanceDisplay: this.formatBalance(profile.mojinBalance)
          });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
        this.setData({ profile: {}, creditDisplay: "--", mojinBalanceDisplay: "0.00" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ profile: {}, creditDisplay: "--", mojinBalanceDisplay: "0.00" });
      });
  },
  formatBalance(value) {
    const amount = Number(value);
    if (!Number.isFinite(amount)) return "0.00";
    return amount.toFixed(2);
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
        this.fetchProfile();
        wx.showToast({ title: "充值成功，摸金币已到账", icon: "success" });
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
  }
});
