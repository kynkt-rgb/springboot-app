# Terraform 本体と AWS プロバイダ（AWS を操作する部品）のバージョン
terraform {
  required_version = ">= 1.10"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region

  # つくったものすべてに同じ札をつけて、AWS の画面で見分けやすくする
  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
    }
  }
}
