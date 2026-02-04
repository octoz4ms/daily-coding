package com.octo.eum.security;

import com.octo.eum.service.LoginTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 
 * 校验流程：
 * 1. JWT验签 + exp
 * 2. 取 uid / ct / tokenId / ver
 * 3. Redis校验 login:{uid}:{ct} == tokenId
 * 4. 校验 token:ver（可选）
 *
 * @author octo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final LoginTokenService loginTokenService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getTokenFromRequest(request);

            // 1. JWT验签 + exp
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                
                // 2. 取关键信息
                Long userId = jwtTokenProvider.getUserId(token);
                ClientType clientType = jwtTokenProvider.getClientType(token);
                String tokenId = jwtTokenProvider.getTokenId(token);
                int tokenVer = jwtTokenProvider.getTokenVersion(token);

                // 3. Redis校验登录态（踢人生效点）
                if (!loginTokenService.validateLogin(userId, clientType, tokenId)) {
                    log.debug("登录态无效: uid={}, ct={}", userId, clientType.getCode());
                    filterChain.doFilter(request, response);
                    return;
                }

                // 4. 校验Token版本（全量失效）
                if (!loginTokenService.validateTokenVersion(userId, tokenVer)) {
                    log.debug("Token版本过期: uid={}, ver={}", userId, tokenVer);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 5. 加载用户信息
                String username = jwtTokenProvider.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 6. 设置认证
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("认证成功: user={}, ct={}", username, clientType.getCode());
            }
        } catch (Exception e) {
            log.debug("Token验证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
