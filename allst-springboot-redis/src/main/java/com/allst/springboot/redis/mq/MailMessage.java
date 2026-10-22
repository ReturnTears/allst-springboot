package com.allst.springboot.redis.mq;

import lombok.Data;

@Data
public class MailMessage {
    private String id;
    private String to;
    private String subject;
    private String body;
    private long createdAt;
}
