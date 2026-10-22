package com.allst.springboot.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色。code 使用 ROLE_ 前缀，供 hasRole 表达式去掉前缀后匹配。见 docs/02-authorization.md。
 */
@Getter
@Setter
@Entity
@Table(name = "sec_role")
public class SecRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "sec_role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<SecPermission> permissions = new HashSet<>();
}
