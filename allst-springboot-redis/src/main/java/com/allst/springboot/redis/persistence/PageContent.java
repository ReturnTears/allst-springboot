package com.allst.springboot.redis.persistence;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PageContent {
    private Long id;
    private String slug;
    private String title;
    private String content;
    private LocalDateTime updatedAt;
}
