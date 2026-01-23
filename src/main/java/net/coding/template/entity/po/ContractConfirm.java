package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("contract_confirms")
public class ContractConfirm {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private String contractId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_role")
    private String userRole;

    @TableField("confirm_status")
    private String confirmStatus = "PENDING";

    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    @TableField("confirm_ip")
    private String confirmIp;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
