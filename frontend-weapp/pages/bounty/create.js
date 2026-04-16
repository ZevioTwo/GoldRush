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
    mojinBalanceDisplay: "0.00",
    balanceNumber: 0,
    totalDeductionDisplay: "0.00",
    balanceEnough: true,
    gameTypeOptions: DEFAULT_GAME_TYPES,
    gameTypeIndex: 0,
    gameDropdownOpen: false,
    searchTerm: "",
    filteredGames: [],
    form: {
      targetRoleName: "",
      title: "",
      rewardMojin: "",
      gameType: DEFAULT_GAME_TYPES[0],
      description: "",
      recruitTargetCount: ""
    }
  },
  onLoad() {
    this.setData({ filteredGames: this.data.gameTypeOptions });
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
          const balanceNumber = this.parseAmount(res.data?.mojinBalance);
          this.setData({
            mojinBalanceDisplay: balanceNumber.toFixed(2),
            balanceNumber
          }, () => this.updateSummary());
          return;
        }
        wx.showToast({ title: res.message || "获取余额失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value || "" }, () => this.updateSummary());
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
    const filteredGames = this.data.gameTypeOptions.filter((item) =>
      item.toLowerCase().includes(term.toLowerCase())
    );
    this.setData({ searchTerm: term, filteredGames });
  },
  selectGame(e) {
    const game = e.currentTarget.dataset.game;
    const gameTypeIndex = this.data.gameTypeOptions.findIndex((item) => item === game);
    this.setData({
      gameTypeIndex: gameTypeIndex >= 0 ? gameTypeIndex : 0,
      gameDropdownOpen: false,
      searchTerm: "",
      filteredGames: this.data.gameTypeOptions,
      "form.gameType": game
    });
  },
  parseAmount(value) {
    const amount = Number(value);
    return Number.isFinite(amount) ? amount : 0;
  },
  updateSummary() {
    const reward = this.parseAmount(this.data.form.rewardMojin);
    const recruitCount = Number(this.data.form.recruitTargetCount);
    const total = reward > 0 && Number.isFinite(recruitCount) && recruitCount > 0
      ? reward * recruitCount
      : 0;
    this.setData({
      totalDeductionDisplay: total.toFixed(2),
      balanceEnough: this.data.balanceNumber >= total
    });
  },
  validateForm() {
    const form = this.data.form;
    if (!form.targetRoleName.trim()) return "请填写被悬赏角色名称";
    if (!form.title.trim()) return "请填写悬赏标题";
    if (!form.gameType.trim()) return "请选择游戏类型";
    if (!form.description.trim()) return "请填写悬赏描述";

    const reward = this.parseAmount(form.rewardMojin);
    if (reward <= 0) return "请填写正确的单人悬赏摸金币";

    const recruitCount = Number(form.recruitTargetCount);
    if (!Number.isInteger(recruitCount) || recruitCount <= 0) return "请填写正确的招募数量";

    const total = reward * recruitCount;
    if (this.data.balanceNumber < total) return "摸金币余额不足";
    return "";
  },
  submit() {
    if (this.data.loading) return;
    const error = this.validateForm();
    if (error) {
      wx.showToast({ title: error, icon: "none" });
      return;
    }

    const form = this.data.form;
    this.setData({ loading: true });
    request({
      url: "/api/bounty/create",
      method: "POST",
      data: {
        targetRoleName: form.targetRoleName.trim(),
        title: form.title.trim(),
        rewardMojin: Number(this.parseAmount(form.rewardMojin)).toFixed(2),
        gameType: form.gameType.trim(),
        description: form.description.trim(),
        recruitTargetCount: Number(form.recruitTargetCount)
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "悬赏发布成功", icon: "success" });
          setTimeout(() => {
            wx.switchTab({ url: "/pages/bounty/index" });
          }, 300);
          return;
        }
        wx.showToast({ title: res.message || "发布失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
