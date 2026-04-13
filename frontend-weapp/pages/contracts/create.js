const { request } = require("../../utils/request");

const DEFAULT_GAME_TYPES = [
  "三角洲行动",
  "DNF",
  "王者荣耀",
  "和平精英",
  "绝地求生",
  "永劫无间",
  "英雄联盟",
  "金铲铲之战",
  "原神",
  "无畏契约",
  "永劫无间手游",
  "魔兽世界",
  "CS2",
  "逃离塔科夫",
  "暗区突围"
];

Page({
  data: {
    loading: false,
    depositRequiredOptions: ["需要", "不需要"],
    depositRequiredIndex: 0,
    gameTypeOptions: DEFAULT_GAME_TYPES,
    gameTypeIndex: 0,
    gameDropdownOpen: false,
    searchTerm: "",
    filteredGames: [],
    minCreditRequired: true,
    minCredit: "",
    form: {
      title: "",
      gameType: DEFAULT_GAME_TYPES[0],
      depositAmount: "",
      termsText: "",
      successCondition: ""
    }
  },
  onLoad() {
    this.setData({ filteredGames: this.data.gameTypeOptions });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  toggleGameDropdown() {
    const gameDropdownOpen = !this.data.gameDropdownOpen;
    this.setData({
      gameDropdownOpen,
      searchTerm: gameDropdownOpen ? "" : this.data.searchTerm,
      filteredGames: gameDropdownOpen ? this.data.gameTypeOptions : this.data.filteredGames
    });
  },
  onGameSearch(e) {
    const term = e.detail.value || "";
    const filtered = this.data.gameTypeOptions.filter((item) =>
      item.toLowerCase().includes(term.toLowerCase())
    );
    this.setData({ searchTerm: term, filteredGames: filtered });
  },
  selectGame(e) {
    const game = e.currentTarget.dataset.game;
    const index = this.data.gameTypeOptions.findIndex((item) => item === game);
    this.setData({
      gameTypeIndex: index >= 0 ? index : 0,
      gameDropdownOpen: false,
      searchTerm: "",
      filteredGames: this.data.gameTypeOptions,
      "form.gameType": game
    });
  },
  onDepositToggle(e) {
    const isRequired = e.detail.value;
    this.setData({
      depositRequiredIndex: isRequired ? 0 : 1,
      "form.depositAmount": isRequired ? this.data.form.depositAmount : ""
    });
  },
  onMinCreditToggle(e) {
    this.setData({ minCreditRequired: e.detail.value });
  },
  onMinCreditInput(e) {
    this.setData({ minCredit: e.detail.value });
  },
  validateForm(form) {
    if (!form.title) return "请填写标题";
    if (form.title.length > 100) return "标题不能超过100字";
    if (!form.gameType) return "请选择游戏类型";
    if (!form.termsText) return "请填写契约详情";

    if (this.data.depositRequiredIndex === 0) {
      if (!form.depositAmount) return "请填写保证金";
      const deposit = Number(form.depositAmount);
      if (Number.isNaN(deposit) || deposit < 0.1 || deposit > 648) return "保证金需在0.1-648之间";
    }

    if (this.data.minCreditRequired) {
      const minCredit = Number(this.data.minCredit);
      if (Number.isNaN(minCredit) || minCredit < 0) return "最低信誉分填写有误";
    }

    if (form.termsText && form.termsText.length > 500) return "契约详情不能超过500字";
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
    const successCondition = this.buildSuccessCondition(form);

    this.setData({ loading: true });
    request({
      url: "/api/contract/create",
      method: "POST",
      data: {
        title: form.title,
        gameType: form.gameType,
        depositAmount,
        minCredit: this.data.minCreditRequired ? this.data.minCredit : 0,
        successCondition
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
  },
  buildSuccessCondition(form) {
    const sections = [];
    if (form.termsText) {
      sections.push(`契约详情：${form.termsText}`);
    }
    if (form.successCondition) {
      sections.push(`达成条件：${form.successCondition}`);
    }
    return sections.join("\n\n");
  },
  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
