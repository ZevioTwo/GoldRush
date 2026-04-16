package net.coding.template.service;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.po.BountyClaim;
import net.coding.template.entity.po.BountyPost;
import net.coding.template.entity.po.MojinLedger;
import net.coding.template.entity.po.User;
import net.coding.template.entity.request.BountyCreateRequest;
import net.coding.template.entity.request.BountyListRequest;
import net.coding.template.entity.response.BountyClaimResponse;
import net.coding.template.entity.response.BountyCreateResponse;
import net.coding.template.entity.response.BountyDetailResponse;
import net.coding.template.entity.response.BountyListResponse;
import net.coding.template.exception.BusinessException;
import net.coding.template.mapper.BountyClaimMapper;
import net.coding.template.mapper.BountyPostMapper;
import net.coding.template.mapper.MojinLedgerMapper;
import net.coding.template.mapper.UserMapper;
import net.coding.template.util.OrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BountyService {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MojinLedgerMapper mojinLedgerMapper;

    @Resource
    private BountyPostMapper bountyPostMapper;

    @Resource
    private BountyClaimMapper bountyClaimMapper;

    @Resource
    private OrderNoGenerator orderNoGenerator;

    @Transactional
    public BountyCreateResponse createBounty(BountyCreateRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        BigDecimal rewardMojin = normalizeAmount(request.getRewardMojin());
        int recruitTargetCount = request.getRecruitTargetCount() == null ? 0 : request.getRecruitTargetCount();
        if (recruitTargetCount <= 0) {
            throw new BusinessException(400, "招募数量必须大于0");
        }

        BigDecimal totalReward = rewardMojin.multiply(BigDecimal.valueOf(recruitTargetCount))
                .setScale(2, RoundingMode.HALF_UP);

        int deducted = userMapper.deductMojinBalance(currentUser.getId(), totalReward);
        if (deducted == 0) {
            throw new BusinessException(400, "摸金币余额不足，无法发起悬赏");
        }

        BountyPost post = new BountyPost();
        post.setBountyNo(orderNoGenerator.generate("BOU"));
        post.setCreatorUserId(currentUser.getId());
        post.setCreatorCreditScore(currentUser.getCreditScore() == null ? 0 : currentUser.getCreditScore());
        post.setTargetRoleName(trimToLength(request.getTargetRoleName(), 100));
        post.setTitle(trimToLength(request.getTitle(), 100));
        post.setRewardMojin(rewardMojin);
        post.setTotalRewardMojin(totalReward);
        post.setGameType(trimToLength(request.getGameType(), 50));
        post.setDescription(trimToLength(request.getDescription(), 1000));
        post.setRecruitTargetCount(recruitTargetCount);
        post.setRecruitCurrentCount(0);
        post.setStatus("OPEN");
        bountyPostMapper.insert(post);

        User refreshedUser = userMapper.selectById(currentUser.getId());
        BigDecimal afterBalance = refreshedUser == null || refreshedUser.getMojinBalance() == null
                ? BigDecimal.ZERO
                : refreshedUser.getMojinBalance();
        BigDecimal beforeBalance = afterBalance.add(totalReward);

        MojinLedger ledger = new MojinLedger();
        ledger.setUserId(currentUser.getId());
        ledger.setChangeAmount(totalReward.negate());
        ledger.setBeforeBalance(beforeBalance);
        ledger.setAfterBalance(afterBalance);
        ledger.setChangeType("BOUNTY_CREATE");
        ledger.setRelatedId(post.getBountyNo());
        ledger.setRelatedType("BOUNTY");
        ledger.setDescription(String.format("发起悬赏，预扣%s摸金币", totalReward.toPlainString()));
        mojinLedgerMapper.insert(ledger);

        BountyCreateResponse response = new BountyCreateResponse();
        response.setBountyId(post.getId());
        response.setBountyNo(post.getBountyNo());
        response.setRewardMojin(post.getRewardMojin());
        response.setTotalRewardMojin(post.getTotalRewardMojin());
        response.setRecruitTargetCount(post.getRecruitTargetCount());
        response.setRemainingMojinBalance(afterBalance);
        return response;
    }

    public BountyListResponse getBountyList(BountyListRequest request, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }

        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : Math.min(request.getSize(), 50);
        int offset = (page - 1) * size;

        List<BountyListResponse.BountyItem> items = bountyPostMapper.selectBountyList(
                currentUser.getId(),
                normalizeKeyword(request.getGameType()),
                normalizeKeyword(request.getKeyword()),
                offset,
                size
        );
        Long total = bountyPostMapper.countBountyList(
                normalizeKeyword(request.getGameType()),
                normalizeKeyword(request.getKeyword())
        );

        BountyListResponse response = new BountyListResponse();
        response.setPage(page);
        response.setSize(size);
        response.setTotal(total == null ? 0 : total.intValue());
        response.setTotalPages(total == null || total == 0 ? 0 : (int) Math.ceil(total.doubleValue() / size));
        response.setItems(items == null ? Collections.emptyList() : items);
        return response;
    }

    public BountyDetailResponse getBountyDetail(Long bountyId, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (bountyId == null) {
            throw new BusinessException(400, "悬赏ID不能为空");
        }

        BountyDetailResponse detail = bountyPostMapper.selectBountyDetail(currentUser.getId(), bountyId);
        if (detail == null) {
            throw new BusinessException(404, "悬赏不存在");
        }
        return detail;
    }

    @Transactional
    public BountyClaimResponse claimBounty(Long bountyId, String token) {
        User currentUser = userService.getCurrentUser(token);
        if (currentUser == null) {
            throw new BusinessException(401, "用户未登录");
        }
        if (bountyId == null) {
            throw new BusinessException(400, "悬赏ID不能为空");
        }

        BountyPost post = bountyPostMapper.selectById(bountyId);
        if (post == null) {
            throw new BusinessException(404, "悬赏不存在");
        }
        if (!"OPEN".equals(post.getStatus())) {
            throw new BusinessException(400, "该悬赏暂不可接取");
        }
        if (post.getCreatorUserId() != null && post.getCreatorUserId().equals(currentUser.getId())) {
            throw new BusinessException(400, "不能接取自己发起的悬赏");
        }
        if (post.getRecruitCurrentCount() != null && post.getRecruitTargetCount() != null
                && post.getRecruitCurrentCount() >= post.getRecruitTargetCount()) {
            throw new BusinessException(400, "该悬赏已招满");
        }

        BountyClaim existing = bountyClaimMapper.selectByBountyAndHunter(bountyId, currentUser.getId());
        if (existing != null) {
            throw new BusinessException(400, "你已接取过该悬赏");
        }

        BountyClaim claim = new BountyClaim();
        claim.setBountyId(bountyId);
        claim.setHunterUserId(currentUser.getId());
        claim.setStatus("CLAIMED");
        bountyClaimMapper.insert(claim);

        int updated = bountyPostMapper.incrementClaimCount(bountyId);
        if (updated == 0) {
            throw new BusinessException(400, "该悬赏已被其他摸金校尉抢满");
        }

        BountyPost refreshed = bountyPostMapper.selectById(bountyId);
        BountyClaimResponse response = new BountyClaimResponse();
        response.setBountyId(bountyId);
        response.setRecruitCurrentCount(refreshed == null ? post.getRecruitCurrentCount() : refreshed.getRecruitCurrentCount());
        response.setRecruitTargetCount(refreshed == null ? post.getRecruitTargetCount() : refreshed.getRecruitTargetCount());
        response.setStatus(refreshed == null ? post.getStatus() : refreshed.getStatus());
        return response;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "悬赏摸金币必须大于0");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
