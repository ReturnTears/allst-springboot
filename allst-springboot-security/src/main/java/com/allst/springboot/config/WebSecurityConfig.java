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
 * 整个项目的安全总开关。Spring Security 不靠在每个接口里手写 if，而是把请求先送进「过滤链」。
 * <p>
 * 这里故意拆成四条链，因为登录方式不一样，不能共用一套规则：
 * <ol>
 *   <li>顺序 1，路径 {@code /api/**}：前端拿 JWT 调接口，不建浏览器会话。</li>
 *   <li>顺序 2，路径 {@code /basic/**}：老系统用的 HTTP Basic，每次请求都带用户名口令。</li>
 *   <li>顺序 3，路径 {@code /session/**}：浏览器 Cookie 会话，要防 CSRF、防会话固定。</li>
 *   <li>顺序 4，路径 {@code /**}：兜底。只放行 {@code /hello/**}，别的地址直接拒绝。</li>
 * </ol>
 * 数字小的链先看。请求的路径一旦被某一条链匹配上，后面的链就不再看。
 * 所以 {@code /api/auth/login} 只会进第 1 条，不会被第 4 条的「全部拒绝」拦住。
 * <p>
 * 只要自己声明了 {@link SecurityFilterChain}，Spring Boot 自带的那套「所有地址都要登录、跳 HTML 登录页」就会退出。
 * 因此必须自己把每条路径的放行或拒绝写全，漏掉的地址会没有保护。见 docs/README.md。
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

    /**
     * 口令只存摘要，不存明文。BCrypt 自带随机盐，同样的口令每次算出来的摘要都不一样，
     * 所以比较时必须用 {@code matches}，不能用字符串相等。10 是计算成本，越大越慢，撞库也更贵。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * 用户名口令的真正校验器。登录时它会调用 {@link SecurityUserDetailsService} 把账号查出来，
     * 再用上面的编码器比对口令。
     * <p>
     * {@code hideUserNotFoundExceptions = true} 的原因：用户不存在和口令错误对外都显示成同一种失败。
     * 否则攻击者可以用返回文案探测哪些用户名已经注册。
     */
    @Bean
    public AuthenticationProvider daoAuthenticationProvider(SecurityUserDetailsService users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    /**
     * 控制器里调用的认证入口。只挂上面这一个校验器，避免口令错误后再去 LDAP 上试一次。
     * LDAP 绑定单独放在 {@code LdapBindClient}，由 {@code POST /api/auth/ldap} 显式调用。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider daoAuthenticationProvider) {
        return new org.springframework.security.authentication.ProviderManager(daoAuthenticationProvider);
    }

    /**
     * 内存里的在线会话表。登录成功后把「谁 + 会话号」登记进来，用来实现「同一账号只留最新一次登录」。
     * 进程重启后这份表会清空，示例够用；多台机器要换成共享存储。
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * 把 Servlet 的会话创建、销毁事件转给 Spring Security。
     * 没有它，用户关掉浏览器或会话超时后，上面的注册表不知道该把这条会话删掉。
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * 浏览器页面在 5173，接口在 1235，这是两个源。默认浏览器会拦这种跨源请求。
     * 开发时 Vite 把请求代理到 1235，浏览器以为还是同一个源；这里仍放开 5173，方便不走代理时直接调接口。
     * {@code allowCredentials = true} 才能带上会话 Cookie，这时来源必须写具体地址，不能写星号。
     */
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

    /**
     * 过滤器只要是 Spring Bean，Boot 还会把它注册成对所有地址生效的 Servlet 过滤器。
     * JWT 过滤器只能待在 {@code /api/**} 这条链里，所以这里把它的自动注册关掉，避免每个请求被校验两次。
     */
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

    /** 同上，密码过期过滤器也只挂在 API 链上，不让 Servlet 容器再跑一遍。 */
    @Bean
    public FilterRegistrationBean<PasswordExpiryFilter> passwordExpiryRegistration(PasswordExpiryFilter filter) {
        FilterRegistrationBean<PasswordExpiryFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 登录保护放在安全链前面，而且只挂这四个登录地址。
     * 它不读取请求体，这样后面的控制器或表单过滤器还能再把用户名口令读出来。
     * 调用链：请求先到这里（维护开关、按 IP 限流）→ 再进入对应的 SecurityFilterChain。
     */
    @Bean
    public FilterRegistrationBean<LoginProtectionFilter> loginProtection(List<LoginGuard> guards) {
        FilterRegistrationBean<LoginProtectionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LoginProtectionFilter(properties, objectMapper, guards));
        registration.addUrlPatterns("/api/auth/login", "/api/auth/password-expired", "/api/auth/ldap", "/session/login");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * API 链。一次带令牌的业务请求会按这个顺序走：
     * <pre>
     * LoginProtectionFilter（仅登录地址）
     *   → 安全响应头
     *   → JwtAuthenticationFilter（有 Bearer 才校验，通过后把用户放进 SecurityContext）
     *   → PasswordExpiryFilter（密码过期就只放行改密相关地址）
     *   → URL 权限规则（hasAuthority / authenticated）
     *   → 控制器
     *   → 方法上的 @PreAuthorize（如果有）
     * </pre>
     * 登录和刷新本身不带访问令牌，所以规则里把它们 permitAll，真正的口令校验在 {@code AuthController} 里做。
     * 见 docs/09-jwt.md。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                        PasswordExpiryFilter passwordExpiryFilter,
                                        DefaultWebSecurityExpressionHandler webExpressionHandler) throws Exception {
        http.antMatcher("/api/**");
        // Bearer 放在请求头里，不靠浏览器自动带上的 Cookie，CSRF 对这条链没有意义，关掉。
        http.csrf().disable();
        http.cors();
        // 无状态接口不需要记住「用户刚才想访问哪个地址」，那个功能会偷偷建会话。
        http.requestCache().disable();
        // STATELESS：即使浏览器带了 JSESSIONID，这条链也不读取、不创建会话。身份只认 JWT。
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        applyHeaders(http);
        http.exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, false))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.authorizeRequests()
                // 角色继承写在这个表达式处理器里，hasRole("ADMIN") 才会同时拥有普通用户和审计员的角色。
                .expressionHandler(webExpressionHandler)
                // 这几个地址是「拿身份」用的，此时调用方还没有令牌，必须放行。
                .antMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/password-expired", "/api/auth/ldap").permitAll()
                .antMatchers(HttpMethod.GET, "/api/password/policy").permitAll()
                .antMatchers(HttpMethod.GET, "/api/users", "/api/users/**").hasAuthority("user:read")
                .antMatchers(HttpMethod.POST, "/api/users", "/api/users/**").hasAuthority("user:write")
                .antMatchers(HttpMethod.PUT, "/api/users", "/api/users/**").hasAuthority("user:write")
                .antMatchers("/api/audit/**").hasAuthority("audit:read")
                // 方法演示接口只要求「已登录」。管理员还是审计员，交给方法上的注解区分。
                .antMatchers("/api/method/**").authenticated()
                // 上面没点名的 /api/**，例如 /api/auth/me、/api/password/change，只要登录过就能进。
                .anyRequest().authenticated();
        // 放在用户名口令过滤器的位置之前。API 链没有启用表单登录，这个位置只是用来固定顺序。
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 必须在 JWT 过滤器之后：那时 SecurityContext 里才有用户，才能判断密码是否过期。
        http.addFilterAfter(passwordExpiryFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Basic 链。调用方每次都在请求头里放 {@code Authorization: Basic base64(用户名:口令)}。
     * 调用链：BasicAuthenticationFilter 取出用户名口令 → AuthenticationManager 校验
     * → 通过后进入 {@code BasicDemoController}。因为是 STATELESS，不会留下会话，所以每次请求都要重新认证。
     * 见 docs/06-protocol-integration.md。
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
        // basic=true 会多写 WWW-Authenticate，告诉客户端「这里要用 Basic，不是跳登录页」。
        http.httpBasic().authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, true));
        http.exceptionHandling().accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.authorizeRequests()
                .expressionHandler(webExpressionHandler)
                .anyRequest().authenticated();
        return http.build();
    }

    /**
     * 会话链。浏览器登录不是 JSON，而是表单字段 username、password。
     * 调用链：
     * <pre>
     * GET  /session/csrf     拿到 CSRF 令牌（放行）
     * POST /session/login    LoginProtectionFilter
     *                        → CSRF 校验
     *                        → UsernamePasswordAuthenticationFilter
     *                        → AuthenticationManager 校验口令
     *                        → LoginSessionStrategy 更换会话号并登记
     *                        → JsonLoginSuccessHandler 返回当前用户
     * GET  /session/me       从会话里取出已经登录的用户
     * POST /session/logout   清掉会话和 Cookie
     * </pre>
     * 见 docs/04-session-management.md。
     */
    @Bean
    @Order(3)
    public SecurityFilterChain sessionChain(HttpSecurity http, AuthenticationManager authenticationManager,
                                            SessionRegistry sessionRegistry,
                                            DefaultWebSecurityExpressionHandler webExpressionHandler) throws Exception {
        http.antMatcher("/session/**");
        http.cors();
        // 会话登录靠 Cookie 识别身份，恶意网站可以偷偷替用户发请求，所以这里必须开 CSRF。
        // HttpOnly 设为 false，是为了让前端脚本读到 XSRF-TOKEN，再放到请求头 X-XSRF-TOKEN。
        http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        applyHeaders(http);
        http.exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper, false))
                .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper));
        http.sessionManagement()
                // 只有登录成功才建会话，匿名访问 /session/csrf 时由 CSRF 过滤器按需处理 Cookie。
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionStrategy((request, response) -> JsonResponses.write(response, objectMapper, 401,
                        ApiResult.fail(ResultCode.SESSION_EXPIRED, "会话已失效")))
                // 登录成功后换一个新的会话号，避免攻击者事先塞给用户一个自己知道的会话号。
                .sessionFixation().changeSessionId()
                // false：超过上限时不拒绝新登录，而是让更早的那次会话失效。
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
        // 不用默认的 /login 跳转页。自己放一个表单过滤器，成功和失败都写 JSON。
        // 这条过滤器不会自动套用上面的 sessionManagement 策略，所以下面又单独设了 LoginSessionStrategy。
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
     * 兜底链，匹配前面三条没接住的地址。
     * {@code /hello/**} 是留给探活的公开接口；{@code /error} 放行是为了容器转发错误页时不再被拦一次。
     * 其余地址 denyAll，避免忘记配置的新接口变成匿名可访问。
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

    /**
     * 每条链都写这些响应头，浏览器会按它们限制页面行为：
     * 不允许被嵌进别的网站的 iframe，不让浏览器猜内容类型，不缓存带身份的响应。
     * CSP 设成 default-src 'none'，因为这些接口返回的是 JSON，不需要加载脚本或样式。
     */
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
