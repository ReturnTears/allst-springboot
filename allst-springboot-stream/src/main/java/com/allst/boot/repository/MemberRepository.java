package com.allst.boot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    /** 批量查询：这是消除 N+1 的关键——一次 IN 查询拿回所有需要的会员。 */
    List<MemberEntity> findByIdIn(Collection<Long> ids);
}