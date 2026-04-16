const { request } = require("../../utils/request");

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
  onLoad(options) {
    const id = options && options.id ? String(options.id) : "";
    if (!id) {
      this.setData({ loading: false });
      wx.showToast({ title: "缺少会话ID", icon: "none" });
      return;
    }
    this.setData({ id }, () => this.fetchDetail());
  },
  onPullDownRefresh() {
    this.fetchDetail();
  },
  fetchDetail() {
    this.setData({ loading: true });
    request({
      url: `/api/message/sessions/${this.data.id}`,
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const session = this.normalizeSession(res.data || {});
          this.setData({
            session,
            scrollIntoView: session.items.length ? `msg-${session.items[session.items.length - 1].id}` : ""
          });
          wx.setNavigationBarTitle({ title: session.peerName || "聊天详情" });
          return;
        }
        wx.showToast({ title: res.message || "获取消息失败", icon: "none" });
        this.setData({ session: null });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
        this.setData({ session: null });
      })
      .finally(() => {
        this.setData({ loading: false });
        wx.stopPullDownRefresh();
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
          this.fetchDetail();
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
