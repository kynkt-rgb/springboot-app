# セキュリティグループ：自分の PC の IP アドレスからだけ入れる
# 8080（バックエンド）は開けない。画面の /api は EC2 の中で 5173 → 8080 に取り次ぐため

resource "aws_security_group" "app" {
  name        = "${var.project}-sg"
  description = "Allow SSH and frontend only from my IP"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.project}-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  security_group_id = aws_security_group.app.id
  description       = "SSH from my IP"
  cidr_ipv4         = "${var.my_ip}/32"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
}

resource "aws_vpc_security_group_ingress_rule" "frontend" {
  security_group_id = aws_security_group.app.id
  description       = "Frontend (Vite) from my IP"
  cidr_ipv4         = "${var.my_ip}/32"
  ip_protocol       = "tcp"
  from_port         = 5173
  to_port           = 5173
}

# 外向きはすべて許可（インストールのダウンロードなどに使う）
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  description       = "All outbound"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
