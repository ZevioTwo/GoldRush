package net.coding.template.entity.request;

import lombok.Data;
import javax.validation.constraints.*;

import java.math.BigDecimal;

@Data
public class ContractCreateRequest {
    @NotBlank(message = "接收方游戏ID不能为空")
    private String receiverGameId;

    @NotBlank(message = "游戏大区不能为空")
    private String gameRegion;

    @NotBlank(message = "游戏类型不能为空")
    private String gameType; // DELTA, AREA18, TARKOV

    @NotNull(message = "押金金额不能为空")
    @DecimalMin(value = "10.00", message = "押金金额不能低于10元")
    @DecimalMax(value = "200.00", message = "押金金额不能超过200元")
    private BigDecimal depositAmount;

    @NotBlank(message = "保底物品不能为空")
    @Size(max = 100, message = "保底物品不能超过100字")
    private String guaranteeItem;

    @Size(max = 500, message = "成功条件不能超过500字")
    private String successCondition;

    @Size(max = 500, message = "失败条件不能超过500字")
    private String failureCondition;

    private String receiverWechatId; // 微信号（可选）
}
