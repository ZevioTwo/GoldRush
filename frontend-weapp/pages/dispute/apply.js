const { request } = require("../../utils/request");

Page({
  data: {
    loading: false,
    contractId: "",
    contract: {},
    contracts: [],
    showContractPicker: true,
    contractOptions: ["请选择契约"],
    contractValues: [""],
    contractIndex: 0,
    disputeTypeOptions: ["请选择争议类型", "违约", "欺诈", "其他"],
    disputeTypeValues: ["", "VIOLATION", "FRAUD", "OTHER"],
    disputeTypeIndex: 0,
    form: {
      disputeType: "",
      description: "",
      evidenceUrls: "",
      gameScreenshotUrls: "",
      videoLinks: "",
      isUrgent: false
    }
  },
  onLoad(options) {
    if (options && options.contractId) {
      this.setData(
        { contractId: options.contractId, showContractPicker: false },
        () => this.fetchContract()
      );
      return;
    }
    this.setData({ showContractPicker: true });
    this.fetchContracts();
  },
  fetchContracts() {
    request({
      url: "/api/contract/list",
      method: "GET",
      data: { page: 1, size: 50 }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          const contracts = res.data?.contracts || [];
          const normalized = contracts
            .map((item) => ({
              ...item,
              _cid: this.normalizeContractId(item),
              _label: `${item.contractNo || item.contractId || item.id} | ${item.status || ""}`
            }))
            .filter((item) => item._cid);
          const options = ["请选择契约"].concat(normalized.map((item) => item._label));
          const values = [""].concat(normalized.map((item) => item._cid));
          this.setData({
            contracts: normalized,
            contractOptions: options,
            contractValues: values
          });
          return;
        }
        wx.showToast({ title: res.message || "获取契约列表失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  fetchContract() {
    if (!this.data.contractId) return;
    request({
      url: `/api/contract/${this.data.contractId}`,
      method: "GET"
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          this.setData({ contract: res.data || {} });
          return;
        }
        wx.showToast({ title: res.message || "获取契约失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      });
  },
  onContractChange(e) {
    const index = Number(e.detail.value) || 0;
    const contractId = this.data.contractValues[index] || "";
    const selected = this.data.contracts.find((item) => item._cid === contractId);
    this.setData(
      {
        contractIndex: index,
        contractId,
        contract: selected
          ? {
              contractNo: selected.contractNo,
              status: selected.status,
              depositAmount: selected.depositAmount
            }
          : {}
      },
      () => this.fetchContract()
    );
  },
  normalizeContractId(value) {
    if (!value) return "";
    if (typeof value === "string") return value;
    if (typeof value === "number") return String(value);
    if (typeof value === "object") {
      return value.contractId || value.id || "";
    }
    return "";
  },
  onDisputeTypeChange(e) {
    const index = Number(e.detail.value) || 0;
    const disputeType = this.data.disputeTypeValues[index] || "";
    this.setData({
      disputeTypeIndex: index,
      form: {
        ...this.data.form,
        disputeType
      }
    });
  },
  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`form.${field}`]: e.detail.value });
  },
  onUrgentChange(e) {
    this.setData({
      form: {
        ...this.data.form,
        isUrgent: e.detail.value
      }
    });
  },
  normalizeList(value) {
    if (!value) return [];
    return value
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item.length > 0);
  },
  validateForm(form) {
    if (!this.data.contractId) return "请选择契约";
    if (!form.disputeType) return "请选择争议类型";
    if (!form.description) return "请填写争议描述";
    if (form.description.length > 2000) return "争议描述不能超过2000字";
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

    this.setData({ loading: true });
    request({
      url: "/api/dispute/apply",
      method: "POST",
      data: {
        contractId: this.data.contractId,
        disputeType: form.disputeType,
        description: form.description,
        evidenceUrls: this.normalizeList(form.evidenceUrls),
        gameScreenshotUrls: this.normalizeList(form.gameScreenshotUrls),
        videoLinks: this.normalizeList(form.videoLinks),
        isUrgent: !!form.isUrgent
      }
    })
      .then((res) => {
        if (res && (res.code === 0 || res.code === 200)) {
          wx.showToast({ title: "提交成功", icon: "success" });
          wx.navigateBack();
          return;
        }
        wx.showToast({ title: res.message || "提交失败", icon: "none" });
      })
      .catch(() => {
        wx.showToast({ title: "网络错误", icon: "none" });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  }
});
