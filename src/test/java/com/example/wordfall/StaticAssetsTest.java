package com.example.wordfall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/** 実際のサーバーを起動し、静的ファイルがハッシュ付きURLで長期キャッシュされることを確かめる。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaticAssetsTest {

    @Autowired
    TestRestTemplate http;

    @Test
    void pagesLinkVersionedAssetsThatAreCachedForAYear() {
        String page = http.getForObject("/", String.class);
        for (String pattern : new String[] {"/css/style-[0-9a-f]{32}\\.css", "/js/game-[0-9a-f]{32}\\.js"}) {
            Matcher matcher = Pattern.compile(pattern).matcher(page);
            assertTrue(matcher.find(), pattern + " not found");
            ResponseEntity<String> asset = http.getForEntity(matcher.group(), String.class);
            assertEquals(200, asset.getStatusCode().value());
            assertEquals("max-age=31536000", asset.getHeaders().getCacheControl());
        }
    }
}
