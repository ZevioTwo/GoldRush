package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class DisputeApplyRequest {
    @NotBlank(message = "契约ID不能为空")
    private String contractId;

    @NotBlank(message = "争议类型不能为空")
    private String disputeType; // VIOLATION/FRAUD/OTHER

    @NotBlank(message = "争议描述不能为空")
    @Size(max = 2000, message = "争议描述不能超过2000字")
    private String description;

    private List<String> evidenceUrls; // 证据链接

    private List<String> gameScreenshotUrls; // 游戏结算截图

    private List<String> videoLinks; // 录屏链接

    @NotNull(message = "是否加急不能为空")
    private Boolean isUrgent = false;

    private Boolean urgentFeePaid = false;
}
