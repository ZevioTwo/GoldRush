App({
  globalData: {
    baseUrl: "https://api.example.com",
    token: ""
  },
  onLaunch() {
    const token = wx.getStorageSync("token");
    if (token) {
      this.globalData.token = token;
    }
  }
});
