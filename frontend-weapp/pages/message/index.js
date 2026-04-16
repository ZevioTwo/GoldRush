const { request } = require("../../utils/request");

Page({
  data: {
    sessions: [],
    filteredSessions: [],
    keyword: "",
    loading: false,
    defaultAvatar: "/images/user_avatar.png"
  },
  onShow() {
    const tabbar = this.getTabBar && this.getTabBar();
    if (tabbar && tabbar.setSelected) {
      tabbar.setSelected("/pages/message/index");
    }
    this.fetchSessions();
  },
  onPullDownRefresh() {
    this.fetchSessions();
  },
  fetchSessions() {
    this.setData({ loading: true });
    request({
      url: "/api/message/sessions",
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const sessions = (res.data?.sessions || []).map((item) => this.normalizeSession(item));
          this.setData({ sessions }, () => this.applyFilter());
          return;
        }
        wx.showToast({ title: res.message || "获取消息失败", icon: "none" });
        this.setData({ sessions: [], filteredSessions: [] });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ sessions: [], filteredSessions: [] });
      })
      .finally(() => {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
      });
  },
  normalizeSession(item) {
    const unreadCount = Number(item.unreadCount) || 0;
    const sessionType = item.sessionType || "PRIVATE";
    return {
      ...item,
      sessionId: item.sessionId,
      sessionType,
      peerName: item.peerName || (sessionType === "SYSTEM" ? "系统通知" : "未命名会话"),
      peerAvatar: item.peerAvatar || (sessionType === "SYSTEM" ? "/images/message_active.png" : this.data.defaultAvatar),
      peerTag: item.peerTag || (sessionType === "SYSTEM" ? "系统" : "契约私聊"),
      lastMessage: item.lastMessage || (sessionType === "SYSTEM" ? "暂无系统通知" : "点击进入后开始聊天"),
      lastTimeDisplay: this.formatDate(item.lastTime),
      unreadCount,
      unreadDisplay: unreadCount > 99 ? "99+" : `${unreadCount}`,
      unread: unreadCount > 0,
      highlight: !!item.highlight || unreadCount > 0,
      canReply: !!item.canReply,
      typeLabel: sessionType === "SYSTEM" ? "系统消息" : "契约私聊"
    };
  },
  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value || "" }, () => this.applyFilter());
  },
  applyFilter() {
    const keyword = (this.data.keyword || "").trim().toLowerCase();
    if (!keyword) {
      this.setData({ filteredSessions: this.data.sessions });
      return;
    }

    const filteredSessions = this.data.sessions.filter((item) => {
      const haystack = [
        item.peerName,
        item.peerTag,
        item.lastMessage,
        item.typeLabel
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(keyword);
    });
    this.setData({ filteredSessions });
  },
  openSession(e) {
    const sessionId = Number(e.currentTarget.dataset.id);
    if (!sessionId) return;
    wx.navigateTo({ url: `/pages/message/detail?id=${sessionId}` });
  },
  formatDate(value) {
    if (!value) return "";
    const pad = (n) => (n < 10 ? `0${n}` : `${n}`);
    if (Array.isArray(value)) {
      const [y, m, d, hh = 0, mm = 0] = value;
      return `${pad(m)}-${pad(d)} ${pad(hh)}:${pad(mm)}`;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  },
  onAvatarError(e) {
    const index = Number(e.currentTarget.dataset.index);
    if (Number.isNaN(index)) return;
    this.setData({ [`filteredSessions[${index}].peerAvatar`]: this.data.defaultAvatar });
  }
});
