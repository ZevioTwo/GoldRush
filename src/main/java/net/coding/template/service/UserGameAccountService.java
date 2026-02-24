package net.coding.template.service;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.po.UserGameAccount;
import net.coding.template.entity.request.UserGameAccountCreateRequest;
import net.coding.template.entity.request.UserGameAccountUpdateRequest;
import net.coding.template.entity.response.UserGameAccountResponse;
import net.coding.template.mapper.UserGameAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserGameAccountService {

    @Resource
    private UserGameAccountMapper accountMapper;

    public List<UserGameAccountResponse> listByUser(Long userId) {
        List<UserGameAccount> accounts = accountMapper.selectByUserId(userId);
        return accounts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserGameAccountResponse create(Long userId, UserGameAccountCreateRequest request) {
        UserGameAccount account = new UserGameAccount();
        account.setUserId(userId);
        account.setGameType(request.getGameType());
        account.setGameRegion(request.getGameRegion());
        account.setGameId(request.getGameId());
        account.setRemark(request.getRemark());

        accountMapper.insert(account);
        return toResponse(account);
    }

    @Transactional
    public UserGameAccountResponse update(Long userId, Long id, UserGameAccountUpdateRequest request) {
        UserGameAccount account = accountMapper.selectByIdAndUser(id, userId);
        if (account == null) {
            throw new RuntimeException("账号不存在或无权限");
        }
        account.setGameType(request.getGameType());
        account.setGameRegion(request.getGameRegion());
        account.setGameId(request.getGameId());
        account.setRemark(request.getRemark());
        accountMapper.updateById(account);
        return toResponse(account);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        int rows = accountMapper.deleteByIdAndUser(id, userId);
        if (rows == 0) {
            throw new RuntimeException("账号不存在或无权限");
        }
    }

    private UserGameAccountResponse toResponse(UserGameAccount account) {
        UserGameAccountResponse resp = new UserGameAccountResponse();
        resp.setId(account.getId());
        resp.setGameType(account.getGameType());
        resp.setGameRegion(account.getGameRegion());
        resp.setGameId(account.getGameId());
        resp.setRemark(account.getRemark());
        resp.setCreateTime(account.getCreateTime());
        resp.setUpdateTime(account.getUpdateTime());
        return resp;
    }
}
