package com.octo.ssd.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;


public class JwtUtils {

    private static final String SECRET = "secretKeyNotTellingYouSecretKeyNotTellingYou";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    private static final String TOKEN_PREFIX = "Bearer ";

    private static final String JWT_ISS = "zms";

    private final static String SUBJECT = "Peripherals";

    public static final int ACCESS_EXPIRE = 60;

    /**
     * 创建token
     * @param username 用户名
     * @return token
     */
    public static String generateToken(String username) {
        Date now = new Date();
        Date exprireDate  = Date.from(Instant.now().plusSeconds(ACCESS_EXPIRE));
        String uuid = UUID.randomUUID().toString();
        return Jwts.builder()
                .header()
                .add("typ","JWT")
                .add("alg","HS256")
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
                .issuer(JWT_ISS)
                // 签名
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        Claims claims = Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
        String username = claims.get("username").toString();
        return false;
    }
}
