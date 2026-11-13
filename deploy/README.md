# deploy（EC2 にアプリを置く手順）

手元でビルドしたものを SCP で EC2 に送り、systemd と Nginx で動かします。
EC2 と RDS は先に [infra/](../infra/README.md) でつくっておきます。

## しくみ
```
ブラウザ ──80番──> Nginx ──┬─ /      → 画面（frontend/dist）
（自分の IP だけ）          └─ /api/  → Spring Boot（127.0.0.1:8080）──5432番──> RDS
```
- 外に開けているのは 80 番だけ（自分の IP から）。8080 番と RDS は外からつながりません。
- Spring Boot は systemd のサービス `springboot-app` として、production プロファイルで動きます。

## ファイル
| ファイル | 置き場所（EC2） | GitHub |
|---|---|---|
| `backend/application-production.yml` | `/opt/springboot-app/application-production.yml` | **上げない**（RDS のパスワード入り） |
| `backend/build/libs/backend-0.0.1-SNAPSHOT.jar` | `/opt/springboot-app/app.jar` | 上げない（ビルドしてできるもの） |
| `db/seed.sql` | `/opt/springboot-app/seed.sql` | 上げる |
| `frontend/dist/` | `/usr/share/nginx/springboot-app/` | 上げない（ビルドしてできるもの） |
| `deploy/springboot-app.service` | `/etc/systemd/system/springboot-app.service` | 上げる |
| `deploy/nginx-springboot-app.conf` | `/etc/nginx/conf.d/springboot-app.conf` | 上げる |

## 1. application-production.yml をつくる（初回だけ）
`backend/application-production.yml` に書きます。`<...>` は `infra` フォルダで調べます。
```bash
terraform output -raw db_address    # 接続先
terraform output -raw db_password   # パスワード（人に見せない）
```
```yaml
spring:
  datasource:
    url: jdbc:postgresql://<db_address>:5432/springboot_app?sslmode=require
    username: app
    password: <db_password>
  sql:
    init:
      mode: always          # 初回だけ。1回起動したら never に変える
      data-locations: file:/opt/springboot-app/seed.sql
      encoding: UTF-8
  jpa:
    defer-datasource-initialization: true   # 表を作ってから seed.sql を流す
    show-sql: false
server:
  address: 127.0.0.1        # 8080 番は EC2 の中からだけ
```
- このファイルは `backend/.gitignore` で除外してあります。`src/main/resources/` には置きません（JAR の中にパスワードが入ってしまうため）。
- EC2 では `app.jar` と同じフォルダに置きます。Spring Boot は、起動したフォルダにある `application-production.yml` を自動で読みます。

## 2. 手元でビルドする
```bash
# バックエンド（springboot-app/backend で）
JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" ./gradlew clean bootJar

# フロントエンド（springboot-app/frontend で）
npm ci
npm run build
```

## 3. EC2 に送る（springboot-app で）
```bash
EC2=ec2-user@<public_ip>      # infra で terraform output -raw public_ip
KEY=~/.ssh/springboot-ec2

ssh -i $KEY $EC2 'mkdir -p ~/deploy && chmod 700 ~/deploy'
scp -i $KEY backend/build/libs/backend-0.0.1-SNAPSHOT.jar backend/application-production.yml \
  db/seed.sql deploy/springboot-app.service deploy/nginx-springboot-app.conf $EC2:~/deploy/
scp -i $KEY -r frontend/dist $EC2:~/deploy/dist
```

## 4. EC2 で置いて起動する（初回）
`ssh -i ~/.ssh/springboot-ec2 ec2-user@<public_ip>` で入ってから：
```bash
# Spring Boot を動かす専用ユーザー（ログインはできない）
sudo useradd --system --no-create-home --shell /sbin/nologin springboot

# バックエンド
sudo mkdir -p /opt/springboot-app
sudo install -o springboot -g springboot -m 644 ~/deploy/backend-0.0.1-SNAPSHOT.jar /opt/springboot-app/app.jar
sudo install -o springboot -g springboot -m 600 ~/deploy/application-production.yml /opt/springboot-app/
sudo install -o springboot -g springboot -m 644 ~/deploy/seed.sql /opt/springboot-app/
sudo chown springboot:springboot /opt/springboot-app && sudo chmod 750 /opt/springboot-app
sudo install -m 644 ~/deploy/springboot-app.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now springboot-app

# 画面と Nginx
sudo dnf install -y nginx
sudo mkdir -p /usr/share/nginx/springboot-app
sudo cp -r ~/deploy/dist/. /usr/share/nginx/springboot-app/
sudo install -m 644 ~/deploy/nginx-springboot-app.conf /etc/nginx/conf.d/springboot-app.conf
sudo nginx -t
sudo systemctl enable --now nginx

# 送ったファイルを片づける（パスワード入りの yml のコピーがあるため）
rm -rf ~/deploy
```

### 初回の起動のあと、必ず `never` にする
`seed.sql` は表を空にしてからテストデータを入れます。`always` のままだと、再起動のたびにデータが消えます。
```bash
curl -s localhost:8080/api/tasks      # テストデータの6件が入ったか見る
sudo sed -i 's/^      mode: always$/      mode: never/' /opt/springboot-app/application-production.yml
sudo systemctl restart springboot-app
```
手元の `backend/application-production.yml` も `mode: never` に変えておきます（次に送ったときに `always` に戻らないように）。

## 2回目からの更新
2・3 のあと、EC2 で入れかえたいものだけを置きなおします。
```bash
# バックエンドを入れかえる
sudo install -o springboot -g springboot -m 644 ~/deploy/backend-0.0.1-SNAPSHOT.jar /opt/springboot-app/app.jar
sudo systemctl restart springboot-app

# 画面を入れかえる（Nginx の再起動はいらない）
sudo rm -rf /usr/share/nginx/springboot-app/*
sudo cp -r ~/deploy/dist/. /usr/share/nginx/springboot-app/

rm -rf ~/deploy
```

## 確認する
PC から：
```bash
curl http://<public_ip>/api/db-check   # DB connected: springboot_app (PostgreSQL 17.11)
curl http://<public_ip>/api/tasks      # タスクの一覧（JSON）
```
ブラウザで `http://<public_ip>/` を開きます（自分の IP からだけ開けます）。

## うまく動かないとき（EC2 で）
```bash
systemctl status springboot-app            # 動いているか
sudo journalctl -u springboot-app -n 100   # Spring Boot のログ
sudo nginx -t                              # Nginx の設定の書きまちがい
sudo tail -n 50 /var/log/nginx/error.log   # Nginx のエラー
```
- 画面は出るのに `/api` が 502 になる → Spring Boot が止まっているか、起動の途中です。
- 開けない → PC の IP が変わったかもしれません。`curl https://checkip.amazonaws.com` で今の IP を調べて、`infra/terraform.tfvars` の `my_ip` を直します。
  そのあと `infra` フォルダで `terraform plan` を実行し、**`Plan: 0 to add, 3 to change, 0 to destroy`（変更3件・削除0件）** になっていることを確かめてから `terraform apply` します。
  - 変わるのは、自分の IP で開けている入口の3つ（22番 SSH・5173番・80番）です。どれも「その場で書きかえ（updated in-place）」で、EC2 や RDS はつくりなおされません。
  - `destroy` や `must be replaced` が出たら、apply しないで止めます。

## 注意
- EC2 を `terraform destroy` や建てなおしで消すと、ここで入れたもの（Nginx・systemd・アプリ）も消えます。RDS もデータごと消えます。もう一度つくったら、この手順を1からやりなおします（`mode: always` に戻すのも忘れずに）。
