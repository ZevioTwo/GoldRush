const { request } = require("../../utils/request");

const POLL_INTERVAL = 3000;

Page({
  data: {
    id: "",
    session: null,
    inputValue: "",
    loading: true,
    sending: false,
    scrollIntoView: "",
    defaultAvatar: "/images/user_avatar.png"
  },
  pollTimer: null,
  isPolling: false,
  onLoad(options) {
    const id = options && options.id ? String(options.id) : "";
    if (!id) {
      this.setData({ loading: false });
      wx.showToast({ title: "缺少会话ID", icon: "none" });
      return;
    }
    this.setData({ id }, () => {
      this.fetchDetail({ forceScroll: true });
      this.startPolling();
    });
  },
  onShow() {
    if (this.data.id) {
      this.fetchDetail({ silent: true });
      this.startPolling();
    }
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
  },
  onPullDownRefresh() {
    this.fetchDetail({ forceScroll: true });
  },
  startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      if (this.isPolling || !this.data.id) return;
      this.fetchDetail({ silent: true });
    }, POLL_INTERVAL);
  },
  stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  },
  fetchDetail(options = {}) {
    const { silent = false, forceScroll = false } = options;
    const previousItems = this.data.session && Array.isArray(this.data.session.items)
      ? this.data.session.items
      : [];
    const previousLastId = previousItems.length ? previousItems[previousItems.length - 1].id : null;

    if (!silent) {
      this.setData({ loading: true });
    }
    this.isPolling = silent;
    request({
      url: `/api/message/sessions/${this.data.id}`,
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const session = this.normalizeSession(res.data || {});
          const nextItems = Array.isArray(session.items) ? session.items : [];
          const nextLastId = nextItems.length ? nextItems[nextItems.length - 1].id : null;
          const shouldScroll = forceScroll || (!!nextLastId && nextLastId !== previousLastId);
          this.setData({
            session,
            scrollIntoView: shouldScroll && nextLastId ? `msg-${nextLastId}` : this.data.scrollIntoView
          });
          wx.setNavigationBarTitle({ title: session.peerName || "聊天详情" });
          return;
        }
        if (!silent) {
          wx.showToast({ title: res.message || "获取消息失败", icon: "none" });
          this.setData({ session: null });
        }
      })
      .catch(() => {
        if (!silent) {
          wx.showToast({ title: "网络错误", icon: "none" });
          this.setData({ session: null });
        }
      })
      .finally(() => {
        this.isPolling = false;
        if (!silent) {
          this.setData({ loading: false });
          wx.stopPullDownRefresh();
        }
      });
  },
  normalizeSession(session) {
    const items = (session.items || []).map((item) => ({
      ...item,
      timeDisplay: this.formatDate(item.createTime),
      self: !!item.self,
      isSystem: item.msgType === "SYSTEM"
    }));

    return {
      ...session,
      peerName: session.peerName || "消息详情",
      peerAvatar: session.peerAvatar || this.data.defaultAvatar,
      peerTag: session.peerTag || (session.sessionType === "SYSTEM" ? "系统" : "契约私聊"),
      canReply: !!session.canReply,
      items
    };
  },
  onInput(e) {
    this.setData({ inputValue: e.detail.value || "" });
  },
  sendMessage() {
    const content = (this.data.inputValue || "").trim();
    if (!content || this.data.sending || !this.data.session || !this.data.session.canReply) return;

    this.setData({ sending: true });
    request({
      url: `/api/message/sessions/${this.data.id}/send`,
      method: "POST",
      data: { content }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ inputValue: "" });
          this.fetchDetail({ silent: true, forceScroll: true });
          return;
        }
        wx.showToast({ title: res.message || "发送失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ sending: false });
      });
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
  onAvatarError() {
    this.setData({ "session.peerAvatar": this.data.defaultAvatar });
  }
});
