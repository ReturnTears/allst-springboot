package com.allst.boot.subscription.service;

import com.allst.boot.model.Book;
import com.allst.boot.repository.BookRepository;
import com.allst.boot.subscription.graphql.MembershipEventBus;
import com.allst.boot.subscription.graphql.dto.MembershipPage;
import com.allst.boot.subscription.graphql.dto.PlanQuote;
import com.allst.boot.subscription.graphql.input.MembershipFilter;
import com.allst.boot.subscription.graphql.input.MembershipInput;
import com.allst.boot.subscription.graphql.input.MembershipUpdateInput;
import com.allst.boot.subscription.graphql.input.PageQuery;
import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.model.MembershipPlan;
import com.allst.boot.subscription.model.MembershipStatus;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MembershipService {
    private static final Set<String> SORTS = Set.of("id", "createdAt", "startDate", "endDate", "price", "status", "plan");
    private static final List<MembershipStatus> OPEN_STATUSES = List.of(MembershipStatus.ACTIVE, MembershipStatus.PAUSED);

    private final MembershipRepository membershipRepository;
    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;
    private final MembershipEventBus eventBus;

    public MembershipService(MembershipRepository membershipRepository,
                             ReaderRepository readerRepository,
                             BookRepository bookRepository,
                             MembershipEventBus eventBus) {
        this.membershipRepository = membershipRepository;
        this.readerRepository = readerRepository;
        this.bookRepository = bookRepository;
        this.eventBus = eventBus;
    }

    @Transactional(readOnly = true)
    public MembershipPage search(MembershipFilter filter, PageQuery pageQuery) {
        Pageable pageable = PageRequests.of(pageQuery, "createdAt", SORTS);
        Page<Membership> page = membershipRepository.findAll(specification(filter), pageable);
        return new MembershipPage(page.getContent(), PageRequests.info(page));
    }

    @Transactional(readOnly = true)
    public Membership get(Integer id) {
        return membershipRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "订阅不存在"));
    }

    @Transactional(readOnly = true)
    public List<Membership> listByReader(Integer readerId) {
        if (!readerRepository.existsById(readerId)) {
            throw new BusinessException("NOT_FOUND", "读者不存在");
        }
        return membershipRepository.findByReaderIdOrderByCreatedAtDesc(readerId);
    }

    public List<PlanQuote> quotes() {
        return Arrays.stream(MembershipPlan.values())
                .map(plan -> new PlanQuote(plan, plan.getDays(), plan.getPrice()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Membership create(MembershipInput input) {
        if (input == null || input.getReaderId() == null || input.getBookId() == null || input.getPlan() == null) {
            throw new BusinessException("BAD_REQUEST", "读者、图书和订阅方案都不能为空");
        }
        Reader reader = readerRepository.findById(input.getReaderId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "读者不存在"));
        if (reader.getStatus() != ReaderStatus.ACTIVE) {
            throw new BusinessException("CONFLICT", "停用的读者不能新建订阅");
        }
        Book book = bookRepository.findById(input.getBookId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "图书不存在"));
        if (book.getId() == null) {
            throw new BusinessException("NOT_FOUND", "图书不存在");
        }
        boolean duplicated = membershipRepository.existsByReaderIdAndBookIdAndStatusIn(
                input.getReaderId(), input.getBookId(), OPEN_STATUSES);
        if (duplicated) {
            throw new BusinessException("CONFLICT", "该读者对这本书已有进行中的订阅");
        }
        LocalDate start = input.getStartDate() == null ? LocalDate.now() : input.getStartDate();
        Membership membership = new Membership();
        membership.setReaderId(reader.getId());
        membership.setBookId(book.getId());
        applyPlan(membership, input.getPlan(), start);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRemark(Texts.trimToNull(input.getRemark()));
        Membership saved = membershipRepository.save(membership);
        publishAfterCommit(saved);
        return saved;
    }

    @Transactional
    public Membership update(MembershipUpdateInput input) {
        if (input == null || input.getId() == null) {
            throw new BusinessException("BAD_REQUEST", "缺少订阅编号");
        }
        Membership membership = get(input.getId());
        if (input.getPlan() != null && input.getPlan() != membership.getPlan()) {
            applyPlan(membership, input.getPlan(), membership.getStartDate());
        }
        if (input.getStatus() != null) {
            membership.setStatus(input.getStatus());
        }
        if (input.getRemark() != null) {
            membership.setRemark(Texts.trimToNull(input.getRemark()));
        }
        Membership saved = membershipRepository.save(membership);
        publishAfterCommit(saved);
        return saved;
    }

    @Transactional
    public Membership cancel(Integer id) {
        Membership membership = get(id);
        if (membership.getStatus() == MembershipStatus.CANCELLED) {
            return membership;
        }
        membership.setStatus(MembershipStatus.CANCELLED);
        Membership saved = membershipRepository.save(membership);
        publishAfterCommit(saved);
        return saved;
    }

    @Transactional
    public boolean delete(Integer id) {
        Membership membership = get(id);
        membershipRepository.delete(membership);
        publishAfterCommit(membership);
        return true;
    }

    private void applyPlan(Membership membership, MembershipPlan plan, LocalDate start) {
        membership.setPlan(plan);
        membership.setStartDate(start);
        membership.setEndDate(start.plusDays(plan.getDays()));
        membership.setPrice(plan.getPrice());
    }

    private void publishAfterCommit(Membership membership) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventBus.publish(membership);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventBus.publish(membership);
            }
        });
    }

    private Specification<Membership> specification(MembershipFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return null;
            }
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getReaderId() != null) {
                predicates.add(cb.equal(root.get("readerId"), filter.getReaderId()));
            }
            if (filter.getBookId() != null) {
                predicates.add(cb.equal(root.get("bookId"), filter.getBookId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPlan() != null) {
                predicates.add(cb.equal(root.get("plan"), filter.getPlan()));
            }
            String keyword = Texts.trimToNull(filter.getKeyword());
            if (keyword != null) {
                String pattern = Texts.likeContains(keyword);
                Subquery<Integer> bookIds = query.subquery(Integer.class);
                Root<Book> book = bookIds.from(Book.class);
                bookIds.select(book.get("id")).where(cb.like(book.get("name"), pattern, '\\'));
                predicates.add(cb.or(
                        cb.like(root.get("remark"), pattern, '\\'),
                        root.get("bookId").in(bookIds)
                ));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
