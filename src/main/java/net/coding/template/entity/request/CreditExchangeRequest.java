package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreditExchangeRequest {

    @NotNull(message = "兑换摸金币数量不能为空")
    @DecimalMin(value = "100", message = "最少需要100摸金币起兑")
    private BigDecimal mojinAmount;
}
