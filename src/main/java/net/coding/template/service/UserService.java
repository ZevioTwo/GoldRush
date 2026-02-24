package net.coding.template.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.po.User;
import net.coding.template.entity.dto.CreditScoreDTO;
import net.coding.template.entity.request.LoginRequest;
import net.coding.template.entity.dto.UserProfileDTO;
import net.coding.template.mapper.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private WeChatService weChatService;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户登录/注册
     */
    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        // 1. 获取微信openid
        Map<String, String> wechatResult = weChatService.getOpenidByCode(request.getCode());
        String openid = wechatResult.get("openid");

        // 2. 检查用户是否存在
        User user = userMapper.selectByOpenid(openid);

        // 3. 新用户注册
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUnionid(wechatResult.get("unionid"));
            user.setNickname("微信用户" + System.currentTimeMillis() % 10000);
            user.setAvatarUrl("https://default-avatar.com/avatar.png");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());

            // 尝试解密用户信息（如果有）
            if (request.getEncryptedData() != null && request.getIv() != null) {
                try {
                    JSONObject userInfo = weChatService.decryptUserInfo(
                            request.getEncryptedData(),
                            wechatResult.get("sessionKey"),
                            request.getIv()
                    );
                    user.setNickname(userInfo.getString("nickName"));
                    user.setAvatarUrl(userInfo.getString("avatarUrl"));
                } catch (Exception e) {
                    log.warn("用户信息解密失败，使用默认信息", e);
                }
            }

            userMapper.insert(user);
            log.info("新用户注册: {}", user.getId());
        }

        // 4. 检查VIP状态是否过期
        if (user.getIsVip() && user.getVipExpireTime() != null
                && LocalDateTime.now().isAfter(user.getVipExpireTime())) {
            user.setIsVip(false);
            userMapper.updateById(user);
        }

        // 5. 生成token
        String token = generateToken(user);

        // 6. 缓存用户信息
        cacheUserInfo(user, token);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("isNewUser", user.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(5)));

        return result;
    }

    /**
     * 获取用户信息
     */
    public UserProfileDTO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setCreditScore(user.getCreditScore());
        dto.setIsVip(user.getIsVip());
        dto.setVipExpireTime(user.getVipExpireTime());
        dto.setTotalContracts(user.getTotalContracts());
        dto.setCompletedContracts(user.getCompletedContracts());
        dto.setDisputeCount(user.getDisputeCount());
        dto.setViolationCount(user.getViolationCount());
        dto.setGameId(user.getGameId());
        dto.setGameRegion(user.getGameRegion());
        dto.setWechatId(user.getWechatId());
        dto.setPhone(user.getPhone());

        // 计算成功率
        if (user.getTotalContracts() > 0) {
            double successRate = (double) user.getCompletedContracts() / user.getTotalContracts() * 100;
            dto.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
        } else {
            dto.setSuccessRate(100.0);
        }

        // 用户等级
        dto.setUserLevel(calculateUserLevel(user));

        return dto;
    }

    /**
     * 查询信用分详情
     */
    public CreditScoreDTO getCreditScore(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        CreditScoreDTO dto = new CreditScoreDTO();
        dto.setCurrentScore(user.getCreditScore());

        // 计算信用等级
        if (user.getCreditScore() >= 90) {
            dto.setLevel("AAA");
        } else if (user.getCreditScore() >= 70) {
            dto.setLevel("AA");
        } else if (user.getCreditScore() >= 50) {
            dto.setLevel("A");
        } else if (user.getCreditScore() >= 30) {
            dto.setLevel("B");
        } else {
            dto.setLevel("C");
        }

        // 获取信用历史（这里简化处理，实际应从数据库查询）
        List<CreditScoreDTO.CreditHistory> history = new ArrayList<>();

        // 添加模拟历史记录
        if (user.getCompletedContracts() > 0) {
            CreditScoreDTO.CreditHistory history1 = new CreditScoreDTO.CreditHistory();
            history1.setType("COMPLETE");
            history1.setDescription("成功完成契约");
            history1.setChange(10);
            history1.setTime(LocalDateTime.now().minusDays(1).toString());
            history.add(history1);
        }

        if (user.getViolationCount() > 0) {
            CreditScoreDTO.CreditHistory history2 = new CreditScoreDTO.CreditHistory();
            history2.setType("VIOLATION");
            history2.setDescription("契约违约");
            history2.setChange(-50);
            history2.setTime(LocalDateTime.now().minusDays(3).toString());
            history.add(history2);
        }

        dto.setHistory(history);

        return dto;
    }

    /**
     * 内部工具方法：生成token
     */
    private String generateToken(User user) {
        String raw = user.getId() + "_" + user.getOpenid() + "_" + System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(raw.getBytes());
    }

    /**
     * 内部工具方法：缓存用户信息
     */
    private void cacheUserInfo(User user, String token) {
        Map<String, Object> userCache = new HashMap<>();
        userCache.put("userId", user.getId());
        userCache.put("openid", user.getOpenid());
        userCache.put("creditScore", user.getCreditScore());
        userCache.put("isVip", user.getIsVip());

        // 缓存7天
        String catchStr = JSON.toJSONString(userCache);
        stringRedisTemplate.opsForValue().set("user:token:" + token, catchStr, 7 * 24 * 60 * 60, TimeUnit.MINUTES);
        // 维护token-userId映射
        stringRedisTemplate.opsForValue().set("user:token_mapping:" + user.getId(), token, 7 * 24 * 60 * 60, TimeUnit.MINUTES);
    }

    /**
     * 内部工具方法：计算用户等级
     */
    private String calculateUserLevel(User user) {
        int score = user.getCreditScore();
        int contracts = user.getTotalContracts();

        if (score >= 90 && contracts >= 10) return "钻石打手";
        if (score >= 80 && contracts >= 5) return "黄金打手";
        if (score >= 70 && contracts >= 3) return "白银打手";
        if (score >= 60) return "青铜打手";
        return "新手学徒";
    }

    /**
     * 根据token获取用户ID（供其他服务调用）
     */
    public Long getUserIdByToken(String token) {
        String cache = stringRedisTemplate.opsForValue().get("user:token:" + token);
        Map<String, Object> map = JSON.parseObject(cache, Map.class);
        if (cache != null && map.containsKey("userId")) {
            return Long.parseLong(map.get("userId").toString());
        }
        return null;
    }

    /**
     * 获取当前用户（供其他服务调用）
     */
    public User getCurrentUser(String token) {
        Long userId = getUserIdByToken(token);
        if (userId == null) {
            throw new RuntimeException("用户未登录或token已过期");
        }
        return userMapper.selectById(userId);
    }

    /**
     * 更新用户游戏信息与联系方式
     */
    @Transactional
    public void updateProfile(Long userId, String gameId, String gameRegion, String wechatId, String phone) {
        int rows = userMapper.updateGameInfo(userId, gameId, gameRegion, wechatId, phone);
        if (rows == 0) {
            throw new RuntimeException("更新用户信息失败");
        }
    }
}
