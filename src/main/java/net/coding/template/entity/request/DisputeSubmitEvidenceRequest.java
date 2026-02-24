package net.coding.template.entity.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class DisputeSubmitEvidenceRequest {
    @NotBlank(message = "仲裁编号不能为空")
    private String disputeNo;

    @Size(max = 2000, message = "补充说明不能超过2000字")
    private String description;

    private List<String> evidenceUrls;

    private List<String> gameScreenshotUrls;

    private List<String> videoLinks;
}
