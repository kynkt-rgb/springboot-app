# RDS（PostgreSQL）1台。無料枠：db.t4g.micro・マルチAZなし・20GB

# パスワードはランダムに作る。保存されるのは terraform.tfstate の中だけ（GitHub には上げない）
# 見たいとき：terraform output -raw db_password
resource "random_password" "db" {
  length  = 32
  special = false # 記号なし（接続の文字列に入れても困らないように）
}

# RDS を置いてよいサブネットのまとまり（非公開サブネット2つ）
resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnets"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "${var.project}-db-subnets" }
}

resource "aws_db_instance" "main" {
  identifier     = "${var.project}-db"
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  # ディスク（無料枠は 20GB まで）
  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  # 置き場所：非公開サブネット、EC2 と同じ AZ、外には公開しない
  db_subnet_group_name   = aws_db_subnet_group.main.name
  availability_zone      = aws_subnet.public.availability_zone
  vpc_security_group_ids = [aws_security_group.db.id]
  publicly_accessible    = false
  multi_az               = false

  # バージョンを手元の Docker と同じに保つ（勝手に上げない）
  auto_minor_version_upgrade = false

  # 自動バックアップは1日分だけ
  backup_retention_period = 1

  # 練習用なので、消すときに最後のバックアップは取らない・削除保護もしない
  skip_final_snapshot = true
  deletion_protection = false
  apply_immediately   = true

  tags = { Name = "${var.project}-db" }
}
