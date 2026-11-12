# EC2 1台（Amazon Linux 2023・t3.micro）

# いちばん新しい Amazon Linux 2023（x86_64）を探す
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

# PC で作った SSH の公開鍵を AWS に登録する
resource "aws_key_pair" "main" {
  key_name   = "${var.project}-key"
  public_key = file(pathexpand(var.public_key_path))
}

resource "aws_instance" "app" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.app.id]
  key_name               = aws_key_pair.main.key_name

  # 起動したときに1回だけ動く準備の手順（Java・Node.js などを入れる）
  user_data = templatefile("${path.module}/user_data.sh", {
    node_version = var.node_version
  })
  # user_data は最初の起動でしか動かないので、書きかえたら EC2 を建てなおす
  user_data_replace_on_change = true

  root_block_device {
    volume_type = "gp3"
    volume_size = 20 # 無料枠は 30GB まで
    encrypted   = true
  }

  # サーバーの中の情報を読むときに、安全な方式（IMDSv2）だけを使う
  metadata_options {
    http_tokens = "required"
  }

  tags = { Name = "${var.project}-ec2" }

  # 新しい AMI が出ても、勝手に EC2 を作りなおさない
  lifecycle {
    ignore_changes = [ami]
  }
}
