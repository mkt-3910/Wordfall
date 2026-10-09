package com.example.wordfall.web;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ログインなしでプレイヤーを見分けるため、ランダムなIDを HttpOnly Cookie で発行する。
 * IDはリクエスト属性 {@link #PLAYER_ID} としてコントローラーに渡す。
 */
@Component
@Order(2)
public class PlayerCookieFilter extends OncePerRequestFilter {

    public static final String PLAYER_ID = "wordfall.playerId";
    static final String COOKIE_NAME = "wordfall_player";
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private final boolean secureCookie;

    public PlayerCookieFilter(@Value("${wordfall.cookie.secure:false}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID playerId = readPlayerId(request);
        if (playerId == null) {
            playerId = UUID.randomUUID();
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, playerId.toString())
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(MAX_AGE)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        request.setAttribute(PLAYER_ID, playerId);
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.equals("/favicon.ico");
    }

    private static UUID readPlayerId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                try {
                    return UUID.fromString(cookie.getValue());
                } catch (IllegalArgumentException invalid) {
                    return null;
                }
            }
        }
        return null;
    }
}
