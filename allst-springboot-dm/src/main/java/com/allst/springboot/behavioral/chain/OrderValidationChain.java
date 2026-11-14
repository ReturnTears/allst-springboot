package com.allst.springboot.behavioral.chain;

import com.allst.springboot.domain.OrderRequest;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 责任链（Chain of Responsibility）——下单前的一串前置校验。
 *
 * 场景：参数校验 → 风控 → 库存/限购 → 优惠券 → 黑名单，每个环节都可能拒绝请求。
 * 全写在一个方法里会得到几十行 if 和层层嵌套；链上每个节点只回答「我这一关过不过」，
 * 失败即短路，并留下是哪一环拦下的。
 *
 * Spring 项目里的常见写法：每个 Check 做成 @Component，用 @Order 标顺序，
 * 注入 List&lt;Check&gt; 后遍历；新增校验只加一个 bean，不改链本身。
 * 这里收在一个类里便于一次读完整条链，语义完全一致。
 *
 * 注意：顺序是业务语义，不能靠 bean 名字母序；一定要显式排序。
 */
@Component
public class OrderValidationChain {

    public interface Check {
        String name();

        /** 返回拒绝原因；empty 表示放行 */
        Optional<String> violation(OrderRequest request);
    }

    private final List<Check> checks;

    public OrderValidationChain() {
        this.checks = Arrays.asList(
                new ParameterCheck(),
                new RiskCheck(),
                new StockLimitCheck(),
                new CouponCheck(),
                new BlacklistCheck());
    }

    public ValidationResult validate(OrderRequest request) {
        List<String> passed = new ArrayList<>();
        for (Check check : checks) {
            Optional<String> reason = check.violation(request);
            if (reason.isPresent()) {
                return ValidationResult.rejected(passed, check.name(), reason.get());
            }
            passed.add(check.name());
        }
        return ValidationResult.accepted(passed);
    }

    public List<String> checkNames() {
        List<String> names = new ArrayList<>();
        checks.forEach(c -> names.add(c.name()));
        return Collections.unmodifiableList(names);
    }

    @Getter
    public static final class ValidationResult {
        private final boolean passed;
        private final List<String> passedChecks;
        private final String failedAt;
        private final String reason;

        private ValidationResult(boolean passed, List<String> passedChecks, String failedAt, String reason) {
            this.passed = passed;
            this.passedChecks = Collections.unmodifiableList(new ArrayList<>(passedChecks));
            this.failedAt = failedAt;
            this.reason = reason;
        }

        static ValidationResult accepted(List<String> passedChecks) {
            return new ValidationResult(true, passedChecks, null, null);
        }

        static ValidationResult rejected(List<String> passedChecks, String failedAt, String reason) {
            return new ValidationResult(false, passedChecks, failedAt, reason);
        }
    }

    private static final class ParameterCheck implements Check {
        @Override
        public String name() {
            return "参数校验";
        }

        @Override
        public Optional<String> violation(OrderRequest r) {
            if (r.getBuyer() == null || r.getBuyer().isBlank() || r.getSku() == null || r.getSku().isBlank()) {
                return Optional.of("买家或 SKU 为空");
            }
            if (r.getUnitPrice() == null || r.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0 || r.getQuantity() < 1) {
                return Optional.of("单价或数量不合法");
            }
            return Optional.empty();
        }
    }

    private static final class RiskCheck implements Check {
        @Override
        public String name() {
            return "风控校验";
        }

        @Override
        public Optional<String> violation(OrderRequest r) {
            return r.getRiskScore() >= 90 ? Optional.of("风控分过高: " + r.getRiskScore()) : Optional.empty();
        }
    }

    private static final class StockLimitCheck implements Check {
        @Override
        public String name() {
            return "库存与限购";
        }

        @Override
        public Optional<String> violation(OrderRequest r) {
            return r.getQuantity() > 50 ? Optional.of("超过单笔限购 50 件") : Optional.empty();
        }
    }

    private static final class CouponCheck implements Check {
        @Override
        public String name() {
            return "优惠券校验";
        }

        @Override
        public Optional<String> violation(OrderRequest r) {
            return "EXPIRED".equals(r.getCouponCode()) ? Optional.of("优惠券已过期") : Optional.empty();
        }
    }

    private static final class BlacklistCheck implements Check {
        @Override
        public String name() {
            return "黑名单校验";
        }

        @Override
        public Optional<String> violation(OrderRequest r) {
            return "BLOCKED_BUYER".equalsIgnoreCase(r.getBuyer()) ? Optional.of("买家在黑名单中") : Optional.empty();
        }
    }
}
