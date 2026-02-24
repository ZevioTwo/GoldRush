const { request } = require("../../utils/request");

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    statusIndex: 0,
    statusOptions: ["全部", "进行中", "已完成", "已取消"],
    statusValues: [null, "ACTIVE", "COMPLETED", "CANCELLED"]
  },
  onShow() {
    this.fetchList();
  },
  onStatusChange(e) {
    const index = Number(e.detail.value) || 0;
    this.setData({ statusIndex: index, page: 1 }, () => this.fetchList());
  },
  prevPage() {
    if (this.data.page <= 1) return;
    this.setData({ page: this.data.page - 1 }, () => this.fetchList());
  },
  nextPage() {
    this.setData({ page: this.data.page + 1 }, () => this.fetchList());
  },
  fetchList() {
    const status = this.data.statusValues[this.data.statusIndex];
    const data = {
      page: this.data.page,
      size: this.data.size
    };
    if (status) {
      data.status = status;
    }

    request({
      url: "/api/contract/list",
      method: "GET",
      data
    })
      .then((res) => {
        if (res && res.code === 0) {
          this.setData({ list: res.data?.contracts || [] });
          return;
        }
        wx.showToast({ title: res.message || "获取失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  goDetail(e) {
    const contractId = e.currentTarget.dataset.id;
    if (!contractId) return;
    wx.navigateTo({ url: `/pages/contracts/detail?id=${contractId}` });
  }
});
