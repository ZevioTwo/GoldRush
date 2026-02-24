const app = getApp();

const setToken = (token) => {
  app.globalData.token = token || "";
  if (token) {
    wx.setStorageSync("token", token);
  } else {
    wx.removeStorageSync("token");
  }
};

const getToken = () => {
  return wx.getStorageSync("token") || app.globalData.token || "";
};

module.exports = {
  setToken,
  getToken
};
