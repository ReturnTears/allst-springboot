package com.allst.boot.subscription.repository;

import com.allst.boot.subscription.model.Membership;
import com.allst.boot.subscription.model.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Integer>, JpaSpecificationExecutor<Membership> {
    boolean existsByReaderIdAndBookIdAndStatusIn(Integer readerId, Integer bookId, Collection<MembershipStatus> statuses);

    boolean existsByReaderId(Integer readerId);

    List<Membership> findByReaderIdIn(Collection<Integer> readerIds);

    List<Membership> findByReaderIdOrderByCreatedAtDesc(Integer readerId);

    long countByStatus(MembershipStatus status);
}
