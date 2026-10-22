package com.allst.springboot.web;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.header.RequestContext;
import com.allst.springboot.common.header.RequestMeta;
import com.allst.springboot.config.NormProperties;
import com.allst.springboot.security.ClientIpResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * IP 白名单。默认不信任 X-Forwarded-For。对应规范：3 IP 白名单。
 */
@Component
public class IpWhitelistInterceptor implements HandlerInterceptor {

    private final NormProperties properties;

    public IpWhitelistInterceptor(NormProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }
        if (!properties.getIp().isEnabled()) {
            return true;
        }
        String ip = currentIp(request);
        boolean allowed = properties.getIp().getWhitelist().stream()
                .map(ClientIpResolver::normalize)
                .anyMatch(ip::equals);
        if (!allowed) {
            throw new BizException(ResultCode.IP_FORBIDDEN, "IP 不在白名单: " + ip);
        }
        return true;
    }

    private String currentIp(HttpServletRequest request) {
        RequestMeta meta = RequestContext.get();
        if (meta != null && meta.getClientIp() != null) {
            return meta.getClientIp();
        }
        return ClientIpResolver.resolve(request, properties.getIp().isTrustForwarded());
    }
}
