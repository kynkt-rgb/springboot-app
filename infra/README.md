# infra（AWS の EC2 を Terraform でつくる）

AWS の無料利用枠に収まるように、サーバー（EC2）を1台だけつくります。
今はまだ EC2 までです。DB（RDS）とアプリを動かす作業は、次の段階でやります。

## つくるもの
| もの | 中身 |
|------|------|
| ネットワーク | VPC 1つ・公開用サブネット 1つ・インターネットゲートウェイ（NAT Gateway は使わない） |
| セキュリティグループ | 自分の PC の IP アドレスからだけ、22番（SSH）と 5173番（画面）に入れる |
| EC2 | Amazon Linux 2023・t3.micro・ディスク 20GB |
| EC2 に入るもの | Java 21（Amazon Corretto）・Node.js 26.13.0・git・unzip・tar・xz・libatomic（Node.js に必要）・スワップ 2GB |

8080番（バックエンド）は外に開けていません。画面からの `/api` は、EC2 の中で 5173番から 8080番に取り次ぎます。

## はじめにやること（1回だけ）
```bash
# 1. SSH の鍵を作る
ssh-keygen -t ed25519 -f ~/.ssh/springboot-ec2 -C "springboot-ec2"

# 2. 自分の値を書くファイルを作る
cd infra
cp terraform.tfvars.example terraform.tfvars
curl https://checkip.amazonaws.com   # 出てきた IP を terraform.tfvars の my_ip に書く

# 3. Terraform の準備
terraform init
```

## つくる
```bash
terraform plan    # 何がつくられるかを見る（まだ何もつくられない）
terraform apply   # 内容を見て yes と打つと、つくられる
```
最後に、公開 IP・SSH のコマンド・画面の URL が表示されます。

## 確認する
```bash
ssh -i ~/.ssh/springboot-ec2 ec2-user@<公開IP>
java -version   # 21
node -v         # v26.13.0
npm -v
free -h         # Swap が 2.0Gi
cat /var/log/user-data.log   # 準備の記録。最後に "user_data done"
```
起動してから準備が終わるまで、数分かかります。

## IP アドレスが変わったとき
家とカフェなど、場所が変わると IP アドレスも変わり、EC2 に入れなくなります。
`terraform.tfvars` の `my_ip` を書きなおして、`terraform apply` をもう一度実行してください。

## 使わないときは消す
```bash
terraform destroy
```
- EC2 を止めて起動しなおすと、公開 IP が変わります。
- 公開 IP アドレスは、無料枠を超えると1時間ごとに少しお金がかかります。
