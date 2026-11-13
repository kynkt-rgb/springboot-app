# 入力。自分の値は terraform.tfvars に書く（ひな形は terraform.tfvars.example）

variable "my_ip" {
  description = "今使っている PC の IP アドレス（curl https://checkip.amazonaws.com で調べる）"
  type        = string

  validation {
    condition     = can(regex("^\\d{1,3}(\\.\\d{1,3}){3}$", var.my_ip))
    error_message = "my_ip は 203.0.113.10 のような IPv4 アドレスで書いてください（/32 はつけない）。"
  }
}

variable "public_key_path" {
  description = "EC2 に登録する SSH 公開鍵の場所"
  type        = string
  default     = "~/.ssh/springboot-ec2.pub"
}

variable "region" {
  description = "AWS のリージョン（東京）"
  type        = string
  default     = "ap-northeast-1"
}

variable "project" {
  description = "つくるものの名前の頭につける文字"
  type        = string
  default     = "springboot-app"
}

variable "instance_type" {
  description = "EC2 の大きさ（無料枠の t3.micro）"
  type        = string
  default     = "t3.micro"
}

variable "node_version" {
  description = "Node.js のバージョン（docs/tech-stack.md に合わせる）"
  type        = string
  default     = "26.13.0"
}

# --- RDS（compose.yaml に合わせる） ---

variable "db_name" {
  description = "最初に作るデータベースの名前"
  type        = string
  default     = "springboot_app"
}

variable "db_username" {
  description = "DB にログインするユーザー名"
  type        = string
  default     = "app"
}

variable "db_engine_version" {
  description = "PostgreSQL のバージョン（手元の Docker の postgres:17 = 17.11）"
  type        = string
  default     = "17.11"
}

variable "db_instance_class" {
  description = "RDS の大きさ（無料枠の db.t4g.micro）"
  type        = string
  default     = "db.t4g.micro"
}
