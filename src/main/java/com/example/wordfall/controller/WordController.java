package com.example.wordfall.controller;

import org.springframework.core.io.ClasspathResource;
import org.springfrmaework.web.bind.annotation.GetMapping;
import org.springfrmaework.web.bind.annotation.RequestParam;
import org.springfrmaework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class WordController {

    private Set<String> words = new HashSet<>();

    // アプリ起動時に1回だけ呼ばれるメソッド。ここでwords.txtを読み込む
    @PostConstruct
    public void loadWords() throws IOException {
        ClassPathResource resource = new ClassPathResource("words.txt");
        try(BuffereredReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(),StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line.trim().toUpperCase());
            }
                }
    
    }
    
}
