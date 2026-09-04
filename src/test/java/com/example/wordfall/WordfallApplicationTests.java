package com.example.wordfall;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WordfallApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("言葉落とし")));
        mockMvc.perform(get("/history")).andExpect(status().isOk());
        mockMvc.perform(get("/vocabulary")).andExpect(status().isOk());
        mockMvc.perform(get("/achievements")).andExpect(status().isOk());
    }

    @Test
    void batchWordCheckReturnsDictionaryMatches() throws Exception {
        mockMvc.perform(post("/api/check-words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"CAT\",\"DOG\",\"ZZZZZZZZZZZZZ\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CAT").value(true))
                .andExpect(jsonPath("$.DOG").value(true))
                .andExpect(jsonPath("$.ZZZZZZZZZZZZZ").value(false));
    }

    @Test
    void invalidScorePayloadReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":10,\"wordCount\":1,\"words\":\"<script>\"}"))
                .andExpect(status().isBadRequest());
    }
}
