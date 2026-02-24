const app = getApp();

const request = (options) => {
  const token = wx.getStorageSync("token");
  const headers = Object.assign({}, options.header || {});

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}${options.url}`,
      method: options.method || "GET",
      data: options.data || {},
      header: headers,
      success: (res) => {
        const { data } = res;
        if (data && data.code === 401) {
          wx.removeStorageSync("token");
          app.globalData.token = "";
          wx.redirectTo({ url: "/pages/login/login" });
          reject(data);
          return;
        }
        resolve(data);
      },
      fail: (err) => reject(err)
    });
  });
};

module.exports = {
  request
};
