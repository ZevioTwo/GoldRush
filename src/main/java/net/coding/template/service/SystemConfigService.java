package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.enums.ConfigGroup;
import net.coding.template.entity.enums.ConfigType;
import net.coding.template.entity.po.SystemConfig;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.SystemConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private RedisService redisService;

    // 配置缓存
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
    private final Map<String, Long> configVersion = new ConcurrentHashMap<>();

    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "sys:config:";
    private static final String CACHE_VERSION_KEY = "sys:config:version";
    private static final long CACHE_EXPIRE_SECONDS = 3600; // 1小时

    /**
     * 初始化加载配置
     */
    @PostConstruct
    public void init() {
        loadAllConfigs();
        log.info("系统配置初始化完成，加载{}条配置", configCache.size());
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof String) {
            return (String) value;
        } else if (value != null) {
            return value.toString();
        }

        // 从数据库查询
        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            cacheConfig(config);
            return config.getConfigValue();
        }

        return defaultValue;
    }

    /**
     * 获取整数配置
     */
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    public Integer getInt(String key, Integer defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("配置值不是有效的整数: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                Integer intValue = Integer.parseInt(config.getConfigValue());
                cacheConfig(key, intValue, config.getConfigType());
                return intValue;
            } catch (NumberFormatException e) {
                log.error("数据库配置值不是有效的整数: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取长整数配置
     */
    public Long getLong(String key, Long defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("配置值不是有效的长整数: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                Long longValue = Long.parseLong(config.getConfigValue());
                cacheConfig(key, longValue, config.getConfigType());
                return longValue;
            } catch (NumberFormatException e) {
                log.error("数据库配置值不是有效的长整数: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取小数配置
     */
    public BigDecimal getDecimal(String key) {
        return getDecimal(key, null);
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (Exception e) {
                log.warn("配置值不是有效的小数: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                BigDecimal decimalValue = new BigDecimal(config.getConfigValue());
                cacheConfig(key, decimalValue, config.getConfigType());
                return decimalValue;
            } catch (Exception e) {
                log.error("数据库配置值不是有效的小数: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取布尔配置
     */
    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            String strValue = ((String) value).toLowerCase();
            return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue) || "on".equals(strValue);
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            String strValue = config.getConfigValue().toLowerCase();
            Boolean boolValue = "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue) || "on".equals(strValue);
            cacheConfig(key, boolValue, config.getConfigType());
            return boolValue;
        }

        return defaultValue;
    }

    /**
     * 获取JSON配置
     */
    public JSONObject getJsonObject(String key) {
        return getJsonObject(key, null);
    }

    public JSONObject getJsonObject(String key, JSONObject defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        } else if (value instanceof String) {
            try {
                JSONObject jsonValue = JSON.parseObject((String) value);
                cacheConfig(key, jsonValue, ConfigType.JSON.getCode());
                return jsonValue;
            } catch (Exception e) {
                log.warn("配置值不是有效的JSON对象: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                JSONObject jsonValue = JSON.parseObject(config.getConfigValue());
                cacheConfig(key, jsonValue, config.getConfigType());
                return jsonValue;
            } catch (Exception e) {
                log.error("数据库配置值不是有效的JSON对象: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取JSON数组配置
     */
    public JSONArray getJsonArray(String key) {
        return getJsonArray(key, null);
    }

    public JSONArray getJsonArray(String key, JSONArray defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        } else if (value instanceof String) {
            try {
                JSONArray jsonValue = JSON.parseArray((String) value);
                cacheConfig(key, jsonValue, ConfigType.ARRAY.getCode());
                return jsonValue;
            } catch (Exception e) {
                log.warn("配置值不是有效的JSON数组: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                JSONArray jsonValue = JSON.parseArray(config.getConfigValue());
                cacheConfig(key, jsonValue, config.getConfigType());
                return jsonValue;
            } catch (Exception e) {
                log.error("数据库配置值不是有效的JSON数组: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取泛型对象配置
     */
    public <T> T getObject(String key, Class<T> clazz) {
        return getObject(key, clazz, null);
    }

    public <T> T getObject(String key, Class<T> clazz, T defaultValue) {
        Object value = getFromCache(key);
        if (value != null && clazz.isInstance(value)) {
            return clazz.cast(value);
        } else if (value instanceof String) {
            try {
                T jsonValue = JSON.parseObject((String) value, clazz);
                cacheConfig(key, jsonValue, ConfigType.JSON.getCode());
                return jsonValue;
            } catch (Exception e) {
                log.warn("配置值不是有效的{}对象: {} = {}", clazz.getSimpleName(), key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                T jsonValue = JSON.parseObject(config.getConfigValue(), clazz);
                cacheConfig(key, jsonValue, config.getConfigType());
                return jsonValue;
            } catch (Exception e) {
                log.error("数据库配置值不是有效的{}对象: {} = {}", clazz.getSimpleName(), key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取列表配置
     */
    public <T> List<T> getList(String key, Class<T> clazz) {
        return getList(key, clazz, null);
    }

    public <T> List<T> getList(String key, Class<T> clazz, List<T> defaultValue) {
        Object value = getFromCache(key);
        if (value instanceof List) {
            return (List<T>) value;
        } else if (value instanceof String) {
            try {
                List<T> listValue = JSON.parseArray((String) value, clazz);
                cacheConfig(key, listValue, ConfigType.ARRAY.getCode());
                return listValue;
            } catch (Exception e) {
                log.warn("配置值不是有效的列表: {} = {}", key, value);
            }
        }

        SystemConfig config = getConfigFromDb(key);
        if (config != null && config.getConfigValue() != null) {
            try {
                List<T> listValue = JSON.parseArray(config.getConfigValue(), clazz);
                cacheConfig(key, listValue, config.getConfigType());
                return listValue;
            } catch (Exception e) {
                log.error("数据库配置值不是有效的列表: {} = {}", key, config.getConfigValue());
            }
        }

        return defaultValue;
    }

    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfigs() {
        return new HashMap<>(configCache);
    }

    /**
     * 获取分组配置
     */
    public Map<String, Object> getConfigsByGroup(String group) {
        List<SystemConfig> configs = getConfigListByGroup(group);
        return configs.stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        config -> parseConfigValue(config.getConfigType(), config.getConfigValue()),
                        (v1, v2) -> v1
                ));
    }

    /**
     * 获取分组配置列表
     */
    public List<SystemConfig> getConfigListByGroup(String group) {
        return systemConfigMapper.selectByGroup(group);
    }

    /**
     * 获取所有分组
     */
    public List<String> getAllGroups() {
        return systemConfigMapper.selectAllGroups();
    }

    /**
     * 添加或更新配置
     */
    @Transactional
    public void saveConfig(SystemConfig config) {
        if (config == null || !StringUtils.hasText(config.getConfigKey())) {
            throw new BusinessException(400, "配置键不能为空");
        }

        if (config.getConfigType() == null) {
            config.setConfigType(ConfigType.STRING.getCode());
        }

        if (config.getConfigGroup() == null) {
            config.setConfigGroup(ConfigGroup.COMMON.getCode());
        }

        // 检查是否已存在
        SystemConfig existing = systemConfigMapper.selectByKey(config.getConfigKey());

        if (existing != null) {
            // 更新
            if (Boolean.TRUE.equals(existing.getIsSystem())) {
                throw new BusinessException(400, "系统内置配置不允许修改");
            }

            existing.setConfigValue(config.getConfigValue());
            existing.setConfigType(config.getConfigType());
            existing.setConfigGroup(config.getConfigGroup());
            existing.setDescription(config.getDescription());
            existing.setExtData(config.getExtData());

            systemConfigMapper.updateById(existing);
            cacheConfig(existing);
        } else {
            // 新增
            if (config.getIsSystem() == null) {
                config.setIsSystem(false);
            }
            systemConfigMapper.insert(config);
            cacheConfig(config);
        }

        // 更新缓存版本
        updateCacheVersion();

        log.info("配置保存成功: {} = {}", config.getConfigKey(), config.getConfigValue());
    }

    /**
     * 批量保存配置
     */
    @Transactional
    public void batchSaveConfigs(List<SystemConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }

        for (SystemConfig config : configs) {
            saveConfig(config);
        }

        log.info("批量保存配置完成，共{}条", configs.size());
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }

        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null) {
            return;
        }

        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new BusinessException(400, "系统内置配置不允许删除");
        }

        int deleted = systemConfigMapper.deleteNonSystemConfig(key);
        if (deleted > 0) {
            // 从缓存中移除
            removeFromCache(key);
            updateCacheVersion();
            log.info("配置删除成功: {}", key);
        }
    }

    /**
     * 搜索配置
     */
    public List<SystemConfig> searchConfigs(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        return systemConfigMapper.searchConfigs(keyword);
    }

    /**
     * 检查配置是否存在
     */
    public boolean exists(String key) {
        return systemConfigMapper.existsByKey(key) > 0;
    }

    /**
     * 重新加载所有配置
     */
    public void reloadConfigs() {
        configCache.clear();
        configVersion.clear();
        loadAllConfigs();
        updateCacheVersion();
        log.info("系统配置重新加载完成，当前配置数量: {}", configCache.size());
    }

    /**
     * 获取配置统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", systemConfigMapper.countConfigs());
        stats.put("cached", configCache.size());
        stats.put("groups", getAllGroups());
        stats.put("cacheVersion", getCacheVersion());
        return stats;
    }

    /**
     * 初始化默认配置
     */
    @Transactional
    public void initDefaultConfigs() {
        List<SystemConfig> defaultConfigs = createDefaultConfigs();

        for (SystemConfig config : defaultConfigs) {
            if (!exists(config.getConfigKey())) {
                config.setIsSystem(true);
                systemConfigMapper.insert(config);
                log.info("初始化默认配置: {}", config.getConfigKey());
            }
        }

        reloadConfigs();
        log.info("默认配置初始化完成");
    }

    // ============ 私有方法 ============

    private void loadAllConfigs() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        for (SystemConfig config : configs) {
            cacheConfig(config);
        }
    }

    private Object getFromCache(String key) {
        // 先检查本地缓存
        Object value = configCache.get(key);
        if (value != null) {
            return value;
        }

        // 检查Redis缓存
        String cacheKey = CACHE_KEY_PREFIX + key;
        Object cachedValue = redisService.getObject(cacheKey);
        if (cachedValue != null) {
            configCache.put(key, cachedValue);
            return cachedValue;
        }

        return null;
    }

    private void cacheConfig(SystemConfig config) {
        String key = config.getConfigKey();
        String type = config.getConfigType();
        String value = config.getConfigValue();

        Object parsedValue = parseConfigValue(type, value);
        configCache.put(key, parsedValue);

        // 同时缓存到Redis
        String cacheKey = CACHE_KEY_PREFIX + key;
        redisService.set(cacheKey, parsedValue, CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void cacheConfig(String key, Object value, String type) {
        configCache.put(key, value);

        // 同时缓存到Redis
        String cacheKey = CACHE_KEY_PREFIX + key;
        redisService.set(cacheKey, value, CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void removeFromCache(String key) {
        configCache.remove(key);

        // 从Redis删除
        String cacheKey = CACHE_KEY_PREFIX + key;
        redisService.delete(cacheKey);
    }

    private Object parseConfigValue(String type, String value) {
        if (value == null) {
            return null;
        }

        try {
            switch (type) {
                case "NUMBER":
                    if (value.contains(".")) {
                        return new BigDecimal(value);
                    } else {
                        try {
                            return Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            return Long.parseLong(value);
                        }
                    }
                case "BOOLEAN":
                    String lowerValue = value.toLowerCase();
                    return "true".equals(lowerValue) || "1".equals(lowerValue) ||
                            "yes".equals(lowerValue) || "on".equals(lowerValue);
                case "JSON":
                    return JSON.parse(value);
                case "ARRAY":
                    return JSON.parseArray(value);
                default: // STRING, OBJECT
                    return value;
            }
        } catch (Exception e) {
            log.error("解析配置值失败: type={}, value={}", type, value, e);
            return value;
        }
    }

    private SystemConfig getConfigFromDb(String key) {
        return systemConfigMapper.selectByKey(key);
    }

    private void updateCacheVersion() {
        long version = System.currentTimeMillis();
        configVersion.put("global", version);
        redisService.set(CACHE_VERSION_KEY, version, CACHE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    private Long getCacheVersion() {
        Long version = configVersion.get("global");
        if (version == null) {
            Object cached = redisService.getObject(CACHE_VERSION_KEY);
            version = cached != null ? Long.parseLong(cached.toString()) : 0L;
            configVersion.put("global", version);
        }
        return version;
    }

    private List<SystemConfig> createDefaultConfigs() {
        List<SystemConfig> configs = new ArrayList<>();

        // 通用配置
        configs.add(createConfig("app.name", "摸金小契约", "STRING", "COMMON", "应用名称"));
        configs.add(createConfig("app.version", "1.0.0", "STRING", "COMMON", "应用版本"));
        configs.add(createConfig("app.description", "游戏组队履约保障工具", "STRING", "COMMON", "应用描述"));
        configs.add(createConfig("app.copyright", "© 2024 摸金科技", "STRING", "COMMON", "版权信息"));

        // 支付配置
        configs.add(createConfig("payment.service_fee_rate", "0.01", "NUMBER", "PAYMENT", "平台服务费率"));
        configs.add(createConfig("payment.default_deposit", "20", "NUMBER", "PAYMENT", "默认押金金额"));
        configs.add(createConfig("payment.max_deposit", "200", "NUMBER", "PAYMENT", "最大押金金额"));
        configs.add(createConfig("payment.min_deposit", "10", "NUMBER", "PAYMENT", "最小押金金额"));
        configs.add(createConfig("payment.penalty_fee_rate", "0.01", "NUMBER", "PAYMENT", "违约金平台手续费率"));
        configs.add(createConfig("payment.order_expire_minutes", "30", "NUMBER", "PAYMENT", "订单过期时间(分钟)"));
        configs.add(createConfig("payment.auto_refund_hours", "168", "NUMBER", "PAYMENT", "自动退款时间(小时)"));

        // 信用配置
        configs.add(createConfig("credit.init_score", "100", "NUMBER", "CREDIT", "初始信用分"));
        configs.add(createConfig("credit.max_score", "100", "NUMBER", "CREDIT", "最高信用分"));
        configs.add(createConfig("credit.min_score", "0", "NUMBER", "CREDIT", "最低信用分"));
        configs.add(createConfig("credit.violation_deduct", "50", "NUMBER", "CREDIT", "违约扣分"));
        configs.add(createConfig("credit.complete_add", "10", "NUMBER", "CREDIT", "完成契约加分"));
        configs.add(createConfig("credit.dispute_win_add", "5", "NUMBER", "CREDIT", "争议胜诉加分"));
        configs.add(createConfig("credit.dispute_lose_deduct", "20", "NUMBER", "CREDIT", "争议败诉扣分"));
        configs.add(createConfig("credit.blacklist_threshold", "30", "NUMBER", "CREDIT", "黑名单阈值"));
        configs.add(createConfig("credit.recover_days", "7", "NUMBER", "CREDIT", "信用分恢复周期(天)"));
        configs.add(createConfig("credit.recover_points", "5", "NUMBER", "CREDIT", "每次恢复点数"));

        // 契约配置
        configs.add(createConfig("contract.default_game_type", "DELTA", "STRING", "CONTRACT", "默认游戏类型"));
        configs.add(createConfig("contract.dispute_timeout_hours", "24", "NUMBER", "CONTRACT", "争议举证超时时间(小时)"));
        configs.add(createConfig("contract.auto_complete_hours", "72", "NUMBER", "CONTRACT", "自动完成时间(小时)"));
        configs.add(createConfig("contract.auto_cancel_minutes", "30", "NUMBER", "CONTRACT", "自动取消时间(分钟)"));
        configs.add(createConfig("contract.max_active_contracts", "3", "NUMBER", "CONTRACT", "最大同时进行契约数"));
        configs.add(createConfig("contract.min_guarantee_length", "5", "NUMBER", "CONTRACT", "保底物品最小长度"));
        configs.add(createConfig("contract.max_guarantee_length", "100", "NUMBER", "CONTRACT", "保底物品最大长度"));

        // VIP配置
        configs.add(createConfig("vip.month_price", "9.9", "NUMBER", "VIP", "月卡价格"));
        configs.add(createConfig("vip.quarter_price", "29.7", "NUMBER", "VIP", "季卡价格"));
        configs.add(createConfig("vip.year_price", "99.0", "NUMBER", "VIP", "年卡价格"));
        configs.add(createConfig("vip.trial_days", "3", "NUMBER", "VIP", "试用天数"));
        configs.add(createConfig("vip.max_contract_limit", "-1", "NUMBER", "VIP", "最大契约限制(-1为无限)"));
        configs.add(createConfig("vip.free_service_fee", "true", "BOOLEAN", "VIP", "免服务费"));
        configs.add(createConfig("vip.special_card_style", "true", "BOOLEAN", "VIP", "专属卡片样式"));
        configs.add(createConfig("vip.priority_support", "true", "BOOLEAN", "VIP", "优先客服支持"));

        // 安全配置
        configs.add(createConfig("security.token_expire_hours", "168", "NUMBER", "SECURITY", "Token过期时间(小时)"));
        configs.add(createConfig("security.max_login_attempts", "5", "NUMBER", "SECURITY", "最大登录尝试次数"));
        configs.add(createConfig("security.login_lock_minutes", "30", "NUMBER", "SECURITY", "登录锁定时间(分钟)"));
        configs.add(createConfig("security.password_min_length", "6", "NUMBER", "SECURITY", "密码最小长度"));
        configs.add(createConfig("security.enable_captcha", "true", "BOOLEAN", "SECURITY", "启用验证码"));
        configs.add(createConfig("security.require_realname", "false", "BOOLEAN", "SECURITY", "要求实名认证"));

        // 运营配置
        configs.add(createConfig("operation.customer_service_qq", "123456789", "STRING", "OPERATION", "客服QQ"));
        configs.add(createConfig("operation.customer_service_phone", "400-123-4567", "STRING", "OPERATION", "客服电话"));
        configs.add(createConfig("operation.official_group_url", "https://qq.com/group", "STRING", "OPERATION", "官方QQ群链接"));
        configs.add(createConfig("operation.mini_program_name", "摸金小契约", "STRING", "OPERATION", "小程序名称"));
        configs.add(createConfig("operation.company_name", "摸金科技有限公司", "STRING", "OPERATION", "公司名称"));
        configs.add(createConfig("operation.icp_number", "京ICP备12345678号", "STRING", "OPERATION", "ICP备案号"));
        configs.add(createConfig("operation.privacy_policy_url", "https://goldrush.com/privacy", "STRING", "OPERATION", "隐私政策链接"));
        configs.add(createConfig("operation.user_agreement_url", "https://goldrush.com/agreement", "STRING", "OPERATION", "用户协议链接"));

        // 通知配置
        configs.add(createConfig("notification.enable_sms", "false", "BOOLEAN", "NOTIFICATION", "启用短信通知"));
        configs.add(createConfig("notification.enable_email", "false", "BOOLEAN", "NOTIFICATION", "启用邮件通知"));
        configs.add(createConfig("notification.enable_wechat", "true", "BOOLEAN", "NOTIFICATION", "启用微信通知"));
        configs.add(createConfig("notification.template_contract_created", "您有一个新的契约待处理", "STRING", "NOTIFICATION", "契约创建通知模板"));
        configs.add(createConfig("notification.template_contract_completed", "您的契约已完成", "STRING", "NOTIFICATION", "契约完成通知模板"));
        configs.add(createConfig("notification.template_payment_success", "支付成功通知", "STRING", "NOTIFICATION", "支付成功通知模板"));
        configs.add(createConfig("notification.template_dispute_created", "您有一个新的争议待处理", "STRING", "NOTIFICATION", "争议创建通知模板"));

        // 第三方配置
        configs.add(createConfig("third_party.wechat_app_id", "wx1234567890abcdef", "STRING", "THIRD_PARTY", "微信AppId"));
        configs.add(createConfig("third_party.wechat_app_secret", "your_app_secret", "STRING", "THIRD_PARTY", "微信AppSecret"));
        configs.add(createConfig("third_party.wechat_mch_id", "1234567890", "STRING", "THIRD_PARTY", "微信商户号"));
        configs.add(createConfig("third_party.qiniu_access_key", "your_qiniu_access_key", "STRING", "THIRD_PARTY", "七牛AccessKey"));
        configs.add(createConfig("third_party.qiniu_secret_key", "your_qiniu_secret_key", "STRING", "THIRD_PARTY", "七牛SecretKey"));
        configs.add(createConfig("third_party.qiniu_bucket", "goldrush", "STRING", "THIRD_PARTY", "七牛存储桶"));
        configs.add(createConfig("third_party.qiniu_domain", "https://cdn.goldrush.com", "STRING", "THIRD_PARTY", "七牛域名"));
        configs.add(createConfig("third_party.aliyun_sms_access_key", "your_aliyun_access_key", "STRING", "THIRD_PARTY", "阿里云短信AccessKey"));
        configs.add(createConfig("third_party.aliyun_sms_secret_key", "your_aliyun_secret_key", "STRING", "THIRD_PARTY", "阿里云短信SecretKey"));

        return configs;
    }

    private SystemConfig createConfig(String key, String value, String type, String group, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(type);
        config.setConfigGroup(group);
        config.setDescription(description);
        config.setIsSystem(true);
        return config;
    }
}