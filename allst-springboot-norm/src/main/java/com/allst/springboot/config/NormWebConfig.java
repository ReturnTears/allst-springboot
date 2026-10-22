package com.allst.springboot.config;

import com.allst.springboot.web.HeaderContextInterceptor;
import com.allst.springboot.web.IpWhitelistInterceptor;
import com.allst.springboot.web.SignatureInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 多个拦截器的执行顺序由 在 WebMvcConfigurer.addInterceptors() 中的注册顺序 决定，
 * 整体遵循 「preHandle 正序、postHandle / afterCompletion 逆序」 的规则。
 * 拦截器顺序：先解析公共头，再校验 IP，最后校验签名。
 */
@Configuration
public class NormWebConfig implements WebMvcConfigurer {

    private final HeaderContextInterceptor headerContextInterceptor;
    private final IpWhitelistInterceptor ipWhitelistInterceptor;
    private final SignatureInterceptor signatureInterceptor;

    public NormWebConfig(HeaderContextInterceptor headerContextInterceptor,
                         IpWhitelistInterceptor ipWhitelistInterceptor,
                         SignatureInterceptor signatureInterceptor) {
        this.headerContextInterceptor = headerContextInterceptor;
        this.ipWhitelistInterceptor = ipWhitelistInterceptor;
        this.signatureInterceptor = signatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerContextInterceptor).addPathPatterns("/**").order(0);
        registry.addInterceptor(ipWhitelistInterceptor).addPathPatterns("/api/**").order(1);
        registry.addInterceptor(signatureInterceptor).addPathPatterns("/api/**").order(2);
    }
}
