package com.example.wordfall;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.wordfall.game.Board;
import com.example.wordfall.game.Piece;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** ブラウザと同じようにCookieでプレイヤーを分けてAPIを呼ぶためのヘルパー。 */
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected static Cookie newPlayer() {
        return new Cookie("wordfall_player", UUID.randomUUID().toString());
    }

    protected ResultActions postJson(Cookie player, String url, Object body) throws Exception {
        return mvc.perform(post(url).cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    protected JsonNode getJson(Cookie player, String url) throws Exception {
        return read(mvc.perform(get(url).cookie(player)).andExpect(status().isOk()));
    }

    protected JsonNode startGame(Cookie player) throws Exception {
        return read(postJson(player, "/api/games", Map.of()).andExpect(status().isOk()));
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** ミノを出現位置の真下にまっすぐ落としたときの着地リクエスト。 */
    protected static Map<String, Integer> hardDrop(JsonNode board, JsonNode piece, int index) {
        StringBuilder text = new StringBuilder();
        board.forEach(row -> text.append(row.asText()));
        Board parsed = Board.parse(text.toString());
        List<Piece.Cell> cells = new ArrayList<>();
        piece.get("cells").forEach(cell ->
                cells.add(new Piece.Cell(cell.get("x").asInt(), cell.get("y").asInt(), cell.get("letter").asText().charAt(0))));
        int x = piece.get("x").asInt();
        int y = piece.get("y").asInt();
        while (!parsed.collides(cells, x, y + 1)) y++;
        return Map.of("index", index, "x", x, "y", y, "rotation", 0);
    }
}
