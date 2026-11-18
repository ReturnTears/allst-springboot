package com.allst.boot.repository;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 会员表。与订单表无 JPA 关联，靠 memberId 手动关联。 */
@Getter
@Setter
@Entity
@Table(name = "stream_member")
public class MemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(nullable = false, length = 32)
    private String city;
}