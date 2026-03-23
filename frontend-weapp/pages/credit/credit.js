const { request } = require("../../utils/request");

Page({
  data: {
    creditScore: "-",
    profile: {},
    amount: "",
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
          this.setData({ creditScore: res.data?.currentScore ?? "-" });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  onAmountInput(e) {
    this.setData({ amount: e.detail.value });
  },
  submitRecharge() {
    if (this.data.loading) return;
    const amount = Number(this.data.amount);
    if (!amount || Number.isNaN(amount) || amount <= 0) {
      wx.showToast({ title: "请输入正确金额", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    request({
      url: "/api/payment/prepay",
      method: "POST",
      data: {
        orderType: "CREDIT_RECHARGE",
        amount: Number(amount).toFixed(2)
      }
    })
      .then((res) => {
        if (!(res && (res.code === 0 || res.code === 200))) {
          wx.showToast({ title: res.message || "充值失败", icon: "none" });
          return;
        }
        const payParams = res.data?.payParams || {};
        if (!payParams.timeStamp) {
          wx.showToast({ title: "支付参数异常", icon: "none" });
          return;
        }
        wx.requestPayment({
          timeStamp: payParams.timeStamp,
          nonceStr: payParams.nonceStr,
          package: payParams.package || payParams.packageValue || payParams.prepay_id,
          signType: payParams.signType || "RSA",
          paySign: payParams.paySign,
          success: () => {
            wx.showToast({ title: "支付成功", icon: "success" });
            setTimeout(() => {
              this.fetchCredit();
              this.setData({ amount: "" });
            }, 600);
          },
          fail: () => {
            wx.showToast({ title: "支付取消", icon: "none" });
          }
        });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
