package com.example.wordfall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autocofigure.SpringBootApplication;

// @SpringBootApplication は3つのアノテーションをまとめたもの:
// ①@Configuration  → このクラスが設定用のクラスであることを示す
// ②@EnableAutoConfiguration → pom.xmlの依存関係を見て、必要な設定を自動で行う
// ③@ComponentScan  → 同じパッケージ以下にある@Controller等のクラスを自動で見つけて登録する
@SpringBootApplication
public class WordfallApplication {

    //Javaプログラムの開始地点(エントリーポイント)
    public static void main(String[] args) {
        // Spring Bootのアプリケーションを起動するための決まり文句
        // 内部でTomcat(組み込みサーバー)を立ち上げて、リクエストを待ち受ける状態にする
        SpringApplication.run(WordfallApplication.class, args);
    }
}
