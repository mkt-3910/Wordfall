package com.example.wordfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import com.example.wordfall.dto.GameDtos.LandingRequest;
import com.example.wordfall.dto.GameDtos.LandingResponse;
import com.example.wordfall.entity.Game;
import com.example.wordfall.repository.GameRepository;
import com.example.wordfall.service.GameService;
import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
class GameApiTest extends ApiTestSupport {

    @Autowired
    GameRepository games;

    @Autowired
    GameService gameService;

    @Test
    void landingIsComputedByTheServerAndRetriesGetTheSameAnswer() throws Exception {
        Cookie player = newPlayer();
        JsonNode game = startGame(player);
        String url = "/api/games/" + game.get("gameId").asText() + "/landings";
        Map<String, Integer> landing = hardDrop(game.get("board"), game.get("piece"), 0);

        String first = postJson(player, url, landing).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String retry = postJson(player, url, landing).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(json.readTree(first), json.readTree(retry));
        assertEquals(0, json.readTree(first).get("index").asInt());
        assertEquals(1, games.findById(UUID.fromString(game.get("gameId").asText())).orElseThrow().getLandingCount());
        // 同じ番号で別の位置、または番号の飛び越しは受け付けない
        postJson(player, url, Map.of("index", 0, "x", 0, "y", 0, "rotation", 0)).andExpect(status().isConflict());
        postJson(player, url, Map.of("index", 5, "x", 0, "y", 0, "rotation", 0)).andExpect(status().isConflict());
    }

    @Test
    void rejectsImpossibleLandingsAndOtherPlayersGames() throws Exception {
        Cookie player = newPlayer();
        JsonNode game = startGame(player);
        String url = "/api/games/" + game.get("gameId").asText() + "/landings";
        JsonNode piece = game.get("piece");

        // 空中で止める・盤面の外に置く・回転値の改ざんは400
        postJson(player, url, Map.of("index", 0, "x", piece.get("x").asInt(), "y", 3, "rotation", 0))
                .andExpect(status().isBadRequest());
        postJson(player, url, Map.of("index", 0, "x", 50, "y", 11, "rotation", 0)).andExpect(status().isBadRequest());
        postJson(player, url, Map.of("index", 0, "x", 4, "y", 11, "rotation", 9)).andExpect(status().isBadRequest());
        postJson(player, url, Map.of("index", 0)).andExpect(status().isBadRequest());

        Cookie stranger = newPlayer();
        startGame(stranger);
        postJson(stranger, url, hardDrop(game.get("board"), piece, 0)).andExpect(status().isNotFound());
        postJson(stranger, "/api/games/" + game.get("gameId").asText() + "/quit", Map.of())
                .andExpect(status().isNotFound());
    }

    @Test
    void fullGameIsRecordedOnlyForItsOwnPlayer() throws Exception {
        Cookie player = newPlayer();
        JsonNode game = startGame(player);
        String url = "/api/games/" + game.get("gameId").asText() + "/landings";
        JsonNode board = game.get("board");
        JsonNode piece = game.get("piece");
        Set<String> completedWords = new HashSet<>();
        JsonNode result = null;
        Map<String, Integer> lastRequest = null;
        JsonNode lastResponse = null;
        int lives = 3;
        for (int index = 0; index < 1000 && result == null; index++) {
            lastRequest = hardDrop(board, piece, index);
            JsonNode landing = read(postJson(player, url, lastRequest).andExpect(status().isOk()));
            lastResponse = landing;
            landing.get("words").forEach(word -> completedWords.add(word.get("word").asText()));
            if (landing.get("lifeLost").asBoolean()) lives--;
            assertEquals(lives, landing.get("life").asInt());
            board = landing.get("board");
            piece = landing.get("nextPiece");
            result = landing.get("result").isNull() ? null : landing.get("result");
        }

        assertNotNull(result, "dropping every piece in the middle must end the game");
        assertEquals("GAME_OVER", result.get("reason").asText());
        assertTrue(result.get("newHighScore").asBoolean() || result.get("score").asLong() == 0);

        JsonNode history = getJson(player, "/api/score/list?page=0&size=5");
        assertEquals(1, history.get("content").size());
        assertEquals(result.get("score").asLong(), history.get("content").get(0).get("score").asLong());
        assertEquals(result.get("score").asLong(), getJson(player, "/api/score/high").get("score").asLong());

        JsonNode vocabulary = getJson(player, "/api/word-log/list?page=0&size=100");
        assertEquals(completedWords.size(), vocabulary.get("content").size());
        vocabulary.get("content").forEach(entry -> assertFalse(entry.get("meaning").asText().isBlank()));

        // 他のブラウザ(プレイヤー)からは見えない
        Cookie stranger = newPlayer();
        assertEquals(0, getJson(stranger, "/api/score/list").get("content").size());
        assertEquals(0, getJson(stranger, "/api/word-log/list").get("content").size());
        assertEquals(0, getJson(stranger, "/api/score/high").get("score").asLong());
        getJson(stranger, "/api/achievements").forEach(item -> assertFalse(item.get("unlocked").asBoolean()));

        // 終わったゲームには着地できないが、最後の着地の再送には同じ応答を返す
        postJson(player, url, Map.of("index", 999, "x", 0, "y", 0, "rotation", 0)).andExpect(status().isConflict());
        assertEquals(lastResponse, read(postJson(player, url, lastRequest).andExpect(status().isOk())));
    }

    @Test
    void quittingRecordsPlayedGamesAndDropsEmptyOnes() throws Exception {
        Cookie player = newPlayer();
        JsonNode empty = startGame(player);
        postJson(player, "/api/games/" + empty.get("gameId").asText() + "/quit", Map.of())
                .andExpect(status().isNoContent());

        JsonNode played = startGame(player);
        String id = played.get("gameId").asText();
        postJson(player, "/api/games/" + id + "/landings", hardDrop(played.get("board"), played.get("piece"), 0))
                .andExpect(status().isOk());
        // 途中のプレイが残ったまま次を始めると、残っていた方は中断として記録される
        startGame(player);

        assertEquals(Game.Status.ABANDONED, games.findById(UUID.fromString(empty.get("gameId").asText())).orElseThrow().getStatus());
        Game quit = games.findById(UUID.fromString(id)).orElseThrow();
        assertEquals(Game.Status.FINISHED, quit.getStatus());
        assertEquals("QUIT", quit.getEndReason().name());
        assertEquals(1, getJson(player, "/api/score/list").get("content").size());
    }

    @Test
    void concurrentRetriesOfOneLandingAreAppliedOnce() throws Exception {
        UUID playerId = UUID.randomUUID();
        var game = gameService.start(playerId);
        Map<String, Integer> drop = hardDrop(json.valueToTree(game.board()), json.valueToTree(game.piece()), 0);
        LandingRequest request = new LandingRequest(0, drop.get("x"), drop.get("y"), 0);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<LandingResponse>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Callable<LandingResponse> call = () -> {
                    start.await();
                    return gameService.land(playerId, game.gameId(), request);
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            LandingResponse firstResponse = futures.get(0).get(20, TimeUnit.SECONDS);
            for (Future<LandingResponse> future : futures) {
                assertEquals(json.writeValueAsString(firstResponse), json.writeValueAsString(future.get(20, TimeUnit.SECONDS)));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, games.findById(game.gameId()).orElseThrow().getLandingCount());
    }

    @Test
    void writeApisAcceptOnlySmallJsonBodies() throws Exception {
        Cookie player = newPlayer();
        mvc.perform(post("/api/games").cookie(player).contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/games").cookie(player).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pad\":\"" + "x".repeat(5000) + "\"}"))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/games").cookie(player).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isLengthRequired());
        postJson(player, "/api/games/not-a-uuid/landings", Map.of()).andExpect(status().isBadRequest());
    }

    @Test
    void paginationIsValidated() throws Exception {
        Cookie player = newPlayer();
        mvc.perform(get("/api/score/list?page=-1&size=5").cookie(player)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/score/list?page=0&size=51").cookie(player)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/word-log/list?page=0&size=101").cookie(player)).andExpect(status().isBadRequest());
    }
}
