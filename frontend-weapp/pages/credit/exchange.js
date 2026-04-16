const { request } = require("../../utils/request");

Page({
  data: {
    profile: {},
    creditDisplay: "--",
    mojinBalanceDisplay: "0.00",
    balanceNumber: 0,
    buttonText: "立即兑换",
    presets: [
      { mojin: 100, credit: 10, label: "试水兑换" },
      { mojin: 500, credit: 50, label: "常用兑换", popular: true },
      { mojin: 1000, credit: 100, label: "进阶兑换" },
      { mojin: 2000, credit: 200, label: "大额兑换" }
    ],
    selectedPreset: 1,
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
          const balanceNumber = this.parseAmount(profile.mojinBalance);
          this.setData({
            profile,
            creditDisplay: profile.creditScore ?? "--",
            mojinBalanceDisplay: this.formatBalance(profile.mojinBalance),
            balanceNumber
          });
          this.updateButtonText(this.data.selectedPreset, balanceNumber);
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  parseAmount(value) {
    const amount = Number(value);
    return Number.isFinite(amount) ? amount : 0;
  },
  formatBalance(value) {
    const amount = this.parseAmount(value);
    return amount.toFixed(2);
  },
  updateButtonText(index, balanceNumber = this.data.balanceNumber) {
    const preset = this.data.presets[index];
    if (!preset) {
      this.setData({ buttonText: "立即兑换" });
      return;
    }
    if (balanceNumber < preset.mojin) {
      this.setData({ buttonText: `余额不足，还差 ${preset.mojin - balanceNumber} 金币` });
      return;
    }
    this.setData({ buttonText: `消耗 ${preset.mojin} 金币兑换 ${preset.credit} 信誉分` });
  },
  selectPreset(e) {
    const index = Number(e.currentTarget.dataset.index) || 0;
    this.setData({ selectedPreset: index });
    this.updateButtonText(index);
  },
  submitExchange() {
    const preset = this.data.presets[this.data.selectedPreset];
    if (!preset || this.data.loading) return;
    if (this.data.balanceNumber < preset.mojin) {
      wx.showToast({ title: "摸金币不足，请先充值", icon: "none" });
      return;
    }

    this.setData({ loading: true });
    request({
      url: "/api/user/credit/exchange",
      method: "POST",
      data: {
        mojinAmount: preset.mojin
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: `已兑换${preset.credit}信誉分`, icon: "success" });
          this.fetchProfile();
          return;
        }
        wx.showToast({ title: res.message || "兑换失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
