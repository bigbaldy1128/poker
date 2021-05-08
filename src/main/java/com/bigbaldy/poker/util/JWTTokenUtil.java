package com.bigbaldy.poker.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.lang.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTTokenUtil {

    private static final String TOKEN_TYPE = "Bearer ";
    private static final String USER_CLAIM_KEY = "user_id";

    public static String getTokenString(String authorization) {
        if (hasJWTTokenInAuthorization(authorization)) {
            return authorization.replaceAll(TOKEN_TYPE, "");
        } else {
            return authorization;
        }
    }

    public static String getAccessTokenSignature(String accessToken) {
        String[] split = accessToken.split("\\.");
        if (split.length == 3) {
            return split[2];
        }

        return "";
    }

    public static JWTToken getJWTToken(Long userId, String jwtSecret,
                                       long accessTokenTTLInSeconds,
                                       long refreshTokenTTLInSeconds) {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put(USER_CLAIM_KEY, userId);
        return getJWTToken(additionalClaims, Long.toString(userId), jwtSecret,
                accessTokenTTLInSeconds,
                refreshTokenTTLInSeconds);
    }

    public static Object getClaimFromAuthorization(String authorization,
                                                   String claimKey, String jwtSecret) {
        return getClaimFromAccessToken(getTokenString(authorization), claimKey, jwtSecret);
    }

    private static Claims getClaimBodyFromAccessToken(String accessToken,
                                                      String claimKey, String jwtSecret) {
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(accessToken)
                .getBody();
    }

    public static Date getExpireInFromAuthorization(String accessToken,
                                                    String claimKey, String jwtSecret) {

        return getClaimBodyFromAccessToken(getTokenString(accessToken), claimKey, jwtSecret).getExpiration();
    }

    public static Object getClaimFromAccessToken(String accessToken,
                                                 String claimKey, String jwtSecret) {

        return getClaimBodyFromAccessToken(accessToken, claimKey, jwtSecret).get(claimKey);
    }

    public static boolean hasJWTTokenInAuthorization(String authorization) {
        return authorization.startsWith(TOKEN_TYPE);
    }

    @Nullable
    public static JWTToken refreshToken(String authorization, String jwtSecret,
                                        long accessTokenTTL, long refreshTokenTTL) {
        Object value = getClaimFromAuthorization(authorization, USER_CLAIM_KEY,
                jwtSecret);

        if (value != null) {
            String userId = value.toString();
            Map<String, Object> additionalClaims = new HashMap<>();
            additionalClaims.put(USER_CLAIM_KEY, userId);
            return getJWTToken(additionalClaims, userId, jwtSecret, accessTokenTTL, refreshTokenTTL);
        }

        return null;
    }

    public static JWTToken getJWTToken(Map<String, Object> additionalClaims, String subject,
                                       String jwtSecret, Long accessTokenTTLInSeconds,
                                       Long refreshTokenTTLInSeconds) {
        long now = System.currentTimeMillis();
        long accessTokenTTL = accessTokenTTLInSeconds * 1000L;
        long refreshTokenTTL = refreshTokenTTLInSeconds * 1000L;
        Date currentDate = new Date(now);
        Date expireDate = new Date(now + accessTokenTTL);
        String accessToken = Jwts.builder()
                .addClaims(additionalClaims)
                .setSubject(subject)
                .setIssuedAt(currentDate)
                .setExpiration(new Date(now + accessTokenTTL))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();

        String refreshToken = Jwts.builder()
                .addClaims(additionalClaims)
                .setSubject(subject)
                .setIssuedAt(currentDate)
                .setExpiration(new Date(now + refreshTokenTTL))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();

        return new JWTToken(accessToken, TOKEN_TYPE, expireDate.getTime(), refreshToken);
    }

    public static class JWTToken {

        private String accessToken;
        private String tokenType;
        private Long expiresIn;
        private String refreshToken;

        public JWTToken(String accessToken, String tokenType, Long expiresIn, String refreshToken) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
