package com.allst.springboot.web.dto;

import com.allst.springboot.domain.entity.LoginAudit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LoginAuditView {

    private final Long id;
    private final String username;
    private final boolean success;
    private final String ip;
    private final String message;
    private final LocalDateTime createdAt;

    public static LoginAuditView from(LoginAudit audit) {
        return new LoginAuditView(audit.getId(), audit.getUsername(), audit.isSuccess(), audit.getIp(),
                audit.getMessage(), audit.getCreatedAt());
    }
}
