App({
  globalData: {
    baseUrl: "http://81.71.88.97:8080",
    token: ""
  },
  onLaunch() {
    const token = wx.getStorageSync("token");
    if (token) {
      this.globalData.token = token;
    }
  }
});
