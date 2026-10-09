package com.example.wordfall.web;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 書き込みAPI(POST)の乱用を防ぐ。
 * ・リクエスト本文の大きさを制限する
 * ・接続元IPごとに1分あたりの回数を制限する(ゲーム開始はより厳しく)
 * 1台のサーバーで動かす前提のメモリ内カウンター。複数台で動かす場合はリバースプロキシ側で制限する。
 */
@Component
@Order(1)
public class ApiGuardFilter extends OncePerRequestFilter {

    static final int MAX_BODY_BYTES = 4 * 1024;
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int postsPerMinute;
    private final int startsPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public ApiGuardFilter(@Value("${wordfall.rate-limit.posts-per-minute:300}") int postsPerMinute,
                          @Value("${wordfall.rate-limit.starts-per-minute:20}") int startsPerMinute) {
        this.postsPerMinute = postsPerMinute;
        this.startsPerMinute = startsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length < 0 || length > MAX_BODY_BYTES) {
            response.sendError(length < 0 ? HttpStatus.LENGTH_REQUIRED.value() : HttpStatus.PAYLOAD_TOO_LARGE.value());
            return;
        }
        String client = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        boolean allowed = tryAcquire("post|" + client, postsPerMinute, now)
                && (!"/api/games".equals(request.getRequestURI()) || tryAcquire("start|" + client, startsPerMinute, now));
        if (!allowed) {
            response.setHeader("Retry-After", "60");
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tryAcquire(String key, int limit, long now) {
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(window -> now - window.start >= WINDOW_MILLIS);
        }
        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.start >= WINDOW_MILLIS ? new Window(now) : current);
        synchronized (window) {
            return ++window.count <= limit;
        }
    }

    private static final class Window {
        private final long start;
        private int count;

        Window(long start) {
            this.start = start;
        }
    }
}
