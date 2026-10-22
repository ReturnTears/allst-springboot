package com.allst.springboot.redis.persistence;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BehaviorEvent {
    private Long id;
    private String userId;
    private Long itemId;
    private String action;
    private Integer score;
    private LocalDateTime createdAt;
}
