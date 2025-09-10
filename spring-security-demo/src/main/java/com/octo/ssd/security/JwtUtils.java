package com.octo.ssd.security;

import com.octo.ssd.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


@Slf4j
@Component
public class JwtUtils {

    private static SecretKey KEY;

    private static String ISSUER;

    private static String SUBJECT;

    private static int ACCESS_EXPIRE;

    @Resource
    private JwtProperties jwtProperties;

    // 配置文件信息 赋值 静态字段
    @PostConstruct
    public void init() {
        KEY = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        ISSUER = jwtProperties.getIssuer();
        SUBJECT = jwtProperties.getSubject();
        ACCESS_EXPIRE = jwtProperties.getAccessExpire();
    }

    /**
     * 创建token
     *
     * @param username 用户名
     * @return token
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date exprireDate = Date.from(Instant.now().plusMillis(ACCESS_EXPIRE));
        String uuid = UUID.randomUUID().toString();
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                // 设置自定义负载信息payload
                .claim("username", username)
                // 令牌ID
                .id(uuid)
                // 过期日期
                .expiration(exprireDate)
                // 签发时间
                .issuedAt(now)
                // 主题
                .subject(SUBJECT)
                // 签发者
                .issuer(ISSUER)
                // 签名
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
            return true;
        } catch (Exception e) {
            log.error("e: ", e);
        }
        return false;
    }

    public static String getUsername(String token) {
        Claims claims = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
        return (String) claims.get("username");
    }

}
