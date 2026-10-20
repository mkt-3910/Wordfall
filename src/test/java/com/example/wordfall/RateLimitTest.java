package com.example.wordfall;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "wordfall.rate-limit.starts-per-minute=2")
@AutoConfigureMockMvc
class RateLimitTest extends ApiTestSupport {

    @Test
    void tooManyGameStartsFromOneAddressAreRejected() throws Exception {
        // Cookieを捨てて別人になりすましても、接続元が同じなら制限される
        for (int i = 0; i < 2; i++) {
            postJson(newPlayer(), "/api/games", Map.of()).andExpect(status().isOk());
        }
        Cookie another = newPlayer();
        postJson(another, "/api/games", Map.of())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
