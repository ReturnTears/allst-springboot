package com.allst.springboot.config;

import com.allst.springboot.common.api.ApiResult;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.web.JsonResponses;
import com.allst.springboot.security.auth.JsonLoginFailureHandler;
import com.allst.springboot.security.auth.JsonLoginSuccessHandler;
import com.allst.springboot.security.auth.LoginGuard;
import com.allst.springboot.security.auth.LoginProtectionFilter;
import com.allst.springboot.security.auth.RestAccessDeniedHandler;
import com.allst.springboot.security.auth.RestAuthenticationEntryPoint;
import com.allst.springboot.security.jwt.JwtAuthenticationFilter;
import com.allst.springboot.security.jwt.JwtTokenService;
import com.allst.springboot.security.jwt.PasswordExpiryFilter;
import com.allst.springboot.security.session.LoginSessionStrategy;
import com.allst.springboot.security.user.SecurityUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 四条过滤链：JWT 接口、HTTP Basic、浏览器会话、其余公开路径。
 * 定义了 SecurityFilterChain 之后，Spring Boot 自带的默认登录链会退出。见 docs/README.md。
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class WebSecurityConfig {

    private final ObjectMapper objectMapper;
    private final SecurityProperties properties;

    public WebSecurityConfig(ObjectMapper objectMapper, SecurityProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider(SecurityUserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider daoAuthenticationProvider) {
        return new org.springframework.security.authentication.ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService tokens, SecurityUserDetailsService users) {
        return new JwtAuthenticationFilter(tokens, users, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordExpiryFilter passwordExpiryFilter() {
        return new PasswordExpiryFilter(objectMapper);
    }

    @Bean
    public FilterRegistrationBean<PasswordExpiryFilter> passwordExpiryRegistration(PasswordExpiryFilter filter) {
        FilterRegistrationBean<PasswordExpiryFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LoginProtectionFilter> loginProtection(List<LoginGuard> guards) {
        FilterRegistrationBean<LoginProtectionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoginProtectionFilter(properties, objectMapper, guards));
        registration.addUrlPatterns("/api/auth/login", "/api/auth/password-expired", "/api/auth/ldap", "/session/login");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * 无状态 JWT。口令登录和刷新在控制器里完成，过滤器只认访问令牌。见 docs/09-jwt.md。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                        PasswordExpiryFilter passwordExpiryFilter,
                                        DefaultWebSecurityExpressionHandler webExpressionHandler) throws Exception {
        http.antMatcher("/api/**");
        http.csrf().disable();
        http.cors();
        http.requestCache().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        applyHeaders(http);
        http.exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, false))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.authorizeRequests()
                .expressionHandler(webExpressionHandler)
                .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/password-expired", "/api/auth/ldap").permitAll()
                .antMatchers(HttpMethod.GET, "/api/password/policy").permitAll()
                .antMatchers(HttpMethod.GET, "/api/users", "/api/users/**").hasAuthority("user:read")
                .antMatchers(HttpMethod.POST, "/api/users", "/api/users/**").hasAuthority("user:write")
                .antMatchers(HttpMethod.PUT, "/api/users", "/api/users/**").hasAuthority("user:write")
                .antMatchers("/api/audit/**").hasAuthority("audit:read")
                .antMatchers("/api/method/**").authenticated()
                .anyRequest().authenticated();
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(passwordExpiryFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * HTTP Basic，给不能带 Bearer 的调用方。每次请求都重新认证。见 docs/06-protocol-integration.md。
     */
    @Bean
    @Order(2)
    public SecurityFilterChain basicChain(HttpSecurity http, DefaultWebSecurityExpressionHandler webExpressionHandler)
            throws Exception {
        http.antMatcher("/basic/**");
        http.csrf().disable();
        http.cors();
        http.requestCache().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        applyHeaders(http);
        http.httpBasic().authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, true));
        http.exceptionHandling().accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.authorizeRequests()
                .expressionHandler(webExpressionHandler)
                .anyRequest().authenticated();
        return http.build();
    }

    /**
     * 浏览器会话：固定会话、并发下线、CSRF Cookie。登录体是表单，不是 JSON。见 docs/04-session-management.md。
     */
    @Bean
    @Order(3)
    public SecurityFilterChain sessionChain(HttpSecurity http, AuthenticationManager authenticationManager,
                                            SessionRegistry sessionRegistry,
                                            DefaultWebSecurityExpressionHandler webExpressionHandler) throws Exception {
        http.antMatcher("/session/**");
        http.cors();
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        applyHeaders(http);
        http.exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, false))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionStrategy((request, response) -> JsonResponses.write(response, objectMapper, 401,
                        ApiResult.fail(ResultCode.SESSION_EXPIRED, "会话已失效")))
                .sessionFixation().changeSessionId()
                .maximumSessions(properties.getSession().getMaximum())
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry)
                .expiredSessionStrategy(event -> JsonResponses.write(event.getResponse(), objectMapper, 401,
                        ApiResult.fail(ResultCode.SESSION_EXPIRED, "会话已在其他地方登录")));
        http.authorizeRequests()
                .expressionHandler(webExpressionHandler)
                .antMatchers(HttpMethod.POST, "/session/login").permitAll()
                .antMatchers(HttpMethod.GET, "/session/csrf").permitAll()
                .antMatchers("/session/active").hasRole("ADMIN")
                .anyRequest().authenticated();
        http.logout()
                .logoutUrl("/session/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                        JsonResponses.write(response, objectMapper, 200, ApiResult.ok("已退出")));
        UsernamePasswordAuthenticationFilter loginFilter = new UsernamePasswordAuthenticationFilter();
        loginFilter.setFilterProcessesUrl("/session/login");
        loginFilter.setAuthenticationManager(authenticationManager);
        loginFilter.setAuthenticationSuccessHandler(new JsonLoginSuccessHandler(objectMapper));
        loginFilter.setAuthenticationFailureHandler(new JsonLoginFailureHandler(objectMapper));
        loginFilter.setSessionAuthenticationStrategy(new LoginSessionStrategy(sessionRegistry, properties.getSession().getMaximum()));
        http.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 兜底链。只放行探测接口和错误页，其余拒绝。
     */
    @Bean
    @Order(4)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http.antMatcher("/**");
        http.csrf().disable();
        http.cors();
        applyHeaders(http);
        http.exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, false))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.authorizeRequests()
                .antMatchers("/hello/**", "/error").permitAll()
                .anyRequest().denyAll();
        return http.build();
    }

    private void applyHeaders(HttpSecurity http) throws Exception {
        http.headers()
                .contentTypeOptions().and()
                .frameOptions().deny()
                .xssProtection().block(true).and()
                .cacheControl().and()
                .referrerPolicy().policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN).and()
                .contentSecurityPolicy("default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
    }
}
