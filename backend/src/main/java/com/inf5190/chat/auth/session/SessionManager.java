package com.inf5190.chat.auth.session;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Repository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Repository
public class SessionManager {

    private static final String SECRET_KEY_BASE64 = "6jGzuf4Cp0mHcUoB3884594872519FA6EFEB24B69442DAGmXslXT8p79SQ8m";
    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    public SessionManager() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY_BASE64));
        this.jwtParser = Jwts.parser()
                             .setSigningKey(secretKey)
                             .build();
    }

    public String addSession(SessionData authData) {
        long expirationTimeMs = 2 * 60 * 60 * 1000;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTimeMs);


        return Jwts.builder()
                .setSubject(authData.username())
                .setAudience("http://127.0.0.1")
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact();  
    }


    public SessionData getSession(String token) {
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            String username = claims.getSubject();
            System.out.println("");
            return new SessionData(username);
        } catch (JwtException ex) {
            return null;
        }
    }
}