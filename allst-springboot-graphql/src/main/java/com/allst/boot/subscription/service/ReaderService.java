package com.allst.boot.subscription.service;

import com.allst.boot.subscription.graphql.dto.ReaderPage;
import com.allst.boot.subscription.graphql.input.PageQuery;
import com.allst.boot.subscription.graphql.input.ReaderFilter;
import com.allst.boot.subscription.graphql.input.ReaderInput;
import com.allst.boot.subscription.graphql.input.ReaderUpdateInput;
import com.allst.boot.subscription.model.Reader;
import com.allst.boot.subscription.model.ReaderStatus;
import com.allst.boot.subscription.repository.MembershipRepository;
import com.allst.boot.subscription.repository.ReaderRepository;
import com.allst.boot.subscription.support.BusinessException;
import com.allst.boot.subscription.support.PageRequests;
import com.allst.boot.subscription.support.Texts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ReaderService {
    private static final Set<String> SORTS = Set.of("id", "username", "email", "createdAt", "status");

    private final ReaderRepository readerRepository;
    private final MembershipRepository membershipRepository;

    public ReaderService(ReaderRepository readerRepository, MembershipRepository membershipRepository) {
        this.readerRepository = readerRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public ReaderPage search(ReaderFilter filter, PageQuery pageQuery) {
        Pageable pageable = PageRequests.of(pageQuery, "createdAt", SORTS);
        Page<Reader> page = readerRepository.findAll(specification(filter), pageable);
        return new ReaderPage(page.getContent(), PageRequests.info(page));
    }

    @Transactional(readOnly = true)
    public Reader get(Integer id) {
        return readerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "读者不存在"));
    }

    @Transactional
    public Reader create(ReaderInput input) {
        if (input == null) {
            throw new BusinessException("BAD_REQUEST", "缺少读者信息");
        }
        String username = normalizeUsername(input.getUsername());
        String email = normalizeEmail(input.getEmail());
        if (readerRepository.existsByUsername(username)) {
            throw new BusinessException("CONFLICT", "读者姓名已存在");
        }
        if (readerRepository.existsByEmail(email)) {
            throw new BusinessException("CONFLICT", "邮箱已存在");
        }
        Reader reader = new Reader();
        reader.setUsername(username);
        reader.setEmail(email);
        reader.setPhone(normalizePhone(input.getPhone()));
        reader.setStatus(ReaderStatus.ACTIVE);
        return readerRepository.save(reader);
    }

    @Transactional
    public Reader update(ReaderUpdateInput input) {
        if (input == null || input.getId() == null) {
            throw new BusinessException("BAD_REQUEST", "缺少读者编号");
        }
        Reader reader = get(input.getId());
        if (input.getEmail() != null) {
            String email = normalizeEmail(input.getEmail());
            if (readerRepository.existsByEmailAndIdNot(email, reader.getId())) {
                throw new BusinessException("CONFLICT", "邮箱已存在");
            }
            reader.setEmail(email);
        }
        if (input.getPhone() != null) {
            reader.setPhone(normalizePhone(input.getPhone()));
        }
        if (input.getStatus() != null) {
            reader.setStatus(input.getStatus());
        }
        return readerRepository.save(reader);
    }

    @Transactional
    public boolean delete(Integer id) {
        Reader reader = get(id);
        if (membershipRepository.existsByReaderId(id)) {
            throw new BusinessException("REFERENCED", "读者还有订阅记录，先删除订阅");
        }
        readerRepository.delete(reader);
        return true;
    }

    private Specification<Reader> specification(ReaderFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return null;
            }
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            String keyword = Texts.trimToNull(filter.getKeyword());
            if (keyword != null) {
                String pattern = Texts.likeContains(keyword);
                predicates.add(cb.or(
                        cb.like(root.get("username"), pattern, '\\'),
                        cb.like(root.get("email"), pattern, '\\')
                ));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizeUsername(String username) {
        String normalized = Texts.require(username, "读者姓名不能为空");
        if (normalized.length() < 2 || normalized.length() > 32) {
            throw new BusinessException("BAD_REQUEST", "读者姓名需要 2 到 32 个字");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = Texts.require(email, "邮箱不能为空").toLowerCase();
        if (normalized.length() > 128 || !normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new BusinessException("BAD_REQUEST", "邮箱格式不正确");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        String normalized = Texts.trimToNull(phone);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("\\d{6,20}")) {
            throw new BusinessException("BAD_REQUEST", "电话需要 6 到 20 位数字");
        }
        return normalized;
    }
}
