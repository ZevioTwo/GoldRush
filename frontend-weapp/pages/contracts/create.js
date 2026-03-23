const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    depositRequiredOptions: ["需要", "不需要"],
    depositRequiredIndex: 0,
    gameTypeOptions: [
      "三角洲行动",
      "和平精英",
      "王者荣耀",
      "绝地求生",
      "无畏契约",
      "dnf",
      "穿越火线",
      "逃离塔科夫",
      "使命召唤",
      "APEX 英雄",
      "英雄联盟",
      "流放之路",
      "魔兽世界",
      "命运2",
      "逆水寒",
      "梦幻西游",
      "原神",
      "崩坏",
      "暗区突围",
      "萤火突击",
      "灰区行动",
      "永劫无间",
      "剑网3",
      "星铁",
      "鸣潮",
      "绝区零",
      "其他"
    ],
    gameTypeIndex: 0,
    form: {
      title: "",
      gameType: "三角洲行动",
      depositAmount: "",
      successCondition: ""
    }
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  onGameTypeChange(e) {
    const index = Number(e.detail.value) || 0;
    const gameType = this.data.gameTypeOptions[index] || this.data.gameTypeOptions[0];
    this.setData({
      gameTypeIndex: index,
      "form.gameType": gameType
    });
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
    if (!form.gameType) return "请选择游戏类型";

    if (this.data.depositRequiredIndex === 0) {
      if (!form.depositAmount) return "请填写保证金";
      const deposit = Number(form.depositAmount);
      if (Number.isNaN(deposit) || deposit < 0.1 || deposit > 648) return "保证金需在0.1-648之间";
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
        gameType: form.gameType,
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
