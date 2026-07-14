package com.example.wordfall.controller;

public class MeaningResponse {
    
    private String word;
    private String partOfSpeech;
    private String definition;

    // コンストラクタ:3つの値を受け取って、フィールドにセットする
    public MeaningResponse(String word,String partOfSpeech,String definition) {
        this.word = word;
        this.partOfSpeech = partOfSpeech;
        this.definition = definition;
    }

    //getter:3つ
    public String getWord() {
        return this.word;
    }

    public String getPartOfSpeech() {
        return this.partOfSpeech;
    }

    public String getDefinition() {
        return this.definition;
    }
}
