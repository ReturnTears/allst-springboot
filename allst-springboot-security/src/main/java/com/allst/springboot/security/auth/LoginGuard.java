package com.allst.springboot.security.auth;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 登录前的扩展点。返回拒绝原因时登录不会继续。见 docs/08-extensible-architecture.md。
 */
public interface LoginGuard {

    Optional<String> reject(HttpServletRequest request);
}
