package com.example.wordfall.controller;

mport org.springframework.stereotype.Controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframweork.web.bind.annotation.GetMapping;

// @Controller: このクラスが「画面(HTML)を返すコントローラー」であることを示す
// (JSONを返す@RestControllerとは違い、戻り値の文字列はテンプレート名として扱われる)
@Controller
public class GameController {

    // @GetMapping("/"): ブラウザで http://localhost:8080/ にアクセスした(GETリクエスト)ときに
    // このメソッドが呼ばれる、という紐付け
    @GetMappig("/")
    public String showGame(Model model) {
        // Model: ControllerからHTMLテンプレートへデータを渡すための入れ物
        // ここでは "title" というキーで文字列を1つ渡している
        model.addAttribute("title", "言葉落とし");

        // 戻り値の "game" は、templates/game.html を指す
        // (拡張子.htmlは書かなくてよい。Thymeleafの設定で自動的に補われる)
        return "game";
    }
}
