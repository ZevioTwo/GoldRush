package net.coding.template.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_configs")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;           // 配置键

    @TableField("config_value")
    private String configValue;         // 配置值

    @TableField("config_type")
    private String configType;          // 配置类型：STRING, NUMBER, BOOLEAN, JSON

    @TableField("config_group")
    private String configGroup;         // 配置分组：COMMON, PAYMENT, CREDIT, CONTRACT, VIP

    @TableField("description")
    private String description;         // 配置描述

    @TableField("is_system")
    private Boolean isSystem = false;   // 是否系统内置（不可删除）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 扩展字段，存储额外信息
    @TableField("ext_data")
    private String extData;
}
