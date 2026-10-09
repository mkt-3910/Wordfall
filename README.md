# 言葉落とし (WordFall)

落ちてくるアルファベットのミノを並べて英単語を作るパズルゲームです。完成した単語は日本語の意味つきで単語帳に保存されます。

## 仕組み

- **判定はすべてサーバー側**: ブラウザが送るのは「どのミノを、どこに、何回回転して置いたか」だけです。ミノの文字・単語の判定・得点・次のミノはサーバー (`com.example.wordfall.game`) が決めるため、スコアは改ざんできません。着地位置は、出現位置から移動・落下・回転でたどり着けるかを検証しています。
- **プレイヤーはブラウザ単位**: 初回アクセス時に、ランダムなIDを HttpOnly Cookie (`wordfall_player`) で発行します。単語帳・実績・履歴・ハイスコアはこのIDごとに分かれます。ログインはありません。Cookie を消すと記録は引き継がれません。
- **DB**: テーブルは Flyway (`src/main/resources/db/migration`) で作成します。ローカルは H2、本番は PostgreSQL を想定しています。

## ローカルで動かす

```bash
mvn spring-boot:run
# http://localhost:8080/
```

データは `./data/wordfall.mv.db` に保存されます。テンプレートの変更をすぐ反映したいときは `SPRING_PROFILES_ACTIVE=dev` を付けます。

## テスト

```bash
mvn verify                          # Java (H2)
node --test src/test/js/*.test.cjs  # 画面側の JavaScript

# PostgreSQL でも確認する場合
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wordfall_test \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD=... mvn test
```

GitHub Actions では、H2・PostgreSQL・JavaScript の3種類のテストを実行します。

## 公開する

`mvn package` で作った `target/wordfall-0.0.1-SNAPSHOT.jar` を、Java 17 以上の環境で動かします。HTTPS は、前段のロードバランサーやリバースプロキシで終端させてください。

| 環境変数 | 例 | 説明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` | 公開用の設定を使う（必須） |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/wordfall` | DB の接続先（必須。JDBC 形式で指定） |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | | DB のユーザーとパスワード |
| `PORT` | `8080` | 待ち受けポート |
| `COOKIE_SECURE` | `true` | Cookie に Secure を付ける（prod の既定値は true。HTTPS でない検証環境だけ false にする） |

`prod` プロファイルの内容:
- 全アドレスで待ち受けます。
- プロキシの `X-Forwarded-*` ヘッダーを信頼します。
- 終了時は処理中のリクエストを待ってから止まります（グレースフルシャットダウン）。

起動時に Flyway がテーブルを作成・更新します。

### 公開前に確認すること

- **回数制限**: 書き込み API には、接続元 IP ごとに 1 分あたりの回数制限があります（`wordfall.rate-limit.*`）。サーバー 1 台のメモリで数えているので、複数台で動かす場合はプロキシや WAF 側でも制限してください。
- **プロキシの設定**: プロキシが `X-Forwarded-For` を正しく付け替えないと、回数制限が全員まとめて 1 つの IP として数えられます。
- **DB のバックアップ**: DB のバックアップは、ホスティング側の機能で設定してください。

## 以前のバージョンからの移行

以前は全員で 1 つのデータを共有する設計で、DB ファイルは `./data/wordfalldb.mv.db` でした。プレイヤーごとの設計に変わったため、旧データは引き継がずに `./data/wordfall.mv.db` に新しく作成します（旧ファイルは削除していません）。
