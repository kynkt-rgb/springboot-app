#!/bin/bash
# EC2 が初めて起動したときに1回だけ動く準備の手順
# 結果は /var/log/user-data.log に残る
set -euxo pipefail
exec > >(tee /var/log/user-data.log) 2>&1

# 1. スワップ 2GB（t3.micro はメモリが 1GB しかなく、Gradle のビルドで足りなくなるため）
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=2048
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 2. Java 21（Amazon Corretto）と、インストールに使う道具
#    libatomic は Node.js が動くのに必要（Amazon Linux には最初から入っていない）
#    postgresql17 は RDS につながるか確かめる psql コマンド（DB 本体は入らない）
dnf install -y java-21-amazon-corretto-devel git unzip tar xz libatomic postgresql17

# 3. Node.js（公式サイトの版をそのまま入れる。dnf には 26 系がないため）
cd /tmp
NODE_FILE="node-v${node_version}-linux-x64.tar.xz"
curl -fsSLO "https://nodejs.org/dist/v${node_version}/$NODE_FILE"
curl -fsSLO "https://nodejs.org/dist/v${node_version}/SHASUMS256.txt"
# ダウンロードしたファイルが壊れていないか、公式の一覧と照らし合わせる
grep " $NODE_FILE\$" SHASUMS256.txt | sha256sum -c -
tar -xJf "$NODE_FILE" -C /usr/local --strip-components=1 --no-same-owner
rm -f "$NODE_FILE" SHASUMS256.txt

# 4. 入ったものを記録
java -version
/usr/local/bin/node -v
/usr/local/bin/npm -v
psql --version
echo "user_data done"
