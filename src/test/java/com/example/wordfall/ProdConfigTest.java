package com.example.wordfall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/** prod プロファイルの DB 接続先の組み立て。OS の環境変数は使わず、渡した値だけで確かめる。 */
class ProdConfigTest {

    @Test
    void buildsJdbcUrlFromRenderDatabaseSettings() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(Map.of("DB_HOST", "db.internal", "DB_NAME", "wordfall"));
        assertEquals("jdbc:postgresql://db.internal:5432/wordfall", resolver.getRequiredProperty("spring.datasource.url"));
    }

    /** Spring Boot は解決できない ${...} を文字のまま残すので、存在しないホストへの接続で起動が止まる(H2 には切り替わらない)。 */
    @Test
    void neverFallsBackToInMemoryH2WithoutDatabaseSettings() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(Map.of());
        assertEquals("jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}", resolver.getRequiredProperty("spring.datasource.url"));
    }

    private static PropertySourcesPropertyResolver resolver(Map<String, Object> env) throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new MapPropertySource("env", env));
        new PropertiesPropertySourceLoader()
                .load("prod", new ClassPathResource("application-prod.properties"))
                .forEach(sources::addLast);
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
        resolver.setIgnoreUnresolvableNestedPlaceholders(true);
        return resolver;
    }
}
