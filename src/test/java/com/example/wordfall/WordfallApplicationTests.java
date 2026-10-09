package com.example.wordfall;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class WordfallApplicationTests extends ApiTestSupport {

    @Test
    void allPagesRenderWithSecurityHeaders() throws Exception {
        for (String page : new String[] {"/", "/history", "/vocabulary", "/achievements"}) {
            mvc.perform(get(page))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("言葉落とし")))
                    .andExpect(content().string(not(containsString("<style"))))
                    .andExpect(content().string(not(containsString("style=\""))))
                    .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        }
    }

    @Test
    void firstVisitIssuesHttpOnlyPlayerCookieAndKeepsIt() throws Exception {
        mvc.perform(get("/"))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("wordfall_player="), containsString("HttpOnly"), containsString("SameSite=Lax"))));
        mvc.perform(get("/").cookie(newPlayer()))
                .andExpect(header().doesNotExist("Set-Cookie"));
        // 壊れたCookieは新しいIDに置き換える
        mvc.perform(get("/").cookie(new Cookie("wordfall_player", "broken")))
                .andExpect(header().string("Set-Cookie", containsString("wordfall_player=")));
    }

    @Test
    void apiResponsesAreNotCached() throws Exception {
        mvc.perform(get("/api/achievements").cookie(newPlayer()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void oldTrustingEndpointsAreGone() throws Exception {
        Cookie player = newPlayer();
        postJson(player, "/api/game-results", Map.of()).andExpect(status().isNotFound());
        postJson(player, "/api/check-words", Map.of()).andExpect(status().isNotFound());
        postJson(player, "/api/word-log", Map.of("word", "CAT")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/meaning?word=CAT").cookie(player)).andExpect(status().isNotFound());
    }
}
