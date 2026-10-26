package com.allst.springboot.security.auth;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * 登录前的扩展点。{@code LoginProtectionFilter} 在读请求体之前依次询问每个实现。
 * 返回一段原因时直接 503，登录不会继续。要加「维护中」「只允许内网」这类总开关时实现本接口，不必改过滤器。
 */
public interface LoginGuard {

    Optional<String> reject(HttpServletRequest request);
}
