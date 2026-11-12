output "public_ip" {
  description = "EC2 の公開 IP アドレス（止めて起動しなおすと変わる）"
  value       = aws_instance.app.public_ip
}

output "ssh_command" {
  description = "EC2 に入るコマンド"
  value       = "ssh -i ${trimsuffix(var.public_key_path, ".pub")} ec2-user@${aws_instance.app.public_ip}"
}

output "frontend_url" {
  description = "画面の URL（アプリを起動したあとで使う）"
  value       = "http://${aws_instance.app.public_ip}:5173"
}
