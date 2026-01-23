package net.coding.template.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置字符串值
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis设置失败: {}", key, e);
        }
    }

    /**
     * 设置对象值
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis设置对象失败: {}", key, e);
        }
    }

    /**
     * 获取字符串值
     */
    public String get(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis获取失败: {}", key, e);
            return null;
        }
    }

    /**
     * 获取对象值
     */
    public Object getObject(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis获取对象失败: {}", key, e);
            return null;
        }
    }

    /**
     * 删除键
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis删除失败: {}", key, e);
            return false;
        }
    }

    /**
     * 设置哈希值
     */
    public void hset(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
        } catch (Exception e) {
            log.error("Redis设置哈希失败: {}.{}", key, field, e);
        }
    }

    /**
     * 获取哈希值
     */
    public Object hget(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("Redis获取哈希失败: {}.{}", key, field, e);
            return null;
        }
    }

    /**
     * 获取所有哈希值
     */
    public Map<Object, Object> hgetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("Redis获取所有哈希失败: {}", key, e);
            return null;
        }
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.error("Redis设置过期时间失败: {}", key, e);
            return false;
        }
    }

    /**
     * 判断键是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis判断存在失败: {}", key, e);
            return false;
        }
    }

    /**
     * 自增
     */
    public Long increment(String key, long delta) {
        try {
            return stringRedisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis自增失败: {}", key, e);
            return null;
        }
    }
}
