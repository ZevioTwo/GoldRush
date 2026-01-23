package net.coding.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.coding.template.entity.po.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询
     */
    @Select("SELECT * FROM system_configs WHERE config_key = #{key}")
    SystemConfig selectByKey(@Param("key") String key);

    /**
     * 根据分组查询配置
     */
    @Select("SELECT * FROM system_configs WHERE config_group = #{group} ORDER BY config_key")
    List<SystemConfig> selectByGroup(@Param("group") String group);

    /**
     * 查询所有分组
     */
    @Select("SELECT DISTINCT config_group FROM system_configs ORDER BY config_group")
    List<String> selectAllGroups();

    /**
     * 批量查询配置
     */
    @Select("<script>" +
            "SELECT * FROM system_configs WHERE config_key IN " +
            "<foreach collection='keys' item='key' open='(' separator=',' close=')'>" +
            "   #{key}" +
            "</foreach>" +
            "</script>")
    List<SystemConfig> selectByKeys(@Param("keys") List<String> keys);

    /**
     * 根据类型查询配置
     */
    @Select("SELECT * FROM system_configs WHERE config_type = #{type} ORDER BY config_group, config_key")
    List<SystemConfig> selectByType(@Param("type") String type);

    /**
     * 更新配置值
     */
    @Update("UPDATE system_configs SET config_value = #{value}, update_time = NOW() " +
            "WHERE config_key = #{key}")
    int updateValue(@Param("key") String key, @Param("value") String value);

    /**
     * 删除非系统配置
     */
    @Delete("DELETE FROM system_configs WHERE config_key = #{key} AND is_system = 0")
    int deleteNonSystemConfig(@Param("key") String key);

    /**
     * 查询配置总数
     */
    @Select("SELECT COUNT(*) FROM system_configs")
    int countConfigs();

    /**
     * 模糊查询配置
     */
    @Select("SELECT * FROM system_configs WHERE config_key LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY config_group, config_key")
    List<SystemConfig> searchConfigs(@Param("keyword") String keyword);

    /**
     * 检查配置键是否存在
     */
    @Select("SELECT COUNT(*) FROM system_configs WHERE config_key = #{key}")
    int existsByKey(@Param("key") String key);
}
